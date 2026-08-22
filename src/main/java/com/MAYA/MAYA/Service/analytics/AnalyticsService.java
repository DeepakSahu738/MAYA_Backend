package com.MAYA.MAYA.Service.analytics;

import com.MAYA.MAYA.DTO.analytics.SentimentBreakdownDTO;
import com.MAYA.MAYA.DTO.analytics.TimeSeriesMetricDTO;
import com.MAYA.MAYA.Entity.instagram.Comment;
import com.MAYA.MAYA.Entity.instagram.Creator;
import com.MAYA.MAYA.Entity.instagram.Post;
import com.MAYA.MAYA.Entity.instagram.PostMetrics;
import com.MAYA.MAYA.Repository.instagram.CommentRepository;
import com.MAYA.MAYA.Repository.instagram.CreatorRepository;
import com.MAYA.MAYA.Repository.instagram.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Pure Java math only. NEVER calls LLM. NEVER calls Graph API or Phyllo directly.
 * All data comes from repositories.
 */
@Service
@RequiredArgsConstructor
public class AnalyticsService {
    
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final CreatorRepository creatorRepository;
    
    // ===== Metric #1: Save Rate =====
    public TimeSeriesMetricDTO calculateSaveRate(Long creatorId, Integer postCount) {
        List<Post> posts = getRecentPosts(creatorId, postCount);
        
        OptionalDouble avgRate = posts.stream()
            .map(Post::getMetrics)
            .filter(m -> m.getSaves() != null && m.getReach() != null && m.getReach() > 0)
            .mapToDouble(m -> m.getSaves() * 100.0 / m.getReach())
            .average();
        
        Double currentValue = avgRate.isPresent() ? round(avgRate.getAsDouble()) : null;
        Double delta = computeWeeklyDelta(creatorId, this::postSaveRate);
        
        return buildMetric("save_rate", currentValue, delta, "%");
    }
    
    // ===== Metric #2: Share Rate =====
    public TimeSeriesMetricDTO calculateShareRate(Long creatorId, Integer postCount) {
        List<Post> posts = getRecentPosts(creatorId, postCount);
        
        OptionalDouble avgRate = posts.stream()
            .map(Post::getMetrics)
            .filter(m -> m.getShares() != null && m.getReach() != null && m.getReach() > 0)
            .mapToDouble(m -> m.getShares() * 100.0 / m.getReach())
            .average();
        
        Double currentValue = avgRate.isPresent() ? round(avgRate.getAsDouble()) : null;
        Double delta = computeWeeklyDelta(creatorId, this::postShareRate);
        
        return buildMetric("share_rate", currentValue, delta, "%");
    }
    
    // ===== Metric #3: Comment Rate =====
    public TimeSeriesMetricDTO calculateCommentRate(Long creatorId, Integer postCount) {
        List<Post> posts = getRecentPosts(creatorId, postCount);
        
        OptionalDouble avgRate = posts.stream()
            .map(Post::getMetrics)
            .filter(m -> m.getComments() != null && m.getReach() != null && m.getReach() > 0)
            .mapToDouble(m -> m.getComments() * 100.0 / m.getReach())
            .average();
        
        Double currentValue = avgRate.isPresent() ? round(avgRate.getAsDouble()) : null;
        Double delta = computeWeeklyDelta(creatorId, this::postCommentRate);
        
        return buildMetric("comment_rate", currentValue, delta, "%");
    }
    
    // ===== Metric #4: Like-to-Comment Ratio =====
    public TimeSeriesMetricDTO calculateLikeToCommentRatio(Long creatorId, Integer postCount) {
        List<Post> posts = getRecentPosts(creatorId, postCount);
        
        OptionalDouble avgRatio = posts.stream()
            .map(Post::getMetrics)
            .filter(m -> m.getComments() != null && m.getComments() > 0)
            .mapToDouble(m -> m.getLikes() * 1.0 / m.getComments())
            .average();
        
        Double currentValue = avgRatio.isPresent() ? round(avgRatio.getAsDouble()) : null;
        Double delta = computeWeeklyDelta(creatorId, this::postLikeToCommentRatio);
        
        return buildMetric("like_to_comment_ratio", currentValue, delta, "ratio");
    }
    
