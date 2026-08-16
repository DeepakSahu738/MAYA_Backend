package com.MAYA.MAYA.Service.analytics;

import com.MAYA.MAYA.DTO.analytics.*;
import com.MAYA.MAYA.Entity.instagram.*;
import com.MAYA.MAYA.Repository.instagram.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Runs after DataSeedService (Order=2) to populate derived tables:
 * - hashtag_performance
 * - top_commenters
 * - weekly_reports
 *
 * In production, this logic runs via @Scheduled jobs.
 * For the demo, it runs once on startup after seeding.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Order(2)
public class AnalyticsProcessingService implements CommandLineRunner {
    
    private final CreatorRepository creatorRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final HashtagPerformanceRepository hashtagPerformanceRepository;
    private final TopCommenterRepository topCommenterRepository;
    private final WeeklyReportRepository weeklyReportRepository;
    private final AnalyticsService analyticsService;
    private final SnapshotAnalyticsService snapshotAnalyticsService;
    private final AccountHealthService accountHealthService;
    private final ObjectMapper objectMapper;
    
    @Override
    @Transactional
    public void run(String... args) {
        // Analytics processing is now triggered by DataSeedService after seeding completes.
        // This CommandLineRunner is kept for backward compat but does nothing on its own.
        // The processCreatorAnalytics() method is called directly by DataSeedService and NightlySyncJob.
        log.info("AnalyticsProcessingService ready (triggered by DataSeedService/NightlySyncJob).");
    }
    
    /**
     * Process analytics for a single creator.
     * Called after initial Phyllo sync and from the nightly job.
     * Generates hashtag_performance, top_commenters, and weekly_report.
     * Enforces max 10 weekly reports per creator (deletes oldest beyond 10).
     */
    public void processCreatorAnalytics(Creator creator) {
        log.info("Processing analytics for creator: {} (@{})", creator.getId(), creator.getUsername());
        processHashtagPerformance(creator);
        processTopCommenters(creator);
        processWeeklyReport(creator);
        enforceWeeklyReportLimit(creator);
    }

    /**
     * Keep only the last 10 weekly reports per creator.
     * Deletes the oldest reports beyond the limit.
     */
    private void enforceWeeklyReportLimit(Creator creator) {
        List<WeeklyReport> reports = weeklyReportRepository.findByCreatorIdOrderByWeekStartDateDesc(creator.getId());
        if (reports.size() > 10) {
            List<WeeklyReport> toDelete = reports.subList(10, reports.size());
            weeklyReportRepository.deleteAll(toDelete);
            log.info("  → Trimmed {} old weekly reports for creator {}", toDelete.size(), creator.getUsername());
        }
    }
    
    // ===== Hashtag Performance =====
    private void processHashtagPerformance(Creator creator) {
        List<Post> posts = postRepository.findByCreatorIdOrderByPostedAtDesc(creator.getId());
        
        // Group posts by hashtag
        Map<String, List<Post>> postsByHashtag = new HashMap<>();
        for (Post post : posts) {
            List<String> tags = parseHashtags(post.getHashtags());
            for (String tag : tags) {
                postsByHashtag.computeIfAbsent(tag, k -> new ArrayList<>()).add(post);
            }
        }
        
        int stored = 0;
        List<HashtagPerformance> batch = new ArrayList<>();
        
        for (Map.Entry<String, List<Post>> entry : postsByHashtag.entrySet()) {
            String hashtag = entry.getKey();
            List<Post> tagPosts = entry.getValue();
            
            OptionalDouble avgReach = tagPosts.stream()
                .map(Post::getMetrics)
                .filter(m -> m.getReach() != null)
                .mapToDouble(PostMetrics::getReach)
                .average();
            
            OptionalDouble avgEngagement = tagPosts.stream()
                .map(Post::getMetrics)
                .filter(m -> m.getReach() != null && m.getReach() > 0)
                .mapToDouble(m -> computeEngagementRate(m))
                .average();
            
            OptionalDouble avgSaveRate = tagPosts.stream()
                .map(Post::getMetrics)
                .filter(m -> m.getSaves() != null && m.getReach() != null && m.getReach() > 0)
                .mapToDouble(m -> m.getSaves() * 100.0 / m.getReach())
                .average();
            
            LocalDateTime lastUsed = tagPosts.stream()
                .map(Post::getPostedAt)
                .max(Comparator.naturalOrder())
                .orElse(null);
            
            double perfScore = (avgReach.isPresent() ? avgReach.getAsDouble() * 0.4 : 0)
                + (avgEngagement.isPresent() ? avgEngagement.getAsDouble() * 0.6 : 0);
            
            HashtagPerformance hp = HashtagPerformance.builder()
                .creator(creator)
                .hashtag(hashtag)
                .usageCount(tagPosts.size())
                .avgReachWhenUsed(avgReach.isPresent() ? round(avgReach.getAsDouble()) : null)
                .avgEngagementWhenUsed(avgEngagement.isPresent() ? round(avgEngagement.getAsDouble()) : null)
                .avgSaveRateWhenUsed(avgSaveRate.isPresent() ? round(avgSaveRate.getAsDouble()) : null)
                .lastUsedAt(lastUsed)
                .performanceScore(round(perfScore))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
            
            batch.add(hp);
            stored++;
        }
        
        if (!batch.isEmpty()) {
            hashtagPerformanceRepository.saveAll(batch);
        }
        log.info("  → Stored {} hashtag performance records for {}", stored, creator.getUsername());
    }
    
