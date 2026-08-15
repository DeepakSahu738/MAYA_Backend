package com.MAYA.MAYA.Service.phyllo;

import com.MAYA.MAYA.Entity.UserSocialAccount;
import com.MAYA.MAYA.Entity.instagram.*;
import com.MAYA.MAYA.Repository.UserSocialAccountRepository;
import com.MAYA.MAYA.Repository.instagram.*;
import com.MAYA.MAYA.Service.CreatorAccessService;
import com.MAYA.MAYA.Service.analytics.AnalyticsProcessingService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Nightly sync job — runs at 3am daily.
 *
 * For each connected (non-demo) account:
 *   1. Syncs profile (followers, picture) — 1 API call
 *   2. Fetches all posts (up to 100) — inserts new, UPDATES metrics on existing — 1 API call
 *   3. Fetches comments for last 15 posts only — 15 API calls
 *   4. Recomputes hashtag_performance + top_commenters
 *   5. If Monday: generates weekly_report
 *
 * Rate limit: 200ms delay between API calls (Phyllo allows 10 req/sec)
 * Error isolation: if one user fails, others still complete
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NightlySyncJob {

    private final UserSocialAccountRepository socialAccountRepository;
    private final CreatorRepository creatorRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PhylloService phylloService;
    private final CreatorAccessService creatorAccessService;
    private final AnalyticsProcessingService analyticsProcessingService;

    private static final int COMMENT_FETCH_LIMIT = 15; // fetch comments for last 15 posts
    private static final long RATE_LIMIT_DELAY_MS = 200; // 200ms between API calls

    /**
     * Runs every day at 3:00 AM server time.
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void runNightlySync() {
        log.info("=== NIGHTLY SYNC STARTED ===");
        long startTime = System.currentTimeMillis();

        List<UserSocialAccount> connectedAccounts = socialAccountRepository
            .findByUserIdAndStatus(null, "CONNECTED");

        // Get ALL connected accounts (not using userId filter — we want all users)
        List<UserSocialAccount> allConnected = socialAccountRepository.findAll().stream()
            .filter(a -> "CONNECTED".equals(a.getStatus()))
            .filter(a -> a.getCreator() != null)
            .filter(a -> !creatorAccessService.isDemoCreator(a.getCreator().getId()))
            .collect(Collectors.toList());

        log.info("Found {} connected accounts to sync (excluding demo)", allConnected.size());

        int successCount = 0;
        int failCount = 0;

        for (UserSocialAccount account : allConnected) {
            try {
                syncSingleAccount(account);
                successCount++;
            } catch (Exception e) {
                failCount++;
                log.error("Sync failed for account {} (@{}): {}",
                    account.getPhylloAccountId(), account.getPlatformUsername(), e.getMessage());
            }
        }

        long duration = (System.currentTimeMillis() - startTime) / 1000;
        log.info("=== NIGHTLY SYNC COMPLETED === {} success, {} failed, took {}s", successCount, failCount, duration);
    }

    /**
     * Sync a single account — profile + posts + comments + recompute analytics.
     */
    private void syncSingleAccount(UserSocialAccount account) throws Exception {
        String phylloAccountId = account.getPhylloAccountId();
        Creator creator = account.getCreator();
        log.info("Syncing @{} (creator: {}, platform: {})", account.getPlatformUsername(), creator.getId(), account.getPlatform());

        // 1. Sync profile
        syncProfile(phylloAccountId, creator);
        rateLimitDelay();

        // 2. Fetch all posts — insert new, update existing metrics
        List<Post> syncedPosts = syncPostsWithMetricUpdate(phylloAccountId, creator);
        rateLimitDelay();

        // 3. Fetch comments for last 15 posts only
        syncRecentComments(phylloAccountId, creator, syncedPosts);

        // 4. Update last_synced_at
        creator.setLastSyncedAt(LocalDateTime.now());
        creatorRepository.save(creator);
        account.setLastSyncedAt(LocalDateTime.now());
        socialAccountRepository.save(account);

        // 5. Recompute derived tables
        recomputeAnalytics(creator);

        log.info("  ✓ Sync complete for @{}", account.getPlatformUsername());
    }

    // === SYNC PROFILE ===
    private void syncProfile(String accountId, Creator creator) {
        try {
            JsonNode profileData = phylloService.fetchProfile(accountId);
            JsonNode data = profileData.has("data") && profileData.get("data").isArray()
                && profileData.get("data").size() > 0
                ? profileData.get("data").get(0) : null;

            if (data == null) return;

            if (data.has("platform_username") && !data.get("platform_username").isNull())
                creator.setUsername(data.get("platform_username").asText());
            if (data.has("image_url") && !data.get("image_url").isNull())
                creator.setProfilePictureUrl(data.get("image_url").asText());
            if (data.has("is_verified"))
                creator.setIsVerified(data.get("is_verified").asBoolean());

            JsonNode reputation = data.get("reputation");
            if (reputation != null) {
                if (reputation.has("follower_count") && !reputation.get("follower_count").isNull())
                    creator.setFollowerCount(reputation.get("follower_count").asInt());
                if (reputation.has("following_count") && !reputation.get("following_count").isNull())
                    creator.setFollowingCount(reputation.get("following_count").asInt());
                if (reputation.has("content_count") && !reputation.get("content_count").isNull())
                    creator.setMediaCount(reputation.get("content_count").asInt());
            }

            creatorRepository.save(creator);
            log.info("  → Profile synced (followers: {})", creator.getFollowerCount());
        } catch (Exception e) {
            log.warn("  → Profile sync failed: {}", e.getMessage());
        }
    }

    // === SYNC POSTS (INSERT NEW + UPDATE EXISTING METRICS) ===
    private List<Post> syncPostsWithMetricUpdate(String accountId, Creator creator) {
        List<Post> allPosts = new ArrayList<>();

        try {
            JsonNode contentData = phylloService.fetchContents(accountId, 100);
            JsonNode posts = contentData.get("data");
            if (posts == null || !posts.isArray()) return allPosts;

            int newCount = 0;
            int updatedCount = 0;

            for (JsonNode postNode : posts) {
                String phylloPostId = postNode.get("id").asText();
                Optional<Post> existingOpt = postRepository.findByInstagramId(phylloPostId);

                if (existingOpt.isPresent()) {
                    // UPDATE existing post metrics
                    Post existing = existingOpt.get();
                    updatePostMetrics(existing, postNode);
                    postRepository.save(existing);
                    allPosts.add(existing);
                    updatedCount++;
                } else {
                    // INSERT new post
                    Post newPost = mapPhylloPost(postNode, creator);
                    newPost = postRepository.save(newPost);
                    allPosts.add(newPost);
                    newCount++;
                }
            }

            log.info("  → Posts: {} new, {} updated (total: {})", newCount, updatedCount, allPosts.size());
        } catch (Exception e) {
            log.warn("  → Posts sync failed: {}", e.getMessage());
        }

        return allPosts;
    }

    // === SYNC COMMENTS (LAST 15 POSTS ONLY) ===
    private void syncRecentComments(String accountId, Creator creator, List<Post> posts) {
        // Sort by most recent, take last 15
        List<Post> recentPosts = posts.stream()
            .sorted(Comparator.comparing(Post::getPostedAt, Comparator.nullsLast(Comparator.reverseOrder())))
            .limit(COMMENT_FETCH_LIMIT)
            .collect(Collectors.toList());

        int totalSynced = 0;

        for (Post post : recentPosts) {
            try {
                rateLimitDelay();
                JsonNode commentData = phylloService.fetchComments(accountId, post.getInstagramId(), 100);
                JsonNode comments = commentData.get("data");
                if (comments == null || !comments.isArray()) continue;

                for (JsonNode commentNode : comments) {
                    String commentId = commentNode.get("id").asText();
                    if (commentRepository.findByInstagramId(commentId).isPresent()) continue;

                    Comment comment = mapPhylloComment(commentNode, post, creator.getId());
                    commentRepository.save(comment);
                    totalSynced++;
                }
            } catch (Exception e) {
                log.debug("  → Comments failed for post {}: {}", post.getInstagramId(), e.getMessage());
            }
        }

        log.info("  → Comments: {} new (from last {} posts)", totalSynced, recentPosts.size());
    }

    // === RECOMPUTE ANALYTICS ===
    private void recomputeAnalytics(Creator creator) {
        try {
            analyticsProcessingService.processCreatorAnalytics(creator);
        } catch (Exception e) {
            log.warn("  → Analytics recompute failed: {}", e.getMessage());
        }
    }

    // === HELPER: Update existing post metrics ===
    private void updatePostMetrics(Post post, JsonNode postNode) {
        JsonNode engagement = postNode.get("engagement");
        if (engagement == null) return;

        PostMetrics metrics = post.getMetrics();
        if (metrics == null) {
            metrics = new PostMetrics();
            post.setMetrics(metrics);
        }

        metrics.setLikes(getIntOrZero(engagement, "like_count"));
        metrics.setComments(getIntOrZero(engagement, "comment_count"));
        metrics.setSaves(getIntOrNull(engagement, "save_count"));
        metrics.setShares(getIntOrNull(engagement, "share_count"));
        metrics.setReposts(getIntOrNull(engagement, "repost_count"));
        metrics.setReach(getIntOrNull(engagement, "reach_organic_count"));
        metrics.setImpressions(getIntOrNull(engagement, "impression_organic_count"));
        metrics.setPlays(getIntOrNull(engagement, "view_count"));

        // Recompute rates
        Integer reach = metrics.getReach();
        if (metrics.getSaves() != null && reach != null && reach > 0)
            metrics.setSaveRate(metrics.getSaves() * 100.0 / reach);
        if (metrics.getShares() != null && reach != null && reach > 0)
            metrics.setShareRate(metrics.getShares() * 100.0 / reach);
        if (reach != null && reach > 0) {
            int likes = metrics.getLikes() != null ? metrics.getLikes() : 0;
            int comments = metrics.getComments() != null ? metrics.getComments() : 0;
            int saves = metrics.getSaves() != null ? metrics.getSaves() : 0;
            int shares = metrics.getShares() != null ? metrics.getShares() : 0;
            metrics.setEngagementRate((likes + comments + saves + shares) * 100.0 / reach);
        }

        Integer viewCount = getIntOrNull(engagement, "view_count");
        post.setViewCount(viewCount != null ? viewCount.longValue() : null);
    }

    // === HELPER: Map new Phyllo post to entity ===
    private Post mapPhylloPost(JsonNode node, Creator creator) {
        Post post = new Post();
        post.setInstagramId(node.get("id").asText());
        post.setCreator(creator);

        String caption = getTextOrNull(node, "title");
        post.setCaption(caption != null ? caption : "");
        post.setMediaType(getTextOrNull(node, "format"));
        post.setMediaProductType(getTextOrNull(node, "type"));
        post.setMediaUrl(getTextOrNull(node, "media_url"));
        post.setPermalink(getTextOrNull(node, "url"));
        post.setThumbnailUrl(getTextOrNull(node, "thumbnail_url"));

        // Hashtags
        JsonNode hashtagsNode = node.get("hashtags");
        if (hashtagsNode != null && hashtagsNode.isArray() && !hashtagsNode.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < hashtagsNode.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(hashtagsNode.get(i).asText());
            }
            post.setHashtags(sb.toString());
            post.setHashtagCount(hashtagsNode.size());
        }

        // Metrics
        JsonNode engagement = node.get("engagement");
        if (engagement != null) {
            PostMetrics metrics = new PostMetrics();
            metrics.setLikes(getIntOrZero(engagement, "like_count"));
            metrics.setComments(getIntOrZero(engagement, "comment_count"));
            metrics.setSaves(getIntOrNull(engagement, "save_count"));
            metrics.setShares(getIntOrNull(engagement, "share_count"));
            metrics.setReposts(getIntOrNull(engagement, "repost_count"));
            metrics.setReach(getIntOrNull(engagement, "reach_organic_count"));
            metrics.setImpressions(getIntOrNull(engagement, "impression_organic_count"));
            metrics.setPlays(getIntOrNull(engagement, "view_count"));

            // Compute rates
            Integer reach = metrics.getReach();
            if (metrics.getSaves() != null && reach != null && reach > 0)
                metrics.setSaveRate(metrics.getSaves() * 100.0 / reach);
            if (metrics.getShares() != null && reach != null && reach > 0)
                metrics.setShareRate(metrics.getShares() * 100.0 / reach);
            if (reach != null && reach > 0) {
                int likes = metrics.getLikes() != null ? metrics.getLikes() : 0;
                int comments = metrics.getComments() != null ? metrics.getComments() : 0;
                int saves = metrics.getSaves() != null ? metrics.getSaves() : 0;
                int shares = metrics.getShares() != null ? metrics.getShares() : 0;
                metrics.setEngagementRate((likes + comments + saves + shares) * 100.0 / reach);
            }

            post.setMetrics(metrics);
            Integer viewCount = getIntOrNull(engagement, "view_count");
            post.setViewCount(viewCount != null ? viewCount.longValue() : null);
        }

        // Timestamp
        if (node.has("published_at") && !node.get("published_at").isNull()) {
            post.setPostedAt(LocalDateTime.parse(node.get("published_at").asText(), DateTimeFormatter.ISO_DATE_TIME));
        } else {
            post.setPostedAt(LocalDateTime.now());
        }

        // CTA + question detection
        if (caption != null) {
            String lower = caption.toLowerCase();
            if (lower.contains("link in bio") || lower.contains("bio link")) {
                post.setHasCta(true); post.setCtaType("LINK_IN_BIO");
            } else if (lower.contains("comment below") || lower.contains("drop a comment")) {
                post.setHasCta(true); post.setCtaType("COMMENT_BELOW");
            } else if (lower.contains("save this") || lower.contains("save for later")) {
                post.setHasCta(true); post.setCtaType("SAVE_THIS");
            } else if (lower.contains("share this") || lower.contains("tag someone")) {
                post.setHasCta(true); post.setCtaType("SHARE_THIS");
            } else if (lower.trim().endsWith("?")) {
                post.setHasCta(true); post.setCtaType("QUESTION_CTA");
            }
            if (lower.trim().endsWith("?")) post.setHasQuestion(true);
        }

        return post;
    }

    // === HELPER: Map Phyllo comment to entity ===
    private Comment mapPhylloComment(JsonNode node, Post post, Long creatorId) {
        Comment comment = new Comment();
        comment.setInstagramId(node.get("id").asText());
        comment.setPost(post);
        comment.setCreatorId(creatorId);
        comment.setUsername(node.has("commenter_username") ? node.get("commenter_username").asText() : "unknown");
        comment.setText(node.has("text") ? node.get("text").asText() : "");
        comment.setLikeCount(getIntOrZero(node, "like_count"));
        comment.setReplyCount(getIntOrZero(node, "reply_count"));

        String text = comment.getText().trim().toLowerCase();
        comment.setIsQuestion(text.endsWith("?") ||
            text.matches("^(how|what|when|where|why|which|can|do|did|is|are|should|would|could|will)\\b.*"));

        if (node.has("published_at") && !node.get("published_at").isNull()) {
            comment.setCommentedAt(LocalDateTime.parse(node.get("published_at").asText(), DateTimeFormatter.ISO_DATE_TIME));
        } else {
            comment.setCommentedAt(LocalDateTime.now());
        }

        return comment;
    }

    // === UTILITY ===
    private void rateLimitDelay() {
        try {
            Thread.sleep(RATE_LIMIT_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private Double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private String getTextOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return (value != null && !value.isNull()) ? value.asText() : null;
    }

    private Integer getIntOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return (value != null && !value.isNull()) ? value.asInt() : null;
    }

    private int getIntOrZero(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return (value != null && !value.isNull()) ? value.asInt() : 0;
    }
}