    // ===== Metric #5: Play-through Rate (VIDEO only) =====
    public TimeSeriesMetricDTO calculatePlayThroughRate(Long creatorId, Integer postCount) {
        List<Post> posts = getRecentPosts(creatorId, postCount);
        
        OptionalDouble avgRate = posts.stream()
            .filter(p -> "VIDEO".equalsIgnoreCase(p.getMediaType()))
            .map(Post::getMetrics)
            .filter(m -> m.getPlays() != null && m.getImpressions() != null && m.getImpressions() > 0)
            .mapToDouble(m -> m.getPlays() * 100.0 / m.getImpressions())
            .average();
        
        Double currentValue = avgRate.isPresent() ? round(avgRate.getAsDouble()) : null;
        Double delta = computeWeeklyDeltaVideoOnly(creatorId);
        
        return buildMetric("play_through_rate", currentValue, delta, "%");
    }
    
    // ===== Metric #6: Questions vs Statements =====
    public TimeSeriesMetricDTO calculateQuestionsVsStatements(Long creatorId) {
        List<Post> posts = postRepository.findByCreatorIdOrderByPostedAtDesc(creatorId);
        List<Comment> allComments = commentRepository.findByCreatorIdOrderByCommentedAtDesc(creatorId);
        
        // Group comments by post
        Map<Long, List<Comment>> commentsByPost = allComments.stream()
            .collect(Collectors.groupingBy(c -> c.getPost().getId()));
        
        double questionHeavyLikesSum = 0;
        int questionHeavyCount = 0;
        double statementHeavyLikesSum = 0;
        int statementHeavyCount = 0;
        
        for (Post post : posts) {
            List<Comment> postComments = commentsByPost.getOrDefault(post.getId(), Collections.emptyList());
            if (postComments.isEmpty()) continue;
            
            long questions = postComments.stream().filter(c -> Boolean.TRUE.equals(c.getIsQuestion())).count();
            double questionPct = questions * 100.0 / postComments.size();
            
            if (questionPct >= 50) {
                questionHeavyLikesSum += post.getMetrics().getLikes();
                questionHeavyCount++;
            } else {
                statementHeavyLikesSum += post.getMetrics().getLikes();
                statementHeavyCount++;
            }
        }
        
        Double questionAvg = questionHeavyCount > 0 ? questionHeavyLikesSum / questionHeavyCount : null;
        Double statementAvg = statementHeavyCount > 0 ? statementHeavyLikesSum / statementHeavyCount : null;
        Double delta = (questionAvg != null && statementAvg != null) ? round(questionAvg - statementAvg) : null;
        
        return buildMetric("questions_vs_statements", questionAvg != null ? round(questionAvg) : null, delta, "avg_likes");
    }
    
    // ===== Metric #7: CTA Detection vs Engagement =====
    public TimeSeriesMetricDTO calculateCtaDetection(Long creatorId, Integer postCount) {
        List<Post> posts = getRecentPosts(creatorId, postCount);
        
        OptionalDouble ctaAvg = posts.stream()
            .filter(p -> Boolean.TRUE.equals(p.getHasCta()))
            .mapToDouble(p -> p.getMetrics().getLikes() + p.getMetrics().getComments())
            .average();
        
        OptionalDouble noCtaAvg = posts.stream()
            .filter(p -> !Boolean.TRUE.equals(p.getHasCta()))
            .mapToDouble(p -> p.getMetrics().getLikes() + p.getMetrics().getComments())
            .average();
        
        Double ctaValue = ctaAvg.isPresent() ? round(ctaAvg.getAsDouble()) : null;
        Double lift = (ctaAvg.isPresent() && noCtaAvg.isPresent())
            ? round(ctaAvg.getAsDouble() - noCtaAvg.getAsDouble()) : null;
        
        return buildMetric("cta_engagement_lift", ctaValue, lift, "engagement");
    }
    
    // ===== Metric #8: Posting Frequency Per Week =====
    public TimeSeriesMetricDTO calculatePostingFrequency(Long creatorId) {
        List<Post> posts = postRepository.findByCreatorIdOrderByPostedAtDesc(creatorId);
        
        LocalDate now = LocalDate.now();
        LocalDate weekStart = now.with(DayOfWeek.MONDAY);
        LocalDate prevWeekStart = weekStart.minusWeeks(1);
        LocalDate eightWeeksAgo = weekStart.minusWeeks(8);
        
        long postsThisWeek = posts.stream()
            .filter(p -> !p.getPostedAt().toLocalDate().isBefore(weekStart))
            .count();
        
        long postsLastWeek = posts.stream()
            .filter(p -> !p.getPostedAt().toLocalDate().isBefore(prevWeekStart)
                && p.getPostedAt().toLocalDate().isBefore(weekStart))
            .count();
        
        long postsLast8Weeks = posts.stream()
            .filter(p -> !p.getPostedAt().toLocalDate().isBefore(eightWeeksAgo))
            .count();
        
        Double avgPerWeek = round(postsLast8Weeks / 8.0);
        Double delta = (double) (postsThisWeek - postsLastWeek);
        
        return buildMetric("posting_frequency", avgPerWeek, delta, "posts/week");
    }
    
