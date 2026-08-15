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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    
    @Override
    public void run(String... args) {
        // Run seeding in a separate thread so it doesn't block app startup
        // Cloud Run needs the app to respond to health checks quickly
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
        
        // Quick exit: if all 4 demo creators exist AND have comments, skip entirely
        long creatorCount = creatorRepository.count();
        long commentCount = commentRepository.count();
        if (creatorCount >= 4 && commentCount > 0) {
            log.info("Database already fully seeded ({} creators, {} comments). Skipping.", creatorCount, commentCount);
            return;
        }
        
        List<JsonNode> allPostData = dummyGraphApiService.loadAllPostData();
        List<JsonNode> allCommentData = dummyGraphApiService.loadAllCommentData();
        
        // Build a map of phyllo post id -> Post entity for comment linking
        Map<String, Post> phylloIdToPost = new HashMap<>();
        
        for (int i = 0; i < allPostData.size(); i++) {
            JsonNode postFile = allPostData.get(i);
            JsonNode postsArray = postFile.get("data");
            
            if (postsArray == null || !postsArray.isArray() || postsArray.isEmpty()) {
                continue;
            }
            
            // Extract creator info from first post's account field
            JsonNode firstPost = postsArray.get(0);
            String username = firstPost.get("account").get("platform_username").asText();
            
            // Per-creator skip check
            if (creatorRepository.findByUsername(username).isPresent()) {
                log.info("Creator {} already seeded, skipping...", username);
                // Still populate the map for comment linking if needed
                List<Post> existingPosts = postRepository.findByCreatorIdOrderByPostedAtDesc(
                    creatorRepository.findByUsername(username).get().getId());
                for (Post p : existingPosts) {
                    phylloIdToPost.put(p.getInstagramId(), p);
                }
                continue;
            }
            
            Creator creator = seedCreator(firstPost);
            log.info("Seeded creator: {} ({} posts)", username, postsArray.size());
            
            for (JsonNode postNode : postsArray) {
                Post post = seedPost(creator, postNode);
                phylloIdToPost.put(postNode.get("id").asText(), post);
            }
        }
        
        // Seed comments from separate comment files
        for (JsonNode commentFile : allCommentData) {
            JsonNode commentsArray = commentFile.get("data");
            if (commentsArray == null || !commentsArray.isArray()) {
                continue;
            }
            
            for (JsonNode commentNode : commentsArray) {
                seedComment(commentNode, phylloIdToPost);
            }
        }
        
        log.info("Database seed check completed!");
        
        // Now run analytics processing on seeded data
        log.info("Starting analytics processing for demo creators...");
        List<Creator> creators = creatorRepository.findAll();
        for (Creator creator : creators) {
            try {
                analyticsProcessingService.processCreatorAnalytics(creator);
            } catch (Exception e) {
                log.warn("Analytics processing failed for {}: {}", creator.getUsername(), e.getMessage());
            }
        }
        log.info("Analytics processing completed!");
    }
    
    private Creator seedCreator(JsonNode firstPost) {
        JsonNode account = firstPost.get("account");
        
        Creator creator = new Creator();
        creator.setInstagramId(account.has("id") ? account.get("id").asText() : account.get("platform_username").asText());
        creator.setUsername(account.get("platform_username").asText());
        creator.setNiche(detectNiche(account.get("platform_username").asText()));
        creator.setConnectedAt(LocalDateTime.now());
        creator.setLastSyncedAt(LocalDateTime.now());
        creator.setIsActive(true);
        
        return creatorRepository.save(creator);
    }
    
    private Post seedPost(Creator creator, JsonNode postNode) {
        Post post = new Post();
        post.setInstagramId(postNode.get("id").asText());
        post.setCreator(creator);
        
        // Caption from Phyllo "title" field
        String caption = getTextOrNull(postNode, "title");
        post.setCaption(caption != null ? caption : "");
        
        // Format and type
        post.setMediaType(getTextOrNull(postNode, "format"));
        post.setMediaProductType(getTextOrNull(postNode, "type"));
        post.setMediaUrl(getTextOrNull(postNode, "media_url"));
        post.setPermalink(getTextOrNull(postNode, "url"));
        post.setThumbnailUrl(getTextOrNull(postNode, "thumbnail_url"));
        
        // Hashtags — pre-parsed array from Phyllo
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
        
        // Engagement metrics — preserve nulls
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
        
        // Compute rates where data is available
        computeRates(metrics);
        
        post.setMetrics(metrics);
        
        // View count on post level too (for VIDEO posts)
        Integer viewCount = getIntOrNull(engagement, "view_count");
        post.setViewCount(viewCount != null ? viewCount.longValue() : null);
        
        // Timestamp
        String timestamp = postNode.get("published_at").asText();
        post.setPostedAt(LocalDateTime.parse(timestamp, DateTimeFormatter.ISO_DATE_TIME));
        
        // CTA detection
        detectCta(post, caption);
        
        // Question detection in caption
        if (caption != null && caption.trim().endsWith("?")) {
            post.setHasQuestion(true);
        }
        
        return postRepository.save(post);
    }
    
    private void seedComment(JsonNode commentNode, Map<String, Post> phylloIdToPost) {
        // Link comment to post via Phyllo content.id
        JsonNode contentNode = commentNode.get("content");
        if (contentNode == null) return;
        
        String phylloPostId = contentNode.get("id").asText();
        Post post = phylloIdToPost.get(phylloPostId);
        if (post == null) return;
        
        // Skip if already seeded
        String commentId = commentNode.get("id").asText();
        if (commentRepository.findByInstagramId(commentId).isPresent()) {
            return;
        }
        
        Comment comment = new Comment();
        comment.setInstagramId(commentId);
        comment.setPost(post);
        comment.setCreatorId(post.getCreator().getId());
        comment.setUsername(commentNode.get("commenter_username").asText());
        comment.setText(commentNode.get("text").asText());
        
        // Like and reply counts
        comment.setLikeCount(getIntOrZero(commentNode, "like_count"));
        comment.setReplyCount(getIntOrZero(commentNode, "reply_count"));
        
        // Question detection
        String text = comment.getText();
        comment.setIsQuestion(isQuestion(text));
        
        // Timestamp
        String timestamp = commentNode.get("published_at").asText();
        comment.setCommentedAt(LocalDateTime.parse(timestamp, DateTimeFormatter.ISO_DATE_TIME));
        
        commentRepository.save(comment);
    }
    
    // --- Helper methods ---
    
    private void computeRates(PostMetrics metrics) {
        Integer reach = metrics.getReach();
        
        // Save rate: only when BOTH saves and reach are non-null
        if (metrics.getSaves() != null && reach != null && reach > 0) {
            metrics.setSaveRate(metrics.getSaves() * 100.0 / reach);
        }
        
        // Share rate: only when BOTH shares and reach are non-null
        if (metrics.getShares() != null && reach != null && reach > 0) {
            metrics.setShareRate(metrics.getShares() * 100.0 / reach);
        }
        
        // Engagement rate: gated by reach
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
            post.setHasCta(true);
            post.setCtaType("LINK_IN_BIO");
        } else if (lower.contains("comment below") || lower.contains("drop a comment") || lower.contains("tell me in comments")) {
            post.setHasCta(true);
            post.setCtaType("COMMENT_BELOW");
        } else if (lower.contains("save this") || lower.contains("save for later") || lower.contains("bookmark this")) {
            post.setHasCta(true);
            post.setCtaType("SAVE_THIS");
        } else if (lower.contains("share this") || lower.contains("tag someone") || lower.contains("tag a friend")) {
            post.setHasCta(true);
            post.setCtaType("SHARE_THIS");
        } else if (lower.trim().endsWith("?")) {
            post.setHasCta(true);
            post.setCtaType("QUESTION_CTA");
        }
    }
    
    private boolean isQuestion(String text) {
        String t = text.trim().toLowerCase();
        return t.endsWith("?")
            || t.matches("^(how|what|when|where|why|which|can|do|did|is|are|should|would|could|will)\\b.*");
    }
    
    private String detectNiche(String username) {
        if (username.contains("fit") || username.contains("health")) return "Fitness";
        if (username.contains("tech")) return "Technology";
        if (username.contains("travel")) return "Travel";
        return "Lifestyle";
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