    // ===== Top Commenters =====
    private void processTopCommenters(Creator creator) {
        List<Comment> comments = commentRepository.findByCreatorIdOrderByCommentedAtDesc(creator.getId());
        
        // Group by username
        Map<String, List<Comment>> byUser = comments.stream()
            .collect(Collectors.groupingBy(Comment::getUsername));
        
        int stored = 0;
        List<TopCommenter> batch = new ArrayList<>();
        
        for (Map.Entry<String, List<Comment>> entry : byUser.entrySet()) {
            String username = entry.getKey();
            List<Comment> userComments = entry.getValue();
            
            int commentCount = userComments.size();
            long totalLikes = userComments.stream()
                .mapToLong(c -> c.getLikeCount() != null ? c.getLikeCount() : 0)
                .sum();
            
            LocalDateTime firstCommented = userComments.stream()
                .map(Comment::getCommentedAt)
                .min(Comparator.naturalOrder())
                .orElse(null);
            
            LocalDateTime lastCommented = userComments.stream()
                .map(Comment::getCommentedAt)
                .max(Comparator.naturalOrder())
                .orElse(null);
            
            double superfanScore = (commentCount * 2.0) + (totalLikes * 0.5);
            
            TopCommenter tc = TopCommenter.builder()
                .creator(creator)
                .username(username)
                .commentCount(commentCount)
                .totalLikesReceived(totalLikes)
                .firstCommentedAt(firstCommented)
                .lastCommentedAt(lastCommented)
                .superfanScore(superfanScore)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
            
            batch.add(tc);
            stored++;
        }
        
        if (!batch.isEmpty()) {
            topCommenterRepository.saveAll(batch);
        }
        log.info("  → Stored {} top commenter records for {}", stored, creator.getUsername());
    }
    