    // ===== Metric #9: Engagement Trend Over Time =====
    public TimeSeriesMetricDTO calculateEngagementTrend(Long creatorId, Integer postCount) {
        List<Post> posts = postRepository.findByCreatorIdOrderByPostedAtDesc(creatorId);
        
        LocalDate now = LocalDate.now();
        LocalDate weekStart = now.with(DayOfWeek.MONDAY);
        
        // Last 8 weeks of engagement totals
        double[] weeklyTotals = new double[8];
        for (Post post : posts) {
            LocalDate postDate = post.getPostedAt().toLocalDate();
            for (int w = 0; w < 8; w++) {
                LocalDate wStart = weekStart.minusWeeks(w);
                LocalDate wEnd = wStart.plusDays(7);
                if (!postDate.isBefore(wStart) && postDate.isBefore(wEnd)) {
                    PostMetrics m = post.getMetrics();
                    weeklyTotals[w] += m.getLikes()
                        + m.getComments()
                        + (m.getSaves() != null ? m.getSaves() : 0)
                        + (m.getShares() != null ? m.getShares() : 0);
                    break;
                }
            }
        }
        
        // recent 4 weeks avg vs prior 4 weeks avg
        double recentAvg = (weeklyTotals[0] + weeklyTotals[1] + weeklyTotals[2] + weeklyTotals[3]) / 4.0;
        double priorAvg = (weeklyTotals[4] + weeklyTotals[5] + weeklyTotals[6] + weeklyTotals[7]) / 4.0;
        double slope = recentAvg - priorAvg;
        
        return buildMetric("engagement_trend", round(recentAvg), round(slope), "total_engagement");
    }
    
    // ===== Metric #10: Reach Efficiency =====
    public TimeSeriesMetricDTO calculateReachEfficiency(Long creatorId, Integer postCount) {
        List<Post> posts = getRecentPosts(creatorId, postCount);
        
        OptionalDouble avgEfficiency = posts.stream()
            .map(Post::getMetrics)
            .filter(m -> m.getReach() != null && m.getImpressions() != null && m.getImpressions() > 0)
            .mapToDouble(m -> m.getReach() * 100.0 / m.getImpressions())
            .average();
        
        Double currentValue = avgEfficiency.isPresent() ? round(avgEfficiency.getAsDouble()) : null;
        Double delta = computeWeeklyDelta(creatorId, this::postReachEfficiency);
        
        return buildMetric("reach_efficiency", currentValue, delta, "%");
    }
    
    // ===== Metric #11: Overall Account Engagement Rate =====
    public TimeSeriesMetricDTO calculateOverallEngagement(Long creatorId, Integer postCount) {
        List<Post> posts = getRecentPosts(creatorId, postCount);

        // Try reach-based ER first (only using posts with reliable reach)
        OptionalDouble avgEr = posts.stream()
            .map(Post::getMetrics)
            .filter(m -> m != null && m.getReach() != null && m.getReach() > 0)
            .filter(m -> {
                // Validate: reach must be >= total engagement, otherwise data is garbage
                int likes = m.getLikes() != null ? m.getLikes() : 0;
                int comments = m.getComments() != null ? m.getComments() : 0;
                int saves = m.getSaves() != null ? m.getSaves() : 0;
                int shares = m.getShares() != null ? m.getShares() : 0;
                return m.getReach() >= (likes + comments + saves + shares);
            })
            .mapToDouble(m -> {
                int likes = m.getLikes() != null ? m.getLikes() : 0;
                int comments = m.getComments() != null ? m.getComments() : 0;
                int saves = m.getSaves() != null ? m.getSaves() : 0;
                int shares = m.getShares() != null ? m.getShares() : 0;
                return (likes + comments + saves + shares) * 100.0 / m.getReach();
            })
            .average();

        Double currentValue;
        if (avgEr.isPresent()) {
            currentValue = round(Math.min(avgEr.getAsDouble(), 100.0));
        } else {
            // Fallback: use follower count as denominator (industry-standard approach)
            Creator creator = creatorRepository.findById(creatorId).orElse(null);
            if (creator != null && creator.getFollowerCount() != null && creator.getFollowerCount() > 0) {
                OptionalDouble followerEr = posts.stream()
                    .map(Post::getMetrics)
                    .filter(java.util.Objects::nonNull)
                    .mapToDouble(m -> {
                        int likes = m.getLikes() != null ? m.getLikes() : 0;
                        int comments = m.getComments() != null ? m.getComments() : 0;
                        int saves = m.getSaves() != null ? m.getSaves() : 0;
                        int shares = m.getShares() != null ? m.getShares() : 0;
                        return (likes + comments + saves + shares) * 100.0 / creator.getFollowerCount();
                    })
                    .average();
                currentValue = followerEr.isPresent() ? round(followerEr.getAsDouble()) : null;
            } else {
                currentValue = null; // Neither reach nor followers available
            }
        }

        Double delta = computeWeeklyDelta(creatorId, this::postEngagementRate);
        return buildMetric("overall_engagement_rate", currentValue, delta, "%");
    }
    
