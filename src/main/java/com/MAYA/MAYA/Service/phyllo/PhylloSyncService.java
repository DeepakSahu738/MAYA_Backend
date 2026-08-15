package com.MAYA.MAYA.Service.phyllo;

import com.MAYA.MAYA.Entity.UserSocialAccount;
import com.MAYA.MAYA.Entity.instagram.*;
import com.MAYA.MAYA.Repository.UserSocialAccountRepository;
import com.MAYA.MAYA.Repository.instagram.*;
import com.MAYA.MAYA.Service.analytics.AnalyticsProcessingService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Syncs data from Phyllo API into Maya's database.
 *
 * This is the ONLY class that maps Phyllo JSON → Maya entities.
 * All other services work with Maya entities — never Phyllo directly.
 *
 * Runs @Async so it doesn't block the user-facing response.
 * Called after a successful account connection.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PhylloSyncService {

    private final PhylloService phylloService;
    private final CreatorRepository creatorRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final UserSocialAccountRepository socialAccountRepository;
    private final AnalyticsProcessingService analyticsProcessingService;

    /**
     * Full sync for a connected account.
     * Fetches profile, posts, and comments from Phyllo and stores in Maya DB.
     *
     * @param phylloAccountId - the connected account's Phyllo ID
     * @param creatorId       - the Maya creator entity ID to store data under
     */
    @Async
    @Transactional
    public void syncAccount(String phylloAccountId, Long creatorId) {
        log.info("Starting sync for Phyllo account: {} → creator: {}", phylloAccountId, creatorId);

        Creator creator = creatorRepository.findById(creatorId).orElse(null);
        if (creator == null) {
            log.error("Creator not found: {}", creatorId);
            return;
        }

        try {
            // Sync profile
            syncProfile(phylloAccountId, creator);

            // Phase 1: Sync posts (last 90 days)
            Map<String, Post> phylloIdToPost = syncPosts(phylloAccountId, creator);

            // If no posts returned — account likely has no recent data (older than 90 days)
            // Trigger historic fetch, wait, then retry
            if (phylloIdToPost.isEmpty()) {
                log.info("No recent posts found for account: {} — requesting historic data...", phylloAccountId);
                boolean historicRequested = phylloService.requestHistoricData(phylloAccountId);

                if (historicRequested) {
                    // Wait for Phyllo to process (poll with backoff — max 90 seconds)
                    int attempts = 0;
                    int maxAttempts = 6;
                    while (phylloIdToPost.isEmpty() && attempts < maxAttempts) {
                        attempts++;
                        log.info("Waiting for historic data... attempt {}/{}", attempts, maxAttempts);
                        Thread.sleep(15000); // wait 15 seconds between retries
                        phylloIdToPost = syncPosts(phylloAccountId, creator);
                    }

                    if (phylloIdToPost.isEmpty()) {
                        log.warn("Historic data not yet available for account: {} — will be picked up on next sync", phylloAccountId);
                    } else {
                        log.info("Historic data arrived! Synced {} posts for account: {}", phylloIdToPost.size(), phylloAccountId);
                    }
                }
            }

            // Sync comments (for whatever posts we have)
            if (!phylloIdToPost.isEmpty()) {
                syncComments(phylloAccountId, creator, phylloIdToPost);
            }

            // Update last synced timestamp
            creator.setLastSyncedAt(LocalDateTime.now());
            creatorRepository.save(creator);

            // Update social account status
            socialAccountRepository.findByPhylloAccountId(phylloAccountId).ifPresent(account -> {
                account.setLastSyncedAt(LocalDateTime.now());
                socialAccountRepository.save(account);
            });

            // Generate analytics (hashtags, top commenters, weekly report) for this creator
            if (!phylloIdToPost.isEmpty()) {
                log.info("Computing analytics for creator: {} (@{})", creatorId, creator.getUsername());
                analyticsProcessingService.processCreatorAnalytics(creator);
            }

            log.info("Sync completed for creator: {} (@{})", creatorId, creator.getUsername());
        } catch (Exception e) {
            log.error("Sync failed for Phyllo account: {}", phylloAccountId, e);
        }
    }

    private void syncProfile(String accountId, Creator creator) {
        try {
            JsonNode profileData = phylloService.fetchProfile(accountId);
            JsonNode data = profileData.has("data") && profileData.get("data").isArray()
                && profileData.get("data").size() > 0
                ? profileData.get("data").get(0) : null;

            if (data == null) {
                log.warn("  → No profile data returned for account: {}", accountId);
                return;
            }

            // Top-level fields
            if (data.has("platform_username") && !data.get("platform_username").isNull())
                creator.setUsername(data.get("platform_username").asText());
            if (data.has("full_name") && !data.get("full_name").isNull())
                creator.setBiography(data.get("full_name").asText());
            if (data.has("image_url") && !data.get("image_url").isNull())
                creator.setProfilePictureUrl(data.get("image_url").asText());
            if (data.has("url") && !data.get("url").isNull())
                creator.setWebsite(data.get("url").asText());
            if (data.has("is_verified"))
                creator.setIsVerified(data.get("is_verified").asBoolean());
            if (data.has("platform_account_type") && !data.get("platform_account_type").isNull())
                creator.setAccountType(data.get("platform_account_type").asText());

            // Reputation (nested object with follower_count, following_count, content_count)
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
            log.info("  → Synced profile for @{} (followers: {})", creator.getUsername(), creator.getFollowerCount());
        } catch (Exception e) {
            log.warn("  → Failed to sync profile: {}", e.getMessage());
        }
    }

    private Map<String, Post> syncPosts(String accountId, Creator creator) {
        Map<String, Post> phylloIdToPost = new HashMap<>();

        try {
            JsonNode contentData = phylloService.fetchContents(accountId, 100);
            JsonNode posts = contentData.get("data");
            if (posts == null || !posts.isArray()) return phylloIdToPost;

            int synced = 0;
            for (JsonNode postNode : posts) {
                String phylloPostId = postNode.get("id").asText();

                // Skip if already exists
                if (postRepository.findByInstagramId(phylloPostId).isPresent()) {
                    phylloIdToPost.put(phylloPostId, postRepository.findByInstagramId(phylloPostId).get());
                    continue;
                }

                Post post = mapPhylloPost(postNode, creator);
                post = postRepository.save(post);
                phylloIdToPost.put(phylloPostId, post);
                synced++;
            }

            log.info("  → Synced {} new posts (total in response: {})", synced, posts.size());
        } catch (Exception e) {
            log.warn("  → Failed to sync posts: {}", e.getMessage());
        }

        return phylloIdToPost;
    }

    private void syncComments(String accountId, Creator creator, Map<String, Post> phylloIdToPost) {
        int totalSynced = 0;

        // Fetch comments per post (Phyllo requires both account_id and content_id)
        for (Map.Entry<String, Post> entry : phylloIdToPost.entrySet()) {
            String phylloPostId = entry.getKey();
            Post post = entry.getValue();

            try {
                JsonNode commentData = phylloService.fetchComments(accountId, phylloPostId, 100);
                JsonNode comments = commentData.get("data");
                if (comments == null || !comments.isArray()) continue;

                for (JsonNode commentNode : comments) {
                    String commentId = commentNode.get("id").asText();

                    // Skip if exists
                    if (commentRepository.findByInstagramId(commentId).isPresent()) continue;

                    Comment comment = mapPhylloComment(commentNode, post, creator.getId());
                    commentRepository.save(comment);
                    totalSynced++;
                }
            } catch (Exception e) {
                // Non-critical per post — continue with other posts
                log.debug("  → No comments for post {}: {}", phylloPostId, e.getMessage());
            }
        }

        log.info("  → Synced {} new comments across {} posts", totalSynced, phylloIdToPost.size());
    }

    // --- Mappers: Phyllo JSON → Maya entities ---

    private Post mapPhylloPost(JsonNode node, Creator creator) {
        Post post = new Post();
        post.setInstagramId(node.get("id").asText());
        post.setCreator(creator);

        // Caption from "title"
        String caption = getTextOrNull(node, "title");
        post.setCaption(caption != null ? caption : "");

        // Format and type
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
            computeRates(metrics);
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
        detectCta(post, caption);

        // Question detection
        if (caption != null && caption.trim().endsWith("?")) {
            post.setHasQuestion(true);
        }

        return post;
    }

    private Comment mapPhylloComment(JsonNode node, Post post, Long creatorId) {
        Comment comment = new Comment();
        comment.setInstagramId(node.get("id").asText());
        comment.setPost(post);
        comment.setCreatorId(creatorId);
        comment.setUsername(node.has("commenter_username") ? node.get("commenter_username").asText() : "unknown");
        comment.setText(node.has("text") ? node.get("text").asText() : "");
        comment.setLikeCount(getIntOrZero(node, "like_count"));
        comment.setReplyCount(getIntOrZero(node, "reply_count"));
        comment.setIsQuestion(isQuestion(comment.getText()));

        if (node.has("published_at") && !node.get("published_at").isNull()) {
            comment.setCommentedAt(LocalDateTime.parse(node.get("published_at").asText(), DateTimeFormatter.ISO_DATE_TIME));
        } else {
            comment.setCommentedAt(LocalDateTime.now());
        }

        return comment;
    }

    // --- Helpers (same logic as DataSeedService) ---

    private void computeRates(PostMetrics metrics) {
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
    }

    private void detectCta(Post post, String caption) {
        if (caption == null) return;
        String lower = caption.toLowerCase();
        if (lower.contains("link in bio") || lower.contains("link in my bio") || lower.contains("bio link")) {
            post.setHasCta(true); post.setCtaType("LINK_IN_BIO");
        } else if (lower.contains("comment below") || lower.contains("drop a comment")) {
            post.setHasCta(true); post.setCtaType("COMMENT_BELOW");
        } else if (lower.contains("save this") || lower.contains("save for later")) {
            post.setHasCta(true); post.setCtaType("SAVE_THIS");
        } else if (lower.contains("share this") || lower.contains("tag someone") || lower.contains("tag a friend")) {
            post.setHasCta(true); post.setCtaType("SHARE_THIS");
        } else if (lower.trim().endsWith("?")) {
            post.setHasCta(true); post.setCtaType("QUESTION_CTA");
        }
    }

    private boolean isQuestion(String text) {
        String t = text.trim().toLowerCase();
        return t.endsWith("?") || t.matches("^(how|what|when|where|why|which|can|do|did|is|are|should|would|could|will)\\b.*");
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