    // ===== Weekly Report =====
    private void processWeeklyReport(Creator creator) {
        Long creatorId = creator.getId();
        LocalDate weekStart = LocalDate.now().with(DayOfWeek.MONDAY);
        LocalDate weekEnd = weekStart.plusDays(6);
        
        // Skip if already generated for this week
        if (weeklyReportRepository.findByCreatorIdAndWeekStartDate(creatorId, weekStart).isPresent()) {
            log.info("  → Weekly report already exists for {} week {}", creator.getUsername(), weekStart);
            return;
        }
        
        // Compute all metrics
        AccountHealthScoreDTO healthScore = accountHealthService.calculateAccountHealthScore(creatorId);
        
        List<TimeSeriesMetricDTO> timeSeriesMetrics = new ArrayList<>();
        timeSeriesMetrics.add(analyticsService.calculateSaveRate(creatorId, null));
        timeSeriesMetrics.add(analyticsService.calculateShareRate(creatorId, null));
        timeSeriesMetrics.add(analyticsService.calculateCommentRate(creatorId, null));
        timeSeriesMetrics.add(analyticsService.calculateLikeToCommentRatio(creatorId, null));
        timeSeriesMetrics.add(analyticsService.calculatePlayThroughRate(creatorId, null));
        timeSeriesMetrics.add(analyticsService.calculateQuestionsVsStatements(creatorId));
        timeSeriesMetrics.add(analyticsService.calculateCtaDetection(creatorId, null));
        timeSeriesMetrics.add(analyticsService.calculatePostingFrequency(creatorId));
        timeSeriesMetrics.add(analyticsService.calculateEngagementTrend(creatorId, null));
        timeSeriesMetrics.add(analyticsService.calculateReachEfficiency(creatorId, null));
        timeSeriesMetrics.add(analyticsService.calculateOverallEngagement(creatorId, null));
        timeSeriesMetrics.add(analyticsService.calculateProfileToFollowConversion(creatorId));
        timeSeriesMetrics.add(analyticsService.calculateSentimentBreakdown(creatorId));
        timeSeriesMetrics.add(analyticsService.calculateQuestionsDetected(creatorId));
        timeSeriesMetrics.add(analyticsService.calculateContentMix(creatorId));
        
        List<PostPerformanceDTO> bestPosts = snapshotAnalyticsService.getBestPerformingPosts(creatorId, 5);
        List<PostPerformanceDTO> worstPosts = snapshotAnalyticsService.getWorstPerformingPosts(creatorId, 5);
        List<HashtagPerformanceDTO> topHashtags = snapshotAnalyticsService.getTopPerformingHashtags(creatorId, 10);
        DayHourRecommendationDTO bestTime = snapshotAnalyticsService.getBestDayHourToPost(creatorId);
        List<CommentInsightDTO> commonWords = snapshotAnalyticsService.getCommonCommentWords(creatorId, 30);
        List<TopCommenterDTO> topCommenters = snapshotAnalyticsService.getTopCommenters(creatorId, 5);
        SentimentBreakdownDTO sentiment = analyticsService.getSentimentBreakdown(creatorId);
        
        // Build dashboard DTO
        AnalyticsDashboardDTO dashboard = new AnalyticsDashboardDTO(
            creatorId,
            creator.getUsername(),
            healthScore,
            timeSeriesMetrics,
            bestPosts,
            worstPosts,
            topHashtags,
            bestTime,
            commonWords,
            topCommenters,
            sentiment,
            LocalDateTime.now()
        );
        
        // Serialize to JSON
        String reportJson;
        try {
            reportJson = objectMapper.writeValueAsString(dashboard);
        } catch (Exception e) {
            log.error("Failed to serialize dashboard for {}", creator.getUsername(), e);
            reportJson = "{}";
        }
        
        // Get top/worst post entities for FK references
        Post topPost = bestPosts.isEmpty() ? null
            : postRepository.findById(bestPosts.get(0).getPostId()).orElse(null);
        Post worstPost = worstPosts.isEmpty() ? null
            : postRepository.findById(worstPosts.get(0).getPostId()).orElse(null);
        
        // Count posts this week
        List<Post> allPosts = postRepository.findByCreatorIdOrderByPostedAtDesc(creatorId);
        int postsPublished = (int) allPosts.stream()
            .filter(p -> !p.getPostedAt().toLocalDate().isBefore(weekStart)
                && !p.getPostedAt().toLocalDate().isAfter(weekEnd))
            .count();
        
        // Unanswered questions
        long unanswered = commentRepository.findByCreatorIdOrderByCommentedAtDesc(creatorId).stream()
            .filter(c -> Boolean.TRUE.equals(c.getIsQuestion()) && c.getReplyCount() == 0)
            .count();
        
        // Extract metric values for denormalized columns
        Double avgEngRate = findMetricValue(timeSeriesMetrics, "overall_engagement_rate");
        Double avgSaveRate = findMetricValue(timeSeriesMetrics, "save_rate");
        Double avgShareRate = findMetricValue(timeSeriesMetrics, "share_rate");
        Double avgReachEff = findMetricValue(timeSeriesMetrics, "reach_efficiency");
        
        WeeklyReport report = WeeklyReport.builder()
            .creator(creator)
            .weekStartDate(weekStart)
            .weekEndDate(weekEnd)
            .generatedAt(LocalDateTime.now())
            .reportJson(reportJson)
            .healthScore(healthScore.getScore() != null ? healthScore.getScore().doubleValue() : null)
            .healthScoreDelta(null) // first week, no previous to compare
            .avgEngagementRate(avgEngRate)
            .avgSaveRate(avgSaveRate)
            .avgShareRate(avgShareRate)
            .avgReachEfficiency(avgReachEff)
            .postsPublished(postsPublished)
            .topPost(topPost)
            .worstPost(worstPost)
            .sentimentPositivePct(sentiment.getPositivePercentage())
            .sentimentNeutralPct(sentiment.getNeutralPercentage())
            .sentimentNegativePct(sentiment.getNegativePercentage())
            .unansweredQuestionsCount((int) unanswered)
            .build();
        
        weeklyReportRepository.save(report);
        log.info("  → Generated weekly report for {} (health score: {})", creator.getUsername(), healthScore.getScore());
    }
    
    // ===== Helpers =====
    
    private double computeEngagementRate(PostMetrics m) {
        if (m.getReach() == null || m.getReach() == 0) return 0.0;
        int likes = m.getLikes() != null ? m.getLikes() : 0;
        int comments = m.getComments() != null ? m.getComments() : 0;
        int saves = m.getSaves() != null ? m.getSaves() : 0;
        int shares = m.getShares() != null ? m.getShares() : 0;
        return (likes + comments + saves + shares) * 100.0 / m.getReach();
    }
    
    private List<String> parseHashtags(String hashtags) {
        if (hashtags == null || hashtags.isBlank()) return Collections.emptyList();
        return Arrays.stream(hashtags.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toList());
    }
    
    private Double findMetricValue(List<TimeSeriesMetricDTO> metrics, String name) {
        return metrics.stream()
            .filter(m -> name.equals(m.getMetricName()))
            .findFirst()
            .map(TimeSeriesMetricDTO::getCurrentValue)
            .orElse(null);
    }
    
    private Double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