    // ===== Metric #12: Profile View → Follow Conversion =====
    // Note: Only 30-32% of posts have profile_visits and followers_gained.
    // For now, returns null since Phyllo dummy data does not include additional_info.
    public TimeSeriesMetricDTO calculateProfileToFollowConversion(Long creatorId) {
        return buildMetric("profile_to_follow_conversion", null, null, "%");
    }
    
    // ===== Metric #13: Sentiment Breakdown =====
    public TimeSeriesMetricDTO calculateSentimentBreakdown(Long creatorId) {
        List<Comment> comments = commentRepository.findByCreatorIdOrderByCommentedAtDesc(creatorId);
        if (comments.isEmpty()) {
            return buildMetric("sentiment_breakdown", null, null, "pct");
        }
        
        // Rule-based fallback (LLM classification handled by CommentAnalysisService)
        long positive = 0, negative = 0, neutral = 0;
        for (Comment c : comments) {
            String sentiment = c.getSentiment();
            if (sentiment != null) {
                switch (sentiment.toUpperCase()) {
                    case "POSITIVE" -> positive++;
                    case "NEGATIVE" -> negative++;
                    default -> neutral++;
                }
            } else {
                // Rule-based fallback
                String text = c.getText().toLowerCase();
                if (containsAny(text, "love", "great", "amazing", "awesome", "beautiful", "wonderful", "fantastic", "stunning", "incredible", "best", "helpful")) {
                    positive++;
                } else if (containsAny(text, "hate", "terrible", "awful", "bad", "worst", "boring", "disappointed", "useless")) {
                    negative++;
                } else {
                    neutral++;
                }
            }
        }
        
        double total = positive + negative + neutral;
        Double positivePct = round(positive * 100.0 / total);
        
        return buildMetric("sentiment_positive_pct", positivePct, null, "%");
    }
    
    /**
     * Full sentiment breakdown as a DTO (used by AccountHealthService and dashboard).
     */
    public SentimentBreakdownDTO getSentimentBreakdown(Long creatorId) {
        List<Comment> comments = commentRepository.findByCreatorIdOrderByCommentedAtDesc(creatorId);
        if (comments.isEmpty()) {
            return new SentimentBreakdownDTO(0, 0, 0, 0.0, 0.0, 0.0);
        }
        
        int positive = 0, negative = 0, neutral = 0;
        for (Comment c : comments) {
            String sentiment = c.getSentiment();
            if (sentiment != null) {
                switch (sentiment.toUpperCase()) {
                    case "POSITIVE" -> positive++;
                    case "NEGATIVE" -> negative++;
                    default -> neutral++;
                }
            } else {
                String text = c.getText().toLowerCase();
                if (containsAny(text, "love", "great", "amazing", "awesome", "beautiful", "wonderful", "fantastic", "stunning", "incredible", "best", "helpful")) {
                    positive++;
                } else if (containsAny(text, "hate", "terrible", "awful", "bad", "worst", "boring", "disappointed", "useless")) {
                    negative++;
                } else {
                    neutral++;
                }
            }
        }
        
        double total = positive + negative + neutral;
        return new SentimentBreakdownDTO(
            positive, neutral, negative,
            round(positive * 100.0 / total),
            round(neutral * 100.0 / total),
            round(negative * 100.0 / total)
        );
    }
    
