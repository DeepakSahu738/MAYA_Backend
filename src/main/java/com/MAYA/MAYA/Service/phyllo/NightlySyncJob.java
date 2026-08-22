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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Nightly sync job — runs at 3am daily.
 *
 * Strategy: DELETE old data → batch INSERT fresh data.
 * No duplicates, no slow checks, fast and clean.
 *
 * For each connected (non-demo) account:
 *   1. Sync profile (1 API call)
 *   2. DELETE all posts for this creator → batch INSERT fresh posts (1 API call)
 *   3. DELETE comments for last 15 posts → batch INSERT fresh comments (15 API calls)
 *   4. DELETE hashtag_performance → batch INSERT freshly computed
 *   5. DELETE top_commenters → batch INSERT freshly computed
 *   6. Weekly report (if needed for this week)
 *
 * Rate limit: 200ms delay between API calls
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
    private final HashtagPerformanceRepository hashtagPerformanceRepository;
    private final TopCommenterRepository topCommenterRepository;
    private final PhylloService phylloService;
    private final CreatorAccessService creatorAccessService;
    private final AnalyticsProcessingService analyticsProcessingService;

    private static final int COMMENT_FETCH_LIMIT = 15;
    private static final long RATE_LIMIT_DELAY_MS = 200;

    @Scheduled(cron = "0 0 3 * * *")
    public void runNightlySync() {
        log.info("=== NIGHTLY SYNC STARTED ===");
        long startTime = System.currentTimeMillis();

        List<UserSocialAccount> allConnected = socialAccountRepository.findAll().stream()
            .filter(a -> "CONNECTED".equals(a.getStatus()))
            .filter(a -> a.getCreator() != null)
            .filter(a -> !creatorAccessService.isDemoCreator(a.getCreator().getId()))
            .collect(Collectors.toList());

        log.info("Found {} connected accounts to sync", allConnected.size());

        int successCount = 0;
        int failCount = 0;

        for (UserSocialAccount account : allConnected) {
            try {
                syncSingleAccount(account);
                successCount++;
            } catch (Exception e) {
                failCount++;
                log.error("Sync failed for @{}: {}", account.getPlatformUsername(), e.getMessage());
            }
        }

        long duration = (System.currentTimeMillis() - startTime) / 1000;
        log.info("=== NIGHTLY SYNC COMPLETED === {} success, {} failed, {}s", successCount, failCount, duration);
    }

    @Transactional
    public void syncSingleAccount(UserSocialAccount account) throws Exception {
        String phylloAccountId = account.getPhylloAccountId();
        Creator creator = account.getCreator();
        Long creatorId = creator.getId();
        log.info("Syncing @{} (creator: {})", account.getPlatformUsername(), creatorId);

        // 1. Sync profile
        syncProfile(phylloAccountId, creator);
        rateLimitDelay();

        // 2. DELETE old posts + comments → batch INSERT fresh
        // Delete comments first (FK constraint)
        commentRepository.deleteByCreatorId(creatorId);
        postRepository.deleteByCreatorId(creatorId);
        log.info("  → Cleared old posts + comments for creator {}", creatorId);

        // Fetch fresh posts from Phyllo
        List<Post> freshPosts = fetchAndBuildPosts(phylloAccountId, creator);
        if (!freshPosts.isEmpty()) {
            postRepository.saveAll(freshPosts);
            log.info("  → Inserted {} fresh posts", freshPosts.size());
        }
        rateLimitDelay();

        // 3. Fetch comments for last 15 posts
        List<Post> recentPosts = freshPosts.stream()
            .sorted(Comparator.comparing(Post::getPostedAt, Comparator.nullsLast(Comparator.reverseOrder())))
            .limit(COMMENT_FETCH_LIMIT)
            .collect(Collectors.toList());

        List<Comment> allComments = new ArrayList<>();
        for (Post post : recentPosts) {
            rateLimitDelay();
            List<Comment> postComments = fetchCommentsForPost(phylloAccountId, post, creatorId);
            allComments.addAll(postComments);
        }
        if (!allComments.isEmpty()) {
            commentRepository.saveAll(allComments);
            log.info("  → Inserted {} fresh comments (from {} posts)", allComments.size(), recentPosts.size());
        }

        // 4. DELETE + recompute hashtag_performance
        hashtagPerformanceRepository.deleteByCreatorId(creatorId);
        // 5. DELETE + recompute top_commenters
        topCommenterRepository.deleteByCreatorId(creatorId);

        // 6. Update timestamps
        creator.setLastSyncedAt(LocalDateTime.now());
        creatorRepository.save(creator);
        account.setLastSyncedAt(LocalDateTime.now());
        socialAccountRepository.save(account);

        // 7. Recompute analytics (hashtags + commenters + weekly report)
        analyticsProcessingService.processCreatorAnalytics(creator);

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

    // === FETCH + BUILD POSTS ===
    private List<Post> fetchAndBuildPosts(String accountId, Creator creator) {
        List<Post> posts = new ArrayList<>();
        try {
            JsonNode contentData = phylloService.fetchContents(accountId, 100);
            JsonNode postsArray = contentData.get("data");
            if (postsArray == null || !postsArray.isArray()) return posts;

            for (JsonNode postNode : postsArray) {
                posts.add(buildPost(postNode, creator));
            }
        } catch (Exception e) {
            log.warn("  → Posts fetch failed: {}", e.getMessage());
        }
        return posts;
    }

    // === FETCH COMMENTS FOR ONE POST ===
    private List<Comment> fetchCommentsForPost(String accountId, Post post, Long creatorId) {
        List<Comment> comments = new ArrayList<>();
        try {
            JsonNode commentData = phylloService.fetchComments(accountId, post.getInstagramId(), 100);
            JsonNode commentsArray = commentData.get("data");
            if (commentsArray == null || !commentsArray.isArray()) return comments;

            for (JsonNode commentNode : commentsArray) {
                comments.add(buildComment(commentNode, post, creatorId));
            }
        } catch (Exception e) {
            // Non-critical — some posts may have no comments
        }
        return comments;
    }

    // === BUILD POST ENTITY ===
    private Post buildPost(JsonNode node, Creator creator) {
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
            int likes = metrics.getLikes() != null ? metrics.getLikes() : 0;
            int comments = metrics.getComments() != null ? metrics.getComments() : 0;
            int saves = metrics.getSaves() != null ? metrics.getSaves() : 0;
            int shares = metrics.getShares() != null ? metrics.getShares() : 0;
            int totalEngagement = likes + comments + saves + shares;

            // Validate reach: must be >= total engagement, otherwise unreliable
            boolean reachReliable = reach != null && reach > 0 && reach >= totalEngagement;
            if (reachReliable) {
                if (metrics.getSaves() != null)
                    metrics.setSaveRate(Math.min(metrics.getSaves() * 100.0 / reach, 100.0));
                if (metrics.getShares() != null)
                    metrics.setShareRate(Math.min(metrics.getShares() * 100.0 / reach, 100.0));
                metrics.setEngagementRate(Math.min(totalEngagement * 100.0 / reach, 100.0));
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

        // CTA detection
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

    // === BUILD COMMENT ENTITY ===
    private Comment buildComment(JsonNode node, Post post, Long creatorId) {
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
        try { Thread.sleep(RATE_LIMIT_DELAY_MS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
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
