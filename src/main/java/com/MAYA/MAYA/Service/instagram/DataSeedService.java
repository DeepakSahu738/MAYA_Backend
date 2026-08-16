package com.MAYA.MAYA.Service.instagram;

import com.MAYA.MAYA.Entity.instagram.*;
import com.MAYA.MAYA.Repository.instagram.*;
import com.MAYA.MAYA.Service.analytics.AnalyticsProcessingService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Seeds ONE demo creator (fitlife_by_meera) on first startup.
 * Uses JPA saveAll() batch insert.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Order(1)
public class DataSeedService implements CommandLineRunner {

    private final DummyGraphApiService dummyGraphApiService;
    private final CreatorRepository creatorRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final AnalyticsProcessingService analyticsProcessingService;

    private static final String DEMO_USERNAME = "fitlife_by_meera";
    private static final int EXPECTED_POST_COUNT = 300;

    @Override
    public void run(String... args) {
        new Thread(() -> {
            try {
                seed();
            } catch (Exception e) {
                log.error("Seeding failed: {}", e.getMessage(), e);
            }
        }, "data-seed-thread").start();
    }

    private void seed() {
        log.info("Checking database seed status...");
        long startTime = System.currentTimeMillis();

        // Quick exit
        var existing = creatorRepository.findByUsername(DEMO_USERNAME);
        if (existing.isPresent()) {
            long postCount = postRepository.findByCreatorIdOrderByPostedAtDesc(existing.get().getId()).size();
            if (postCount >= EXPECTED_POST_COUNT) {
                log.info("Demo data fully seeded ({} posts). Skipping.", postCount);
                return;
            }
            log.info("Partial demo data ({} posts). Wiping and re-seeding...", postCount);
            commentRepository.deleteByCreatorId(existing.get().getId());
            postRepository.deleteByCreatorId(existing.get().getId());
            creatorRepository.delete(existing.get());
        }

        // Load JSON
        JsonNode postData = dummyGraphApiService.loadPostData("fitlife_by_meera");
        JsonNode commentData = dummyGraphApiService.loadCommentData("fitlife_by_meera");

        if (postData == null || !postData.has("data")) {
            log.error("Could not load fitlife_by_meera post data");
            return;
        }

        JsonNode postsArray = postData.get("data");
        if (postsArray == null || !postsArray.isArray() || postsArray.isEmpty()) {
            log.error("No posts in data file");
            return;
        }

        // Create creator
        JsonNode firstPost = postsArray.get(0);
        Creator creator = new Creator();
        JsonNode account = firstPost.get("account");
        creator.setInstagramId(account.has("id") ? account.get("id").asText() : DEMO_USERNAME);
        creator.setUsername(DEMO_USERNAME);
        creator.setNiche("Fitness");
        creator.setConnectedAt(LocalDateTime.now());
        creator.setLastSyncedAt(LocalDateTime.now());
        creator.setIsActive(true);
        creator = creatorRepository.save(creator);
        log.info("Created demo creator: {} (id: {})", DEMO_USERNAME, creator.getId());

        // Batch insert posts
        List<Post> postBatch = new ArrayList<>();
        Map<String, Post> phylloIdToPost = new HashMap<>();

        for (JsonNode postNode : postsArray) {
            postBatch.add(buildPost(creator, postNode));
        }

        List<Post> savedPosts = postRepository.saveAll(postBatch);
        for (Post p : savedPosts) {
            phylloIdToPost.put(p.getInstagramId(), p);
        }
        log.info("Inserted {} posts", savedPosts.size());

        // Batch insert comments
        if (commentData != null && commentData.has("data")) {
            JsonNode commentsArray = commentData.get("data");
            if (commentsArray != null && commentsArray.isArray()) {
                List<Comment> commentBatch = new ArrayList<>();

                for (JsonNode commentNode : commentsArray) {
                    Comment comment = buildComment(commentNode, phylloIdToPost, creator.getId());
                    if (comment != null) {
                        commentBatch.add(comment);
                    }
                }

                if (!commentBatch.isEmpty()) {
                    commentRepository.saveAll(commentBatch);
                    log.info("Inserted {} comments", commentBatch.size());
                }
            }
        }

        long elapsed = (System.currentTimeMillis() - startTime) / 1000;
        log.info("Demo data seeded in {}s!", elapsed);

        // Compute analytics
        log.info("Computing analytics...");
        try {
            analyticsProcessingService.processCreatorAnalytics(creator);
            log.info("Analytics computed!");
        } catch (Exception e) {
            log.warn("Analytics failed: {}", e.getMessage());
        }
    }

    // --- Build entities ---

    private Post buildPost(Creator creator, JsonNode postNode) {
        Post post = new Post();
        post.setInstagramId(postNode.get("id").asText());
        post.setCreator(creator);

        String caption = getTextOrNull(postNode, "title");
        post.setCaption(caption != null ? caption : "");
        post.setMediaType(getTextOrNull(postNode, "format"));
        post.setMediaProductType(getTextOrNull(postNode, "type"));
        post.setMediaUrl(getTextOrNull(postNode, "media_url"));
        post.setPermalink(getTextOrNull(postNode, "url"));
        post.setThumbnailUrl(getTextOrNull(postNode, "thumbnail_url"));

        // Hashtags
        JsonNode hashtagsNode = postNode.get("hashtags");
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
        JsonNode engagement = postNode.get("engagement");
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

        // Timestamp
        String timestamp = postNode.get("published_at").asText();
        post.setPostedAt(LocalDateTime.parse(timestamp, DateTimeFormatter.ISO_DATE_TIME));

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

    private Comment buildComment(JsonNode commentNode, Map<String, Post> phylloIdToPost, Long creatorId) {
        JsonNode contentNode = commentNode.get("content");
        if (contentNode == null) return null;

        String phylloPostId = contentNode.get("id").asText();
        Post post = phylloIdToPost.get(phylloPostId);
        if (post == null) return null;

        Comment comment = new Comment();
        comment.setInstagramId(commentNode.get("id").asText());
        comment.setPost(post);
        comment.setCreatorId(creatorId);
        comment.setUsername(commentNode.get("commenter_username").asText());
        comment.setText(commentNode.get("text").asText());
        comment.setLikeCount(getIntOrZero(commentNode, "like_count"));
        comment.setReplyCount(getIntOrZero(commentNode, "reply_count"));

        String text = comment.getText().trim().toLowerCase();
        comment.setIsQuestion(text.endsWith("?") ||
            text.matches("^(how|what|when|where|why|which|can|do|did|is|are|should|would|could|will)\\b.*"));

        String timestamp = commentNode.get("published_at").asText();
        comment.setCommentedAt(LocalDateTime.parse(timestamp, DateTimeFormatter.ISO_DATE_TIME));

        return comment;
    }

    // --- Utility ---
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