    // ===== Metric #14: Questions Detected in Comments =====
    // Returns count of questions this week + delta
    public TimeSeriesMetricDTO calculateQuestionsDetected(Long creatorId) {
        List<Comment> comments = commentRepository.findByCreatorIdOrderByCommentedAtDesc(creatorId);
        
        LocalDate now = LocalDate.now();
        LocalDate weekStart = now.with(DayOfWeek.MONDAY);
        LocalDate prevWeekStart = weekStart.minusWeeks(1);
        
        long questionsThisWeek = comments.stream()
            .filter(c -> Boolean.TRUE.equals(c.getIsQuestion()))
            .filter(c -> !c.getCommentedAt().toLocalDate().isBefore(weekStart))
            .count();
        
        long questionsLastWeek = comments.stream()
            .filter(c -> Boolean.TRUE.equals(c.getIsQuestion()))
            .filter(c -> !c.getCommentedAt().toLocalDate().isBefore(prevWeekStart)
                && c.getCommentedAt().toLocalDate().isBefore(weekStart))
            .count();
        
        long unanswered = comments.stream()
            .filter(c -> Boolean.TRUE.equals(c.getIsQuestion()) && c.getReplyCount() == 0)
            .count();
        
        Double delta = (double) (questionsThisWeek - questionsLastWeek);
        
        return buildMetric("questions_detected", (double) unanswered, delta, "count");
    }
    
    // ===== Metric #15: Content Mix Breakdown =====
    public TimeSeriesMetricDTO calculateContentMix(Long creatorId) {
        List<Post> posts = postRepository.findByCreatorIdOrderByPostedAtDesc(creatorId);
        if (posts.isEmpty()) {
            return buildMetric("content_mix", null, null, "pct");
        }
        
        long imageCount = posts.stream().filter(p -> "IMAGE".equalsIgnoreCase(p.getMediaType())).count();
        long videoCount = posts.stream().filter(p -> "VIDEO".equalsIgnoreCase(p.getMediaType())).count();
        long total = imageCount + videoCount;
        
        Double videoPct = total > 0 ? round(videoCount * 100.0 / total) : null;
        
        // Compare avg engagement rate by format
        OptionalDouble imageAvgEr = posts.stream()
            .filter(p -> "IMAGE".equalsIgnoreCase(p.getMediaType()))
            .map(Post::getMetrics)
            .filter(m -> m.getEngagementRate() != null)
            .mapToDouble(PostMetrics::getEngagementRate)
            .average();
        
        OptionalDouble videoAvgEr = posts.stream()
            .filter(p -> "VIDEO".equalsIgnoreCase(p.getMediaType()))
            .map(Post::getMetrics)
            .filter(m -> m.getEngagementRate() != null)
            .mapToDouble(PostMetrics::getEngagementRate)
            .average();
        
        Double delta = (imageAvgEr.isPresent() && videoAvgEr.isPresent())
            ? round(videoAvgEr.getAsDouble() - imageAvgEr.getAsDouble()) : null;
        
        return buildMetric("content_mix_video_pct", videoPct, delta, "%");
    }
    
    // ======================================================================
    // HELPER METHODS
    // ======================================================================
    
    private List<Post> getRecentPosts(Long creatorId, Integer postCount) {
        List<Post> all = postRepository.findByCreatorIdOrderByPostedAtDesc(creatorId);
        if (postCount != null && postCount < all.size()) {
            return all.subList(0, postCount);
        }
        return all;
    }
    
    // --- Per-post rate extractors for delta computation ---
    
    private Double postSaveRate(Post post) {
        PostMetrics m = post.getMetrics();
        if (m.getSaves() != null && m.getReach() != null && m.getReach() > 0) {
            return m.getSaves() * 100.0 / m.getReach();
        }
        return null;
    }
    
    private Double postShareRate(Post post) {
        PostMetrics m = post.getMetrics();
        if (m.getShares() != null && m.getReach() != null && m.getReach() > 0) {
            return m.getShares() * 100.0 / m.getReach();
        }
        return null;
    }
    
    private Double postCommentRate(Post post) {
        PostMetrics m = post.getMetrics();
        if (m.getComments() != null && m.getReach() != null && m.getReach() > 0) {
            return m.getComments() * 100.0 / m.getReach();
        }
        return null;
    }
    
