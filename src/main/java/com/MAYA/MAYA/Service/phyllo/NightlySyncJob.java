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
import org.springframework.transaction.support.TransactionTemplate;

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
    private final WeeklyReportRepository weeklyReportRepository;
    private final PhylloService phylloService;
    private final CreatorAccessService creatorAccessService;
    private final AnalyticsProcessingService analyticsProcessingService;
    private final TransactionTemplate transactionTemplate;
    private final DataFreshnessService dataFreshnessService;

    private static final int COMMENT_FETCH_LIMIT = 15;
    private static final long RATE_LIMIT_DELAY_MS = 200;

    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Kolkata")
    public void runNightlySync() {
        log.info("=== NIGHTLY SYNC STARTED ===");
        long startTime = System.currentTimeMillis();

        // Load accounts within a transaction to avoid lazy-loading issues
        List<UserSocialAccount> allConnected = transactionTemplate.execute(status -> {
            return socialAccountRepository.findAll().stream()
                .filter(a -> "CONNECTED".equals(a.getStatus()))
                .filter(a -> a.getCreator() != null)
                .filter(a -> !creatorAccessService.isDemoCreator(a.getCreator().getId()))
                .collect(Collectors.toList());
        });

        log.info("Found {} connected accounts to sync", allConnected != null ? allConnected.size() : 0);

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

    public void syncSingleAccount(UserSocialAccount account) throws Exception {
        transactionTemplate.executeWithoutResult(status -> {
            try {
                String phylloAccountId = account.getPhylloAccountId();
                Creator creator = creatorRepository.findById(account.getCreator().getId()).orElseThrow();
                Long creatorId = creator.getId();
                log.info("Syncing @{} (creator: {})", account.getPlatformUsername(), creatorId);

                // 1. Sync profile
                syncProfile(phylloAccountId, creator);
                rateLimitDelay();

                // 2. DELETE old posts + comments → batch INSERT fresh
                // Clear weekly_reports FK refs to posts first, else post delete
                // violates the top_post_id / worst_post_id foreign key constraint.
                weeklyReportRepository.clearPostReferencesByCreatorId(creatorId);
                commentRepository.deleteByCreatorId(creatorId);
                postRepository.deleteByCreatorId(creatorId);
                postRepository.flush(); // Force DELETE to execute in DB before INSERT
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

                // 5b. Recompute data freshness — a creator who resumed posting
                // flips HISTORIC → RECENT here (and latest/oldest post dates update).
                dataFreshnessService.recompute(creatorId);

                // 6. Update timestamps
                creator.setLastSyncedAt(LocalDateTime.now());
                creatorRepository.save(creator);
                account.setLastSyncedAt(LocalDateTime.now());
                socialAccountRepository.save(account);

                // 7. Recompute analytics (hashtags + commenters + weekly report)
                analyticsProcessingService.processCreatorAnalytics(creator);

                log.info("  ✓ Sync complete for @{}", account.getPlatformUsername());
            } catch (Exception e) {
                status.setRollbackOnly();
                throw new RuntimeException(e.getMessage(), e);
            }
        });
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
                // follower_count for most platforms; subscriber_count for YouTube/Twitch/
                // LinkedIn/AdSense/Spotify. Prefer follower_count, fall back to subscriber_count.
                if (reputation.has("follower_count") && !reputation.get("follower_count").isNull())
                    creator.setFollowerCount(reputation.get("follower_count").asInt());
                else if (reputation.has("subscriber_count") && !reputation.get("subscriber_count").isNull())
                    creator.setFollowerCount(reputation.get("subscriber_count").asInt());
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

    // === FETCH + BUILD POSTS (WITH PAGINATION) ===
    private List<Post> fetchAndBuildPosts(String accountId, Creator creator) {
        List<Post> posts = new ArrayList<>();
        try {
            int limit = 100;
            int offset = 0;
            boolean hasMore = true;

            while (hasMore) {
                JsonNode contentData = phylloService.fetchContents(accountId, limit, offset);
                JsonNode postsArray = contentData.get("data");
                if (postsArray == null || !postsArray.isArray() || postsArray.size() == 0) break;

                for (JsonNode postNode : postsArray) {
                    posts.add(buildPost(postNode, creator));
                }

                // Check pagination
                JsonNode metadata = contentData.get("metadata");
                if (metadata != null && metadata.has("total")) {
                    int total = metadata.get("total").asInt();
                    if (posts.size() >= total) {
                        hasMore = false;
                    }
                } else {
                    if (postsArray.size() < limit) {
                        hasMore = false;
                    }
                }

                offset += postsArray.size();
                rateLimitDelay();
            }

            log.info("  → Fetched {} total posts (paginated)", posts.size());
        } catch (Exception e) {
            log.warn("  → Posts fetch failed: {}", e.getMessage());
        }
        return posts;
    }

    // === FETCH COMMENTS FOR ONE POST ===
    private List<Comment> fetchCommentsForPost(String accountId, Post post, Long creatorId) {
        List<Comment> comments = new ArrayList<>();
        try {
            JsonNode commentData = phylloService.fetchComments(accountId, post.getPhylloId(), 100);
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
        post.setPhylloId(node.get("id").asText());
        post.setExternalId(getTextOrNull(node, "external_id"));
        post.setPlatform(creator.getPlatform());
        post.setCreator(creator);

        post.setTitle(getTextOrNull(node, "title"));
        String caption = getTextOrNull(node, "description");
        if (caption == null) caption = getTextOrNull(node, "title");
        post.setCaption(caption != null ? caption : "");
        post.setFormat(getTextOrNull(node, "format"));
        post.setType(getTextOrNull(node, "type"));
        post.setMediaUrl(getTextOrNull(node, "media_url"));
        post.setUrl(getTextOrNull(node, "url"));
        post.setThumbnailUrl(getTextOrNull(node, "thumbnail_url"));
        post.setPersistentThumbnailUrl(getTextOrNull(node, "persistent_thumbnail_url"));
        post.setVisibility(getTextOrNull(node, "visibility"));
        post.setPlatformProfileId(getTextOrNull(node, "platform_profile_id"));
        post.setPlatformProfileName(getTextOrNull(node, "platform_profile_name"));
        post.setDuration(getIntOrNull(node, "duration"));
        if (node.has("is_owned_by_platform_user") && !node.get("is_owned_by_platform_user").isNull()) {
            post.setIsOwnedByPlatformUser(node.get("is_owned_by_platform_user").asBoolean());
        }

        // Hashtags
        post.setHashtags(joinArray(node.get("hashtags")));
        JsonNode hashtagsNode = node.get("hashtags");
        if (hashtagsNode != null && hashtagsNode.isArray()) {
            post.setHashtagCount(hashtagsNode.size());
        }

        // Mentions
        post.setMentions(joinArray(node.get("mentions")));

        // Metrics
        JsonNode engagement = node.get("engagement");
        if (engagement != null) {
            PostMetrics metrics = new PostMetrics();
            metrics.setLikeCount(getIntOrZero(engagement, "like_count"));
            metrics.setCommentCount(getIntOrZero(engagement, "comment_count"));
            metrics.setSaveCount(getIntOrNull(engagement, "save_count"));
            metrics.setShareCount(getIntOrNull(engagement, "share_count"));
            metrics.setRepostCount(getIntOrNull(engagement, "repost_count"));
            metrics.setDislikeCount(getIntOrNull(engagement, "dislike_count"));
            metrics.setReachOrganicCount(getIntOrNull(engagement, "reach_organic_count"));
            metrics.setImpressionOrganicCount(getIntOrNull(engagement, "impression_organic_count"));
            metrics.setViewCount(getLongOrNull(engagement, "view_count"));
            metrics.setWatchTimeInHours(getDoubleOrNull(engagement, "watch_time_in_hours"));
            metrics.setAvgWatchTimeInSec(getDoubleOrNull(engagement, "avg_watch_time_in_sec"));
            metrics.setClickCount(getIntOrNull(engagement, "click_count"));
            metrics.setReplayCount(getIntOrNull(engagement, "replay_count"));

            // Compute rates
            Integer reach = metrics.getReachOrganicCount();
            int likes = metrics.getLikeCount() != null ? metrics.getLikeCount() : 0;
            int comments = metrics.getCommentCount() != null ? metrics.getCommentCount() : 0;
            int saves = metrics.getSaveCount() != null ? metrics.getSaveCount() : 0;
            int shares = metrics.getShareCount() != null ? metrics.getShareCount() : 0;
            int totalEngagement = likes + comments + saves + shares;

            // Validate reach: must be >= total engagement, otherwise unreliable
            boolean reachReliable = reach != null && reach > 0 && reach >= totalEngagement;
            if (reachReliable) {
                if (metrics.getSaveCount() != null)
                    metrics.setSaveRate(Math.min(metrics.getSaveCount() * 100.0 / reach, 100.0));
                if (metrics.getShareCount() != null)
                    metrics.setShareRate(Math.min(metrics.getShareCount() * 100.0 / reach, 100.0));
                metrics.setEngagementRate(Math.min(totalEngagement * 100.0 / reach, 100.0));
            }

            post.setMetrics(metrics);
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
        comment.setPhylloId(node.get("id").asText());
        comment.setExternalId(getTextOrNull(node, "external_id"));
        comment.setPost(post);
        comment.setCreatorId(creatorId);
        comment.setUsername(node.has("commenter_username") ? node.get("commenter_username").asText() : "unknown");
        comment.setCommenterId(getTextOrNull(node, "commenter_id"));
        comment.setCommenterProfileUrl(getTextOrNull(node, "commenter_profile_url"));
        comment.setCommenterDisplayName(getTextOrNull(node, "commenter_display_name"));
        comment.setText(node.has("text") ? node.get("text").asText() : "");
        comment.setLikeCount(getIntOrZero(node, "like_count"));
        comment.setReplyCount(getIntOrZero(node, "reply_count"));

        String text = comment.getText().trim().toLowerCase();
        comment.setIsQuestion(text.endsWith("?") ||
            text.matches("^(how|what|when|where|why|which|can|do|did|is|are|should|would|could|will)\\b.*"));

        // Parent content reference
        JsonNode content = node.get("content");
        if (content != null) {
            comment.setContentUrl(getTextOrNull(content, "url"));
            if (content.has("published_at") && !content.get("published_at").isNull()) {
                comment.setContentPublishedAt(LocalDateTime.parse(content.get("published_at").asText(), DateTimeFormatter.ISO_DATE_TIME));
            }
        }

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

    private Long getLongOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return (value != null && !value.isNull()) ? value.asLong() : null;
    }

    private Double getDoubleOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return (value != null && !value.isNull()) ? value.asDouble() : null;
    }

    private String joinArray(JsonNode arrayNode) {
        if (arrayNode == null || !arrayNode.isArray() || arrayNode.isEmpty()) return null;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arrayNode.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(arrayNode.get(i).asText());
        }
        return sb.toString();
    }
}
