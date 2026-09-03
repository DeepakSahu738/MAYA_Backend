package com.MAYA.MAYA.Service.phyllo;

import com.MAYA.MAYA.Entity.UserSocialAccount;
import com.MAYA.MAYA.Entity.instagram.*;
import com.MAYA.MAYA.Repository.UserSocialAccountRepository;
import com.MAYA.MAYA.Repository.instagram.*;
import com.MAYA.MAYA.Service.EmailService;
import com.MAYA.MAYA.Service.analytics.AnalyticsProcessingService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

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
    private final TransactionTemplate transactionTemplate;
    private final EmailService emailService;

    // Timing constants
    private static final long INITIAL_DELAY_MS = 10_000;        // 10 seconds before first fetch
    private static final long HISTORIC_WAIT_MS = 180_000;       // 3 minutes after historic request
    private static final long COMMENTS_DELAY_MS = 60_000;       // 1 minute before fetching comments
    private static final long RETRY_DELAY_MS = 1_800_000;       // 30 minutes for final retry

    /**
     * Full sync for a connected account.
     * Fetches profile, posts, and comments from Phyllo and stores in Maya DB.
     *
     * NO @Transactional here — we use TransactionTemplate for status updates
     * so they're immediately visible to other threads (frontend polling).
     *
     * @param phylloAccountId - the connected account's Phyllo ID
     * @param creatorId       - the Maya creator entity ID to store data under
     */
    @Async
    public void syncAccount(String phylloAccountId, Long creatorId) {
        log.info("Starting sync for Phyllo account: {} → creator: {}", phylloAccountId, creatorId);

        Creator creator = creatorRepository.findById(creatorId).orElse(null);
        if (creator == null) {
            log.error("Creator not found: {}", creatorId);
            return;
        }

        // --- Sync Status: SYNCING (committed immediately, visible to frontend) ---
        updateSyncStatus(creatorId, "SYNCING", null);

        try {
            // Sync profile first (no delay needed for profile)
            syncProfile(phylloAccountId, creator);

            // Wait 10 seconds for Phyllo to index content after OAuth connection
            log.info("  → Waiting 10 seconds for Phyllo to index content...");
            Thread.sleep(INITIAL_DELAY_MS);

            // Phase 1: Fetch posts
            Map<String, Post> phylloIdToPost = syncPosts(phylloAccountId, creator);

            // If no posts returned — request historic data and wait 5 minutes
            if (phylloIdToPost.isEmpty()) {
                log.info("No recent posts found for account: {} — requesting historic data...", phylloAccountId);
                boolean historicRequested = phylloService.requestHistoricData(phylloAccountId);

                if (historicRequested) {
                    // Update status so frontend knows we're in the long wait
                    updateSyncStatus(creatorId, "SYNCING_WAITING", null);

                    // Wait 5 minutes for Phyllo to process historic data
                    log.info("  → Waiting 5 minutes for Phyllo to index historic data...");
                    Thread.sleep(HISTORIC_WAIT_MS);

                    // Try fetching again
                    phylloIdToPost = syncPosts(phylloAccountId, creator);

                    if (phylloIdToPost.isEmpty()) {
                        log.warn("Historic data not yet available after 5 min for account: {}", phylloAccountId);
                        // Schedule a final retry in 30 minutes
                        scheduleRetrySync(phylloAccountId, creatorId);
                    } else {
                        log.info("Historic data arrived! Synced {} posts for account: {}", phylloIdToPost.size(), phylloAccountId);
                    }
                }
            }

            // Sync comments (for whatever posts we have)
            if (!phylloIdToPost.isEmpty()) {
                // Wait 1 minute for Phyllo to index comments after posts are available
                log.info("  → Waiting 1 minute for Phyllo to index comments...");
                Thread.sleep(COMMENTS_DELAY_MS);
                syncComments(phylloAccountId, creator, phylloIdToPost);
            }

            // --- Compute Data Freshness ---
            computeDataFreshness(creator);

            // --- Sync Status: COMPLETED ---
            updateSyncStatus(creatorId, "COMPLETED", null);

            // Update last synced timestamp
            transactionTemplate.executeWithoutResult(status -> {
                Creator c = creatorRepository.findById(creatorId).orElse(null);
                if (c != null) {
                    c.setLastSyncedAt(LocalDateTime.now());
                    creatorRepository.save(c);
                }
            });

            // Update social account status
            socialAccountRepository.findByPhylloAccountId(phylloAccountId).ifPresent(account -> {
                account.setLastSyncedAt(LocalDateTime.now());
                socialAccountRepository.save(account);
            });

            // Generate analytics
            if (!phylloIdToPost.isEmpty()) {
                log.info("Computing analytics for creator: {} (@{})", creatorId, creator.getUsername());
                analyticsProcessingService.processCreatorAnalytics(creator);
            }

            log.info("Sync completed for creator: {} (@{})", creatorId, creator.getUsername());

            // Send email notification
            sendSyncCompleteEmail(creatorId, phylloIdToPost.size());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            updateSyncStatus(creatorId, "FAILED", "Sync interrupted");
            log.error("Sync interrupted for Phyllo account: {}", phylloAccountId);
        } catch (Exception e) {
            updateSyncStatus(creatorId, "FAILED", e.getMessage() != null ? e.getMessage() : "Unknown error");
            log.error("Sync failed for Phyllo account: {}", phylloAccountId, e);
        }
    }

    /**
     * Retry sync — called 30 min after initial sync if historic data wasn't ready.
     * Fetches any new posts + comments. No email notification (user already got one).
     */
    @Async
    public void retrySyncPosts(String phylloAccountId, Long creatorId) {
        log.info("Retry sync for Phyllo account: {} → creator: {}", phylloAccountId, creatorId);

        Creator creator = creatorRepository.findById(creatorId).orElse(null);
        if (creator == null) return;

        try {
            Map<String, Post> phylloIdToPost = syncPosts(phylloAccountId, creator);

            if (!phylloIdToPost.isEmpty()) {
                log.info("Retry sync got {} posts for account: {}", phylloIdToPost.size(), phylloAccountId);

                // Wait 1 min for comments to be indexed too
                Thread.sleep(COMMENTS_DELAY_MS);
                syncComments(phylloAccountId, creator, phylloIdToPost);
                computeDataFreshness(creator);
                updateSyncStatus(creatorId, "COMPLETED", null);

                transactionTemplate.executeWithoutResult(status -> {
                    Creator c = creatorRepository.findById(creatorId).orElse(null);
                    if (c != null) {
                        c.setLastSyncedAt(LocalDateTime.now());
                        creatorRepository.save(c);
                    }
                });

                analyticsProcessingService.processCreatorAnalytics(creator);
                // No email for retry — user already received the initial sync email
            } else {
                log.warn("Retry sync still found 0 posts for account: {} — giving up", phylloAccountId);
                updateSyncStatus(creatorId, "COMPLETED", null); // Mark as done anyway
            }
        } catch (Exception e) {
            log.error("Retry sync failed for account: {}", phylloAccountId, e);
        }
    }

    /**
     * Updates sync status in its own transaction — immediately visible to frontend.
     */
    private void updateSyncStatus(Long creatorId, String status, String error) {
        transactionTemplate.executeWithoutResult(txStatus -> {
            Creator c = creatorRepository.findById(creatorId).orElse(null);
            if (c != null) {
                c.setSyncStatus(status);
                if ("SYNCING".equals(status)) {
                    c.setSyncStartedAt(LocalDateTime.now());
                    c.setSyncError(null);
                } else if ("COMPLETED".equals(status)) {
                    c.setSyncCompletedAt(LocalDateTime.now());
                    c.setSyncError(null);
                } else if ("FAILED".equals(status)) {
                    c.setSyncCompletedAt(LocalDateTime.now());
                    c.setSyncError(error);
                }
                creatorRepository.save(c);
            }
        });
        log.info("  → Sync status updated to: {} for creator: {}", status, creatorId);
    }

    /**
     * Schedule a retry in 30 minutes (using a simple async delay).
     */
    private void scheduleRetrySync(String phylloAccountId, Long creatorId) {
        log.info("  → Scheduling retry sync in 30 minutes for account: {}", phylloAccountId);
        // Run in a new async thread with delay
        new Thread(() -> {
            try {
                Thread.sleep(RETRY_DELAY_MS);
                retrySyncPosts(phylloAccountId, creatorId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    /**
     * Send email notification when sync completes.
     */
    private void sendSyncCompleteEmail(Long creatorId, int postsCount) {
        try {
            // Find the user email via social account link
            socialAccountRepository.findAll().stream()
                .filter(a -> a.getCreator() != null && a.getCreator().getId().equals(creatorId))
                .findFirst()
                .ifPresent(account -> {
                    // Look up user email from the user table via userId
                    // For now, we'll use a simple approach — the creator's email or username
                    Creator creator = account.getCreator();
                    String platform = account.getPlatform() != null ? account.getPlatform() : "social media";
                    String username = account.getPlatformUsername() != null ? account.getPlatformUsername() : "your account";

                    // Find user email — check if creator has an email (set during profile sync sometimes)
                    // If not available, skip email notification
                    if (creator.getEmail() != null && !creator.getEmail().isEmpty()) {
                        emailService.sendSyncCompleteEmail(creator.getEmail(), username, platform, postsCount);
                    } else {
                        log.info("  → No email available for creator {} — skipping notification", creatorId);
                    }
                });
        } catch (Exception e) {
            log.warn("  → Failed to send sync completion email: {}", e.getMessage());
        }
    }

    /**
     * Computes data freshness based on the latest and oldest post dates.
     * RECENT = latest post within 90 days
     * HISTORIC = has posts but all older than 90 days
     * STALE = no posts at all
     */
    private void computeDataFreshness(Creator creator) {
        transactionTemplate.executeWithoutResult(status -> {
            List<Post> posts = postRepository.findByCreatorIdOrderByPostedAtDesc(creator.getId());

            Creator c = creatorRepository.findById(creator.getId()).orElse(null);
            if (c == null) return;

            if (posts.isEmpty()) {
                c.setDataFreshness("STALE");
                c.setLatestPostDate(null);
                c.setOldestPostDate(null);
                creatorRepository.save(c);
                log.info("  → Data freshness: STALE (no posts)");
                return;
            }

            LocalDateTime latestDate = posts.stream()
                .map(Post::getPostedAt)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);

            LocalDateTime oldestDate = posts.stream()
                .map(Post::getPostedAt)
                .filter(Objects::nonNull)
                .min(LocalDateTime::compareTo)
                .orElse(null);

            c.setLatestPostDate(latestDate);
            c.setOldestPostDate(oldestDate);

            if (latestDate != null && latestDate.isAfter(LocalDateTime.now().minusDays(90))) {
                c.setDataFreshness("RECENT");
                log.info("  → Data freshness: RECENT (latest post: {})", latestDate);
            } else {
                c.setDataFreshness("HISTORIC");
                log.info("  → Data freshness: HISTORIC (latest post: {})", latestDate);
            }
            creatorRepository.save(c);
        });
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
            log.info("  → Synced profile for @{} (followers: {})", creator.getUsername(), creator.getFollowerCount());
        } catch (Exception e) {
            log.warn("  → Failed to sync profile: {}", e.getMessage());
        }
    }

    private Map<String, Post> syncPosts(String accountId, Creator creator) {
        Map<String, Post> phylloIdToPost = new HashMap<>();

        try {
            // Get existing post IDs for this creator to avoid duplicates on reconnect
            Set<String> existingPostIds = new HashSet<>(postRepository.findPhylloIdsByCreatorId(creator.getId()));

            int limit = 100;
            int offset = 0;
            int totalFetched = 0;
            int skipped = 0;
            boolean hasMore = true;

            while (hasMore) {
                JsonNode contentData = phylloService.fetchContents(accountId, limit, offset);
                JsonNode posts = contentData.get("data");
                if (posts == null || !posts.isArray() || posts.size() == 0) break;

                // Build post entities, skipping any that already exist
                List<Post> newPosts = new ArrayList<>();
                for (JsonNode postNode : posts) {
                    String phylloPostId = postNode.get("id").asText();
                    if (existingPostIds.contains(phylloPostId)) {
                        skipped++;
                        // Still add to result map so comments/analytics can reference them
                        postRepository.findByPhylloId(phylloPostId).ifPresent(existing ->
                            phylloIdToPost.put(existing.getPhylloId(), existing)
                        );
                        continue;
                    }
                    newPosts.add(mapPhylloPost(postNode, creator));
                }

                // Batch insert only new posts
                if (!newPosts.isEmpty()) {
                    List<Post> saved = postRepository.saveAll(newPosts);
                    for (Post p : saved) {
                        phylloIdToPost.put(p.getPhylloId(), p);
                        existingPostIds.add(p.getPhylloId()); // prevent double-insert across pages
                    }
                }

                totalFetched += posts.size();

                // Check pagination metadata — Phyllo returns metadata.total or we check if page is full
                JsonNode metadata = contentData.get("metadata");
                if (metadata != null && metadata.has("total")) {
                    int total = metadata.get("total").asInt();
                    if (totalFetched >= total) {
                        hasMore = false;
                    }
                } else {
                    // No metadata — stop if we got fewer items than the limit (last page)
                    if (posts.size() < limit) {
                        hasMore = false;
                    }
                }

                offset += posts.size();
                log.info("  → Fetched page: offset={}, got={}, totalSoFar={}", offset - posts.size(), posts.size(), totalFetched);
            }

            int newCount = phylloIdToPost.size() - skipped;
            if (newCount > 0 || skipped > 0) {
                log.info("  → Synced {} new posts, skipped {} existing (total fetched from API: {})", 
                    phylloIdToPost.size() - skipped, skipped, totalFetched);
            }
        } catch (Exception e) {
            log.warn("  → Failed to sync posts: {}", e.getMessage());
        }

        return phylloIdToPost;
    }

    private void syncComments(String accountId, Creator creator, Map<String, Post> phylloIdToPost) {
        // Only fetch comments for last 15 posts
        List<Post> recentPosts = phylloIdToPost.values().stream()
            .sorted(Comparator.comparing(Post::getPostedAt, Comparator.nullsLast(Comparator.reverseOrder())))
            .limit(15)
            .collect(Collectors.toList());

        // Get existing comment IDs to avoid duplicates on reconnect
        Set<String> existingCommentIds = new HashSet<>(commentRepository.findPhylloIdsByCreatorId(creator.getId()));

        List<Comment> newComments = new ArrayList<>();
        int skipped = 0;

        for (Post post : recentPosts) {
            try {
                JsonNode commentData = phylloService.fetchComments(accountId, post.getPhylloId(), 100);
                JsonNode comments = commentData.get("data");
                if (comments == null || !comments.isArray()) continue;

                for (JsonNode commentNode : comments) {
                    String commentId = commentNode.get("id").asText();
                    if (existingCommentIds.contains(commentId)) {
                        skipped++;
                        continue;
                    }
                    newComments.add(mapPhylloComment(commentNode, post, creator.getId()));
                }
            } catch (Exception e) {
                // Non-critical — some posts may have no comments
            }
        }

        // Batch insert only new comments
        if (!newComments.isEmpty()) {
            commentRepository.saveAll(newComments);
            log.info("  → Synced {} new comments (skipped {} existing, from {} posts)", newComments.size(), skipped, recentPosts.size());
        } else if (skipped > 0) {
            log.info("  → All comments already exist — skipped {} (reconnect scenario)", skipped);
        }
    }

    // --- Mappers: Phyllo JSON → Maya entities ---

    private Post mapPhylloPost(JsonNode node, Creator creator) {
        Post post = new Post();
        post.setPhylloId(node.get("id").asText());
        post.setExternalId(getTextOrNull(node, "external_id"));
        post.setPlatform(creator.getPlatform());
        post.setCreator(creator);

        // Title + caption (Phyllo "description" -> caption; fall back to "title")
        post.setTitle(getTextOrNull(node, "title"));
        String caption = getTextOrNull(node, "description");
        if (caption == null) caption = getTextOrNull(node, "title");
        post.setCaption(caption != null ? caption : "");

        // Format and type
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
            computeRates(metrics);
            post.setMetrics(metrics);
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
        comment.setIsQuestion(isQuestion(comment.getText()));

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

    // --- Helpers (same logic as DataSeedService) ---

    private void computeRates(PostMetrics metrics) {
        Integer reach = metrics.getReachOrganicCount();
        int likes = metrics.getLikeCount() != null ? metrics.getLikeCount() : 0;
        int comments = metrics.getCommentCount() != null ? metrics.getCommentCount() : 0;
        int saves = metrics.getSaveCount() != null ? metrics.getSaveCount() : 0;
        int shares = metrics.getShareCount() != null ? metrics.getShareCount() : 0;
        int totalEngagement = likes + comments + saves + shares;

        // Validate reach: if reach < totalEngagement, it's clearly bad data from the platform
        // Treat it as null (unreliable) rather than producing inflated rates
        boolean reachReliable = reach != null && reach > 0 && reach >= totalEngagement;

        if (reachReliable) {
            if (metrics.getSaveCount() != null)
                metrics.setSaveRate(Math.min(metrics.getSaveCount() * 100.0 / reach, 100.0));
            if (metrics.getShareCount() != null)
                metrics.setShareRate(Math.min(metrics.getShareCount() * 100.0 / reach, 100.0));
            double er = totalEngagement * 100.0 / reach;
            metrics.setEngagementRate(Math.min(er, 100.0)); // cap at 100%
        }
        // If reach is unreliable, leave rates as null — dashboard will handle gracefully
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

    private Long getLongOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return (value != null && !value.isNull()) ? value.asLong() : null;
    }

    private Double getDoubleOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return (value != null && !value.isNull()) ? value.asDouble() : null;
    }

    /**
     * Join a Phyllo JSON array of strings into a comma-separated string.
     * Returns null if the node is missing/empty.
     */
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