    private Double postLikeToCommentRatio(Post post) {
        PostMetrics m = post.getMetrics();
        if (m.getComments() != null && m.getComments() > 0) {
            return m.getLikes() * 1.0 / m.getComments();
        }
        return null;
    }
    
    private Double postReachEfficiency(Post post) {
        PostMetrics m = post.getMetrics();
        if (m.getReach() != null && m.getImpressions() != null && m.getImpressions() > 0) {
            return m.getReach() * 100.0 / m.getImpressions();
        }
        return null;
    }
    
    private Double postEngagementRate(Post post) {
        PostMetrics m = post.getMetrics();
        if (m.getReach() != null && m.getReach() > 0) {
            int likes = m.getLikes() != null ? m.getLikes() : 0;
            int comments = m.getComments() != null ? m.getComments() : 0;
            int saves = m.getSaves() != null ? m.getSaves() : 0;
            int shares = m.getShares() != null ? m.getShares() : 0;
            return (likes + comments + saves + shares) * 100.0 / m.getReach();
        }
        return null;
    }
    
    // --- Weekly delta computation (generic pattern from spec) ---
    
    @FunctionalInterface
    private interface PostRateExtractor {
        Double extract(Post post);
    }
    
    private Double computeWeeklyDelta(Long creatorId, PostRateExtractor extractor) {
        LocalDate now = LocalDate.now();
        LocalDate weekStart = now.with(DayOfWeek.MONDAY);
        LocalDate prevWeekStart = weekStart.minusWeeks(1);
        
        List<Post> posts = postRepository.findByCreatorIdOrderByPostedAtDesc(creatorId);
        
        OptionalDouble thisWeekAvg = posts.stream()
            .filter(p -> !p.getPostedAt().toLocalDate().isBefore(weekStart))
            .map(extractor::extract)
            .filter(Objects::nonNull)
            .mapToDouble(Double::doubleValue)
            .average();
        
        OptionalDouble lastWeekAvg = posts.stream()
            .filter(p -> !p.getPostedAt().toLocalDate().isBefore(prevWeekStart)
                && p.getPostedAt().toLocalDate().isBefore(weekStart))
            .map(extractor::extract)
            .filter(Objects::nonNull)
            .mapToDouble(Double::doubleValue)
            .average();
        
        if (thisWeekAvg.isPresent() && lastWeekAvg.isPresent()) {
            return round(thisWeekAvg.getAsDouble() - lastWeekAvg.getAsDouble());
        }
        return null;
    }
    
    private Double computeWeeklyDeltaVideoOnly(Long creatorId) {
        LocalDate now = LocalDate.now();
        LocalDate weekStart = now.with(DayOfWeek.MONDAY);
        LocalDate prevWeekStart = weekStart.minusWeeks(1);
        
        List<Post> posts = postRepository.findByCreatorIdOrderByPostedAtDesc(creatorId);
        
        OptionalDouble thisWeekAvg = posts.stream()
            .filter(p -> "VIDEO".equalsIgnoreCase(p.getMediaType()))
            .filter(p -> !p.getPostedAt().toLocalDate().isBefore(weekStart))
            .map(Post::getMetrics)
            .filter(m -> m.getPlays() != null && m.getImpressions() != null && m.getImpressions() > 0)
            .mapToDouble(m -> m.getPlays() * 100.0 / m.getImpressions())
            .average();
        
        OptionalDouble lastWeekAvg = posts.stream()
            .filter(p -> "VIDEO".equalsIgnoreCase(p.getMediaType()))
            .filter(p -> !p.getPostedAt().toLocalDate().isBefore(prevWeekStart)
                && p.getPostedAt().toLocalDate().isBefore(weekStart))
            .map(Post::getMetrics)
            .filter(m -> m.getPlays() != null && m.getImpressions() != null && m.getImpressions() > 0)
            .mapToDouble(m -> m.getPlays() * 100.0 / m.getImpressions())
            .average();
        
        if (thisWeekAvg.isPresent() && lastWeekAvg.isPresent()) {
            return round(thisWeekAvg.getAsDouble() - lastWeekAvg.getAsDouble());
        }
        return null;
    }
    
    // --- Utility ---
    
    private boolean containsAny(String text, String... keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) return true;
        }
        return false;
    }
    
    private TimeSeriesMetricDTO buildMetric(String name, Double value, Double delta, String unit) {
        return new TimeSeriesMetricDTO(name, value, delta, unit, LocalDateTime.now());
    }
    
    private Double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
