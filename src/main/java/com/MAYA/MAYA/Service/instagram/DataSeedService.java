package com.MAYA.MAYA.Service.instagram;

import com.MAYA.MAYA.Entity.instagram.*;
import com.MAYA.MAYA.Repository.instagram.*;
import com.MAYA.MAYA.Service.analytics.AnalyticsProcessingService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Seeds ONE demo creator (fitlife_by_meera) on first startup.
 * Uses raw JDBC batch insert for maximum speed (~5 seconds instead of 3-4 minutes).
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
    private final JdbcTemplate jdbcTemplate;

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
            wipeDemoData(existing.get().getId());
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

        // Create creator (single insert via JPA — fine)
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
        Long creatorId = creator.getId();
        log.info("Created demo creator: {} (id: {})", DEMO_USERNAME, creatorId);

        // JDBC batch insert posts
        Map<String, Long> phylloIdToPostId = batchInsertPosts(postsArray, creatorId);
        log.info("Inserted {} posts via JDBC batch", phylloIdToPostId.size());

        // JDBC batch insert comments
        if (commentData != null && commentData.has("data")) {
            JsonNode commentsArray = commentData.get("data");
            if (commentsArray != null && commentsArray.isArray()) {
                int commentCount = batchInsertComments(commentsArray, phylloIdToPostId, creatorId);
                log.info("Inserted {} comments via JDBC batch", commentCount);
            }
        }

        long elapsed = (System.currentTimeMillis() - startTime) / 1000;
        log.info("Demo data seeded in {}s!", elapsed);

        // Compute analytics
        log.info("Computing analytics...");
        try {
            Creator finalCreator = creatorRepository.findById(creatorId).orElse(creator);
            analyticsProcessingService.processCreatorAnalytics(finalCreator);
            log.info("Analytics computed!");
        } catch (Exception e) {
            log.warn("Analytics failed: {}", e.getMessage());
        }
    }

    /**
     * Batch insert posts using raw JDBC. Returns map of phylloPostId → generated DB id.
     */
    private Map<String, Long> batchInsertPosts(JsonNode postsArray, Long creatorId) {
        String sql = """
            INSERT INTO posts (instagram_id, creator_id, caption, caption_length, media_type, media_product_type,
                media_url, permalink, thumbnail_url, hashtags, hashtag_count, has_cta, cta_type, has_question,
                likes, comments, saves, shares, reposts, reach, impressions, plays,
                engagement_rate, save_rate, share_rate, view_count, posted_at, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        // Build data rows
        List<Object[]> rows = new ArrayList<>();
        List<String> instagramIds = new ArrayList<>();

        for (JsonNode postNode : postsArray) {
            String instagramId = postNode.get("id").asText();
            instagramIds.add(instagramId);
            String caption = getTextOrNull(postNode, "title");
            if (caption == null) caption = "";
            int captionLength = caption.length();
            String mediaType = getTextOrNull(postNode, "format");
            String mediaProductType = getTextOrNull(postNode, "type");
            String mediaUrl = getTextOrNull(postNode, "media_url");
            String permalink = getTextOrNull(postNode, "url");
            String thumbnailUrl = getTextOrNull(postNode, "thumbnail_url");

            // Hashtags
            String hashtags = null;
            int hashtagCount = 0;
            JsonNode hashtagsNode = postNode.get("hashtags");
            if (hashtagsNode != null && hashtagsNode.isArray() && !hashtagsNode.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < hashtagsNode.size(); i++) {
                    if (i > 0) sb.append(",");
                    sb.append(hashtagsNode.get(i).asText());
                }
                hashtags = sb.toString();
                hashtagCount = hashtagsNode.size();
            }

            // CTA detection
            boolean hasCta = false;
            String ctaType = null;
            boolean hasQuestion = false;
            if (!caption.isEmpty()) {
                String lower = caption.toLowerCase();
                if (lower.contains("link in bio") || lower.contains("bio link")) { hasCta = true; ctaType = "LINK_IN_BIO"; }
                else if (lower.contains("comment below") || lower.contains("drop a comment")) { hasCta = true; ctaType = "COMMENT_BELOW"; }
                else if (lower.contains("save this") || lower.contains("save for later")) { hasCta = true; ctaType = "SAVE_THIS"; }
                else if (lower.contains("share this") || lower.contains("tag someone")) { hasCta = true; ctaType = "SHARE_THIS"; }
                else if (lower.trim().endsWith("?")) { hasCta = true; ctaType = "QUESTION_CTA"; }
                if (lower.trim().endsWith("?")) hasQuestion = true;
            }

            // Metrics
            JsonNode engagement = postNode.get("engagement");
            Integer likes = getIntOrZero(engagement, "like_count");
            Integer commentCount2 = getIntOrZero(engagement, "comment_count");
            Integer saves = getIntOrNull(engagement, "save_count");
            Integer shares = getIntOrNull(engagement, "share_count");
            Integer reposts = getIntOrNull(engagement, "repost_count");
            Integer reach = getIntOrNull(engagement, "reach_organic_count");
            Integer impressions = getIntOrNull(engagement, "impression_organic_count");
            Integer plays = getIntOrNull(engagement, "view_count");

            Double engagementRate = null, saveRate = null, shareRate = null;
            if (reach != null && reach > 0) {
                int s = saves != null ? saves : 0;
                int sh = shares != null ? shares : 0;
                engagementRate = (likes + commentCount2 + s + sh) * 100.0 / reach;
                if (saves != null) saveRate = saves * 100.0 / reach;
                if (shares != null) shareRate = shares * 100.0 / reach;
            }

            Long viewCount = plays != null ? plays.longValue() : null;
            LocalDateTime postedAt = LocalDateTime.parse(postNode.get("published_at").asText(), DateTimeFormatter.ISO_DATE_TIME);
            LocalDateTime now = LocalDateTime.now();

            rows.add(new Object[]{
                instagramId, creatorId, caption, captionLength, mediaType, mediaProductType,
                mediaUrl, permalink, thumbnailUrl, hashtags, hashtagCount, hasCta, ctaType, hasQuestion,
                likes, commentCount2, saves, shares, reposts, reach, impressions, plays,
                engagementRate, saveRate, shareRate, viewCount, Timestamp.valueOf(postedAt), Timestamp.valueOf(now), Timestamp.valueOf(now)
            });
        }

        // TRUE batch insert — all posts in batches of 100
        jdbcTemplate.batchUpdate(sql, rows, 100, (PreparedStatement ps, Object[] row) -> {
            for (int i = 0; i < row.length; i++) {
                if (row[i] == null) {
                    ps.setNull(i + 1, java.sql.Types.NULL);
                } else if (row[i] instanceof String) {
                    ps.setString(i + 1, (String) row[i]);
                } else if (row[i] instanceof Long) {
                    ps.setLong(i + 1, (Long) row[i]);
                } else if (row[i] instanceof Integer) {
                    ps.setInt(i + 1, (Integer) row[i]);
                } else if (row[i] instanceof Double) {
                    ps.setDouble(i + 1, (Double) row[i]);
                } else if (row[i] instanceof Boolean) {
                    ps.setBoolean(i + 1, (Boolean) row[i]);
                } else if (row[i] instanceof Timestamp) {
                    ps.setTimestamp(i + 1, (Timestamp) row[i]);
                }
            }
        });

        // After batch insert, query back the IDs by instagram_id (one query, not N)
        Map<String, Long> phylloIdToPostId = new HashMap<>();
        List<Map<String, Object>> results = jdbcTemplate.queryForList(
            "SELECT id, instagram_id FROM posts WHERE creator_id = ?", creatorId);
        for (Map<String, Object> row : results) {
            phylloIdToPostId.put((String) row.get("instagram_id"), ((Number) row.get("id")).longValue());
        }

        return phylloIdToPostId;
    }

    /**
     * Batch insert comments using raw JDBC.
     */
    private int batchInsertComments(JsonNode commentsArray, Map<String, Long> phylloIdToPostId, Long creatorId) {
        String sql = """
            INSERT INTO comments (instagram_id, post_id, creator_id, username, text, like_count, reply_count,
                is_question, commented_at, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        List<Object[]> rows = new ArrayList<>();
        for (JsonNode commentNode : commentsArray) {
            JsonNode contentNode = commentNode.get("content");
            if (contentNode == null) continue;

            String phylloPostId = contentNode.get("id").asText();
            Long postId = phylloIdToPostId.get(phylloPostId);
            if (postId == null) continue;

            String instagramId = commentNode.get("id").asText();
            String username = commentNode.get("commenter_username").asText();
            String text = commentNode.get("text").asText();
            int likeCount = getIntOrZero(commentNode, "like_count");
            int replyCount = getIntOrZero(commentNode, "reply_count");

            String lower = text.trim().toLowerCase();
            boolean isQuestion = lower.endsWith("?") ||
                lower.matches("^(how|what|when|where|why|which|can|do|did|is|are|should|would|could|will)\\b.*");

            LocalDateTime commentedAt = LocalDateTime.parse(commentNode.get("published_at").asText(), DateTimeFormatter.ISO_DATE_TIME);
            LocalDateTime now = LocalDateTime.now();

            rows.add(new Object[]{
                instagramId, postId, creatorId, username, text, likeCount, replyCount,
                isQuestion, Timestamp.valueOf(commentedAt), Timestamp.valueOf(now)
            });
        }

        // True batch insert — sends all rows in one network call
        jdbcTemplate.batchUpdate(sql, rows, 500, (PreparedStatement ps, Object[] row) -> {
            ps.setString(1, (String) row[0]);
            ps.setLong(2, (Long) row[1]);
            ps.setLong(3, (Long) row[2]);
            ps.setString(4, (String) row[3]);
            ps.setString(5, (String) row[4]);
            ps.setInt(6, (int) row[5]);
            ps.setInt(7, (int) row[6]);
            ps.setBoolean(8, (boolean) row[7]);
            ps.setTimestamp(9, (Timestamp) row[8]);
            ps.setTimestamp(10, (Timestamp) row[9]);
        });

        return rows.size();
    }

    private void wipeDemoData(Long creatorId) {
        jdbcTemplate.update("DELETE FROM comments WHERE creator_id = ?", creatorId);
        jdbcTemplate.update("DELETE FROM posts WHERE creator_id = ?", creatorId);
        log.info("Wiped partial demo data");
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
