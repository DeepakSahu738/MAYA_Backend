package com.MAYA.MAYA.Service.analytics;

import com.MAYA.MAYA.DTO.analytics.*;
import com.MAYA.MAYA.DTO.analytics.DashboardResponseDTO.*;
import com.MAYA.MAYA.Entity.instagram.*;
import com.MAYA.MAYA.Controller.exception.CreatorNotFoundException;
import com.MAYA.MAYA.Repository.instagram.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Assembles the full dashboard response for a given creator.
 * Computes all 24 metrics on-demand from raw data.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final AnalyticsService analyticsService;
    private final SnapshotAnalyticsService snapshotAnalyticsService;
    private final AccountHealthService accountHealthService;
    private final CreatorRepository creatorRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final TopCommenterRepository topCommenterRepository;

    public DashboardResponseDTO buildDashboard(Long creatorId) {
        Creator creator = creatorRepository.findById(creatorId)
            .orElseThrow(() -> new CreatorNotFoundException(creatorId));

        List<Post> posts = postRepository.findByCreatorIdOrderByPostedAtDesc(creatorId);
        List<Comment> comments = commentRepository.findByCreatorIdOrderByCommentedAtDesc(creatorId);

        return DashboardResponseDTO.builder()
            .creatorId(creatorId)
            .username(creator.getUsername())
            .niche(creator.getNiche())
            .generatedAt(LocalDateTime.now())
            .healthScore(accountHealthService.calculateAccountHealthScore(creatorId))
            .rateCards(buildRateCards(creatorId))
            .engagementTrend(buildEngagementTrend(posts))
            .trendDirection(computeTrendDirection(posts))
            .bestPosts(snapshotAnalyticsService.getBestPerformingPosts(creatorId, 5))
            .worstPosts(snapshotAnalyticsService.getWorstPerformingPosts(creatorId, 5))
            .contentMix(buildContentMix(posts))
            .bestPostingTime(buildBestPostingTime(posts))
            .mostUsedHashtags(snapshotAnalyticsService.getMostUsedHashtags(creatorId, 10))
            .topPerformingHashtags(snapshotAnalyticsService.getTopPerformingHashtags(creatorId, 10))
            .topCommenters(buildTopCommenters(creatorId))
            .sentimentBreakdown(analyticsService.getSentimentBreakdown(creatorId))
            .commonWords(snapshotAnalyticsService.getCommonCommentWords(creatorId, 20))
            .questionsVsStatements(buildQuestionsVsStatements(posts, comments))
            .questionsInsight(buildQuestionsInsight(creatorId, comments))
            .mostLikedComments(buildMostLikedComments(posts, comments))
            .ctaInsight(buildCtaInsight(posts))
            .captionLengthInsight(buildCaptionLengthInsight(posts))
            .profileConversion(analyticsService.calculateProfileToFollowConversion(creatorId))
            .build();
    }

    // --- Rate Cards ---
    private List<TimeSeriesMetricDTO> buildRateCards(Long creatorId) {
        List<TimeSeriesMetricDTO> cards = new ArrayList<>();
        cards.add(analyticsService.calculateOverallEngagement(creatorId, null));
        cards.add(analyticsService.calculateSaveRate(creatorId, null));
        cards.add(analyticsService.calculateShareRate(creatorId, null));
        cards.add(analyticsService.calculateCommentRate(creatorId, null));
        cards.add(analyticsService.calculateLikeToCommentRatio(creatorId, null));
        cards.add(analyticsService.calculatePlayThroughRate(creatorId, null));
        cards.add(analyticsService.calculateReachEfficiency(creatorId, null));
        cards.add(analyticsService.calculatePostingFrequency(creatorId));
        return cards;
    }

    // --- Engagement Trend (weekly data points) ---
    private List<WeeklyEngagementPointDTO> buildEngagementTrend(List<Post> posts) {
        LocalDate now = LocalDate.now();
        LocalDate weekStart = now.with(DayOfWeek.MONDAY);

        List<WeeklyEngagementPointDTO> trend = new ArrayList<>();
        for (int w = 7; w >= 0; w--) {
            LocalDate wStart = weekStart.minusWeeks(w);
            LocalDate wEnd = wStart.plusDays(7);

            long total = posts.stream()
                .filter(p -> !p.getPostedAt().toLocalDate().isBefore(wStart)
                    && p.getPostedAt().toLocalDate().isBefore(wEnd))
                .mapToLong(p -> {
                    PostMetrics m = p.getMetrics();
                    return (m.getLikes() != null ? m.getLikes() : 0)
                        + (m.getComments() != null ? m.getComments() : 0)
                        + (m.getSaves() != null ? m.getSaves() : 0)
                        + (m.getShares() != null ? m.getShares() : 0);
                })
                .sum();

            int weekNum = wStart.get(WeekFields.ISO.weekOfWeekBasedYear());
            trend.add(WeeklyEngagementPointDTO.builder()
                .week(wStart.getYear() + "-W" + String.format("%02d", weekNum))
                .weekStart(wStart.toString())
                .totalEngagement(total)
                .build());
        }
        return trend;
    }

    private String computeTrendDirection(List<Post> posts) {
        LocalDate now = LocalDate.now();
        LocalDate weekStart = now.with(DayOfWeek.MONDAY);

        double[] weeklyTotals = new double[8];
        for (Post post : posts) {
            LocalDate postDate = post.getPostedAt().toLocalDate();
            for (int w = 0; w < 8; w++) {
                LocalDate wStart = weekStart.minusWeeks(w);
                LocalDate wEnd = wStart.plusDays(7);
                if (!postDate.isBefore(wStart) && postDate.isBefore(wEnd)) {
                    PostMetrics m = post.getMetrics();
                    weeklyTotals[w] += (m.getLikes() != null ? m.getLikes() : 0)
                        + (m.getComments() != null ? m.getComments() : 0)
                        + (m.getSaves() != null ? m.getSaves() : 0)
                        + (m.getShares() != null ? m.getShares() : 0);
                    break;
                }
            }
        }

        double recentAvg = (weeklyTotals[0] + weeklyTotals[1] + weeklyTotals[2] + weeklyTotals[3]) / 4.0;
        double priorAvg = (weeklyTotals[4] + weeklyTotals[5] + weeklyTotals[6] + weeklyTotals[7]) / 4.0;

        if (priorAvg == 0) return "FLAT";
        double changePct = Math.abs(recentAvg - priorAvg) / priorAvg * 100;
        if (changePct < 5) return "FLAT";
        return recentAvg > priorAvg ? "GROWING" : "DECLINING";
    }

    // --- Content Mix ---
    private ContentMixDTO buildContentMix(List<Post> posts) {
        long imageCount = posts.stream().filter(p -> "IMAGE".equalsIgnoreCase(p.getMediaType())).count();
        long videoCount = posts.stream().filter(p -> "VIDEO".equalsIgnoreCase(p.getMediaType())).count();
        long total = imageCount + videoCount;
        if (total == 0) return null;

        OptionalDouble imageEr = posts.stream()
            .filter(p -> "IMAGE".equalsIgnoreCase(p.getMediaType()))
            .map(Post::getMetrics)
            .filter(m -> m.getEngagementRate() != null)
            .mapToDouble(PostMetrics::getEngagementRate)
            .average();

        OptionalDouble videoEr = posts.stream()
            .filter(p -> "VIDEO".equalsIgnoreCase(p.getMediaType()))
            .map(Post::getMetrics)
            .filter(m -> m.getEngagementRate() != null)
            .mapToDouble(PostMetrics::getEngagementRate)
            .average();

        String betterFormat = (videoEr.orElse(0) > imageEr.orElse(0)) ? "VIDEO" : "IMAGE";
        double diff = Math.abs(videoEr.orElse(0) - imageEr.orElse(0));

        return ContentMixDTO.builder()
            .imageCount((int) imageCount)
            .videoCount((int) videoCount)
            .imagePct(round(imageCount * 100.0 / total))
            .videoPct(round(videoCount * 100.0 / total))
            .imageAvgEngagementRate(imageEr.isPresent() ? round(imageEr.getAsDouble()) : null)
            .videoAvgEngagementRate(videoEr.isPresent() ? round(videoEr.getAsDouble()) : null)
            .betterFormat(betterFormat)
            .recommendation(String.format("Your %s posts outperform by %.1f%% — consider shifting content mix", betterFormat, diff))
            .build();
    }

    // --- Best Posting Time ---
    private BestPostingTimeDTO buildBestPostingTime(List<Post> posts) {
        Map<DayOfWeek, List<Post>> dayPosts = posts.stream()
            .collect(Collectors.groupingBy(p -> p.getPostedAt().getDayOfWeek()));
        Map<Integer, List<Post>> hourPosts = posts.stream()
            .collect(Collectors.groupingBy(p -> p.getPostedAt().getHour()));
        Map<String, List<Post>> slotPosts = posts.stream()
            .collect(Collectors.groupingBy(p -> p.getPostedAt().getDayOfWeek().name() + "_" + p.getPostedAt().getHour()));

        String bestDay = dayPosts.entrySet().stream()
            .max(Comparator.comparingDouble(e -> avgEr(e.getValue())))
            .map(e -> e.getKey().name())
            .orElse("MONDAY");

        Integer bestHour = hourPosts.entrySet().stream()
            .max(Comparator.comparingDouble(e -> avgEr(e.getValue())))
            .map(Map.Entry::getKey)
            .orElse(9);

        List<TimeSlotDTO> topSlots = slotPosts.entrySet().stream()
            .filter(e -> e.getValue().size() >= 2)
            .sorted((a, b) -> Double.compare(avgEr(b.getValue()), avgEr(a.getValue())))
            .limit(3)
            .map(e -> {
                String[] parts = e.getKey().split("_");
                return TimeSlotDTO.builder()
                    .day(parts[0])
                    .hour(Integer.parseInt(parts[1]))
                    .avgEngagementRate(round(avgEr(e.getValue())))
                    .build();
            })
            .collect(Collectors.toList());

        return BestPostingTimeDTO.builder()
            .bestDay(bestDay)
            .bestHour(bestHour)
            .avgEngagementRate(topSlots.isEmpty() ? null : topSlots.get(0).getAvgEngagementRate())
            .timezone("IST")
            .topSlots(topSlots)
            .build();
    }

    // --- Top Commenters from DB ---
    private List<TopCommenterDetailDTO> buildTopCommenters(Long creatorId) {
        return topCommenterRepository.findByCreatorIdOrderBySuperfanScoreDesc(creatorId).stream()
            .limit(5)
            .map(tc -> TopCommenterDetailDTO.builder()
                .username(tc.getUsername())
                .commentCount(tc.getCommentCount())
                .totalLikesReceived(tc.getTotalLikesReceived())
                .superfanScore(tc.getSuperfanScore())
                .firstCommentedAt(tc.getFirstCommentedAt())
                .lastCommentedAt(tc.getLastCommentedAt())
                .build())
            .collect(Collectors.toList());
    }

    // --- #6: Questions vs Statements ---
    private QuestionsVsStatementsDTO buildQuestionsVsStatements(List<Post> posts, List<Comment> comments) {
        Map<Long, List<Comment>> commentsByPost = comments.stream()
            .collect(Collectors.groupingBy(c -> c.getPost().getId()));

        long totalQuestions = comments.stream().filter(c -> Boolean.TRUE.equals(c.getIsQuestion())).count();
        long totalStatements = comments.size() - totalQuestions;

        double questionHeavyLikes = 0;
        int questionHeavyCount = 0;
        double statementHeavyLikes = 0;
        int statementHeavyCount = 0;

        for (Post post : posts) {
            List<Comment> pc = commentsByPost.getOrDefault(post.getId(), Collections.emptyList());
            if (pc.isEmpty()) continue;

            long qs = pc.stream().filter(c -> Boolean.TRUE.equals(c.getIsQuestion())).count();
            double qPct = qs * 100.0 / pc.size();

            if (qPct >= 50) {
                questionHeavyLikes += post.getMetrics().getLikes();
                questionHeavyCount++;
            } else {
                statementHeavyLikes += post.getMetrics().getLikes();
                statementHeavyCount++;
            }
        }

        Double qAvg = questionHeavyCount > 0 ? round(questionHeavyLikes / questionHeavyCount) : null;
        Double sAvg = statementHeavyCount > 0 ? round(statementHeavyLikes / statementHeavyCount) : null;
        Double delta = (qAvg != null && sAvg != null) ? round(qAvg - sAvg) : null;

        double total = comments.size();
        return QuestionsVsStatementsDTO.builder()
            .questionCount(totalQuestions)
            .statementCount(totalStatements)
            .questionPct(total > 0 ? round(totalQuestions * 100.0 / total) : null)
            .statementPct(total > 0 ? round(totalStatements * 100.0 / total) : null)
            .questionHeavyPostAvgLikes(qAvg)
            .statementHeavyPostAvgLikes(sAvg)
            .likeDelta(delta)
            .build();
    }

    // --- #14: Questions Insight ---
    private QuestionsInsightDTO buildQuestionsInsight(Long creatorId, List<Comment> comments) {
        LocalDate weekStart = LocalDate.now().with(DayOfWeek.MONDAY);
        LocalDate prevWeekStart = weekStart.minusWeeks(1);

        long totalQuestions = comments.stream().filter(c -> Boolean.TRUE.equals(c.getIsQuestion())).count();
        long unanswered = comments.stream()
            .filter(c -> Boolean.TRUE.equals(c.getIsQuestion()) && c.getReplyCount() == 0)
            .count();

        long thisWeek = comments.stream()
            .filter(c -> Boolean.TRUE.equals(c.getIsQuestion()))
            .filter(c -> !c.getCommentedAt().toLocalDate().isBefore(weekStart))
            .count();

        long lastWeek = comments.stream()
            .filter(c -> Boolean.TRUE.equals(c.getIsQuestion()))
            .filter(c -> !c.getCommentedAt().toLocalDate().isBefore(prevWeekStart)
                && c.getCommentedAt().toLocalDate().isBefore(weekStart))
            .count();

        // Top 5 questions by like_count
        List<TopQuestionDTO> topQuestions = comments.stream()
            .filter(c -> Boolean.TRUE.equals(c.getIsQuestion()))
            .sorted(Comparator.comparingInt(c -> -(c.getLikeCount() != null ? c.getLikeCount() : 0)))
            .limit(5)
            .map(c -> TopQuestionDTO.builder()
                .text(c.getText())
                .username(c.getUsername())
                .likeCount(c.getLikeCount())
                .commentedAt(c.getCommentedAt())
                .build())
            .collect(Collectors.toList());

        return QuestionsInsightDTO.builder()
            .totalQuestions(totalQuestions)
            .unansweredCount(unanswered)
            .questionsThisWeek(thisWeek)
            .deltaVsLastWeek(thisWeek - lastWeek)
            .topQuestions(topQuestions)
            .build();
    }

    // --- #24: Most Liked Comments ---
    private List<MostLikedCommentDTO> buildMostLikedComments(List<Post> posts, List<Comment> comments) {
        // Scope: comments on last 10 posts
        Set<Long> last10PostIds = posts.stream()
            .limit(10)
            .map(Post::getId)
            .collect(Collectors.toSet());

        return comments.stream()
            .filter(c -> last10PostIds.contains(c.getPost().getId()))
            .sorted(Comparator.comparingInt(c -> -(c.getLikeCount() != null ? c.getLikeCount() : 0)))
            .limit(10)
            .map(c -> MostLikedCommentDTO.builder()
                .text(c.getText())
                .username(c.getUsername())
                .likeCount(c.getLikeCount())
                .postCaption(c.getPost().getCaption() != null && c.getPost().getCaption().length() > 80
                    ? c.getPost().getCaption().substring(0, 80) + "..." : c.getPost().getCaption())
                .postPermalink(c.getPost().getPermalink())
                .commentedAt(c.getCommentedAt())
                .build())
            .collect(Collectors.toList());
    }

    // --- #7: CTA Insight ---
    private CtaInsightDTO buildCtaInsight(List<Post> posts) {
        List<Post> ctaPosts = posts.stream().filter(p -> Boolean.TRUE.equals(p.getHasCta())).collect(Collectors.toList());
        List<Post> noCtaPosts = posts.stream().filter(p -> !Boolean.TRUE.equals(p.getHasCta())).collect(Collectors.toList());

        OptionalDouble ctaAvg = ctaPosts.stream()
            .mapToDouble(p -> p.getMetrics().getLikes() + p.getMetrics().getComments())
            .average();

        OptionalDouble noCtaAvg = noCtaPosts.stream()
            .mapToDouble(p -> p.getMetrics().getLikes() + p.getMetrics().getComments())
            .average();

        // CTA type breakdown
        Map<String, Integer> ctaBreakdown = ctaPosts.stream()
            .filter(p -> p.getCtaType() != null)
            .collect(Collectors.groupingBy(Post::getCtaType, Collectors.summingInt(p -> 1)));

        String topCtaType = ctaBreakdown.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(null);

        return CtaInsightDTO.builder()
            .ctaPostCount(ctaPosts.size())
            .noCtaPostCount(noCtaPosts.size())
            .ctaAvgEngagement(ctaAvg.isPresent() ? round(ctaAvg.getAsDouble()) : null)
            .noCtaAvgEngagement(noCtaAvg.isPresent() ? round(noCtaAvg.getAsDouble()) : null)
            .engagementLift(ctaAvg.isPresent() && noCtaAvg.isPresent()
                ? round(ctaAvg.getAsDouble() - noCtaAvg.getAsDouble()) : null)
            .topCtaType(topCtaType)
            .ctaTypeBreakdown(ctaBreakdown)
            .build();
    }

    // --- #20: Caption Length vs Engagement ---
    private CaptionLengthInsightDTO buildCaptionLengthInsight(List<Post> posts) {
        List<Post> shortPosts = posts.stream().filter(p -> p.getCaptionLength() != null && p.getCaptionLength() < 100).collect(Collectors.toList());
        List<Post> mediumPosts = posts.stream().filter(p -> p.getCaptionLength() != null && p.getCaptionLength() >= 100 && p.getCaptionLength() <= 300).collect(Collectors.toList());
        List<Post> longPosts = posts.stream().filter(p -> p.getCaptionLength() != null && p.getCaptionLength() > 300).collect(Collectors.toList());

        Double shortEr = avgErForPosts(shortPosts);
        Double mediumEr = avgErForPosts(mediumPosts);
        Double longEr = avgErForPosts(longPosts);

        // Find best bucket (min 5 posts)
        String bestBucket = "SHORT";
        double bestEr = shortEr != null ? shortEr : 0;
        if (mediumPosts.size() >= 5 && mediumEr != null && mediumEr > bestEr) {
            bestBucket = "MEDIUM";
            bestEr = mediumEr;
        }
        if (longPosts.size() >= 5 && longEr != null && longEr > bestEr) {
            bestBucket = "LONG";
        }

        String recommendation = switch (bestBucket) {
            case "LONG" -> "Your audience engages more with longer storytelling captions (300+ chars)";
            case "MEDIUM" -> "Medium-length captions (100-300 chars) perform best for your audience";
            default -> "Short punchy captions (<100 chars) work best — keep it concise";
        };

        return CaptionLengthInsightDTO.builder()
            .shortCount(shortPosts.size())
            .mediumCount(mediumPosts.size())
            .longCount(longPosts.size())
            .shortAvgEngagementRate(shortEr)
            .mediumAvgEngagementRate(mediumEr)
            .longAvgEngagementRate(longEr)
            .bestBucket(bestBucket)
            .recommendation(recommendation)
            .build();
    }

    // --- Helpers ---

    private double avgEr(List<Post> posts) {
        return posts.stream()
            .map(Post::getMetrics)
            .filter(m -> m.getReach() != null && m.getReach() > 0)
            .mapToDouble(m -> {
                int likes = m.getLikes() != null ? m.getLikes() : 0;
                int comments = m.getComments() != null ? m.getComments() : 0;
                int saves = m.getSaves() != null ? m.getSaves() : 0;
                int shares = m.getShares() != null ? m.getShares() : 0;
                return (likes + comments + saves + shares) * 100.0 / m.getReach();
            })
            .average()
            .orElse(0.0);
    }

    private Double avgErForPosts(List<Post> posts) {
        if (posts.isEmpty()) return null;
        OptionalDouble avg = posts.stream()
            .map(Post::getMetrics)
            .filter(m -> m.getEngagementRate() != null)
            .mapToDouble(PostMetrics::getEngagementRate)
            .average();
        return avg.isPresent() ? round(avg.getAsDouble()) : null;
    }

    private Double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
