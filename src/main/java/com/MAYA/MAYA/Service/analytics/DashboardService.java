package com.MAYA.MAYA.Service.analytics;

import com.MAYA.MAYA.DTO.analytics.*;
import com.MAYA.MAYA.DTO.analytics.DashboardResponseDTO.*;
import com.MAYA.MAYA.Entity.instagram.*;
import com.MAYA.MAYA.Enums.Platform;
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

        Platform platform = Platform.normalize(creator.getPlatform());

        List<Post> posts = postRepository.findByCreatorIdOrderByPostedAtDesc(creatorId);
        List<Comment> comments = commentRepository.findByCreatorIdOrderByCommentedAtDesc(creatorId);

        // If no posts yet (still seeding or sync in progress), return a minimal "loading" response
        if (posts.isEmpty()) {
            return DashboardResponseDTO.builder()
                .creatorId(creatorId)
                .username(creator.getUsername())
                .niche(creator.getNiche())
                .platform(platform.name())
                .generatedAt(LocalDateTime.now())
                .build();
        }

        // Platforms that expose comment bodies via the integration.
        // Facebook does NOT, so sentiment / questions / commenters are skipped there.
        boolean hasComments = platform != Platform.FACEBOOK;

        return DashboardResponseDTO.builder()
            .creatorId(creatorId)
            .username(creator.getUsername())
            .niche(creator.getNiche())
            .platform(platform.name())
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
            .topCommenters(hasComments ? buildTopCommenters(creatorId) : null)
            .sentimentBreakdown(hasComments ? analyticsService.getSentimentBreakdown(creatorId) : null)
            .commonWords(hasComments ? snapshotAnalyticsService.getCommonCommentWords(creatorId, 20) : null)
            .questionsVsStatements(hasComments ? buildQuestionsVsStatements(posts, comments) : null)
            .questionsInsight(hasComments ? buildQuestionsInsight(creatorId, comments) : null)
            .mostLikedComments(hasComments ? buildMostLikedComments(posts, comments) : null)
            .ctaInsight(buildCtaInsight(posts))
            .captionLengthInsight(buildCaptionLengthInsight(posts))
            .profileConversion(analyticsService.calculateProfileToFollowConversion(creatorId))
            .platformInsights(buildPlatformInsights(creatorId, platform))
            .unavailableMetrics(buildUnavailableMetrics(platform))
            .build();
    }

    // --- Platform-specific hero cards ---
    private List<PlatformInsightCardDTO> buildPlatformInsights(Long creatorId, Platform platform) {
        List<PlatformInsightCardDTO> cards = new ArrayList<>();
        switch (platform) {
            case YOUTUBE -> {
                addCard(cards, "yt_view_engagement", "View Engagement Rate",
                    analyticsService.calculateViewEngagementRate(creatorId), "%",
                    "Likes + comments as a share of views");
                addCard(cards, "yt_like_to_view", "Like-to-View Ratio",
                    analyticsService.calculateLikeToViewRatio(creatorId), "%",
                    "Likes as a share of total views");
                addCard(cards, "yt_approval_rate", "Approval Rate",
                    analyticsService.calculateApprovalRate(creatorId), "%",
                    "Likes vs likes + dislikes");
                addCard(cards, "yt_views_per_sub", "Views per Subscriber",
                    analyticsService.calculateViewsPerSubscriber(creatorId), "views/sub",
                    "Average views relative to your subscriber base");
            }
            case FACEBOOK -> {
                addCard(cards, "fb_reach_efficiency", "Reach Efficiency",
                    analyticsService.calculateFbReachEfficiency(creatorId), "%",
                    "Unique reach as a share of total impressions");
                addCard(cards, "fb_watch_time", "Watch Time",
                    analyticsService.calculateWatchTimeHours(creatorId), "hours",
                    "Total time your audience spent watching");
                addCard(cards, "fb_view_rate", "View Rate",
                    analyticsService.calculateViewRate(creatorId), "%",
                    "Views as a share of people reached");
                addCard(cards, "fb_click_signal", "Clicks",
                    analyticsService.calculateClickSignal(creatorId), "count",
                    "Total clicks across recent posts");
            }
            case INSTAGRAM, OTHER -> {
                // Instagram's hero metrics already live in the core rateCards
                // (save rate, share rate, reach efficiency, play-through), so no
                // extra platform-specific cards are needed. OTHER uses the same.
            }
        }
        return cards;
    }

    private void addCard(List<PlatformInsightCardDTO> cards, String key, String label,
                         Double value, String unit, String description) {
        cards.add(PlatformInsightCardDTO.builder()
            .key(key).label(label).value(value).unit(unit).delta(null).description(description)
            .build());
    }

    // --- Metrics unavailable per platform (drives UI hide + AI awareness) ---
    private List<UnavailableMetricDTO> buildUnavailableMetrics(Platform platform) {
        List<UnavailableMetricDTO> list = new ArrayList<>();
        switch (platform) {
            case YOUTUBE -> {
                list.add(unavailable("reach_efficiency", "Reach Efficiency",
                    "YouTube doesn't expose reach or impressions, so reach-based metrics aren't available."));
                list.add(unavailable("save_rate", "Save Rate",
                    "YouTube doesn't report saves."));
                list.add(unavailable("share_rate", "Share Rate",
                    "YouTube doesn't report shares through the current integration."));
            }
            case FACEBOOK -> {
                list.add(unavailable("sentiment_breakdown", "Comment Sentiment",
                    "Facebook comment content isn't available through the current integration, so sentiment can't be analyzed."));
                list.add(unavailable("questions_insight", "Questions in Comments",
                    "Facebook comment content isn't available, so questions can't be detected."));
                list.add(unavailable("top_commenters", "Top Commenters",
                    "Facebook comment content isn't available, so top commenters can't be identified."));
                list.add(unavailable("save_rate", "Save Rate",
                    "Facebook doesn't report saves."));
                list.add(unavailable("share_rate", "Share Rate",
                    "Facebook doesn't report shares through the current integration."));
            }
            case INSTAGRAM, OTHER -> {
                // Instagram supports the full metric set.
            }
        }
        return list;
    }

    private UnavailableMetricDTO unavailable(String key, String label, String reason) {
        return UnavailableMetricDTO.builder().key(key).label(label).reason(reason).build();
    }

    /**
     * Builds a short platform-awareness block for the AI system message so the
     * assistant knows which metrics it can and cannot provide for this creator's
     * platform, and explains limitations instead of inventing numbers.
     */
    public String buildPlatformAiContext(Long creatorId) {
        Creator creator = creatorRepository.findById(creatorId).orElse(null);
        Platform platform = Platform.normalize(creator != null ? creator.getPlatform() : null);

        StringBuilder sb = new StringBuilder();
        sb.append("PLATFORM: This creator is on ").append(platform.name()).append(".\n");

        List<UnavailableMetricDTO> unavailable = buildUnavailableMetrics(platform);
        if (unavailable.isEmpty()) {
            sb.append("All standard metrics are available for this platform.");
        } else {
            sb.append("UNAVAILABLE metrics for this platform (do NOT fabricate these — explain they aren't available and suggest an alternative):\n");
            for (UnavailableMetricDTO m : unavailable) {
                sb.append("- ").append(m.getLabel()).append(": ").append(m.getReason()).append("\n");
            }
        }
        return sb.toString();
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
                    return (m.getLikeCount() != null ? m.getLikeCount() : 0)
                        + (m.getCommentCount() != null ? m.getCommentCount() : 0)
                        + (m.getSaveCount() != null ? m.getSaveCount() : 0)
                        + (m.getShareCount() != null ? m.getShareCount() : 0);
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
                    weeklyTotals[w] += (m.getLikeCount() != null ? m.getLikeCount() : 0)
                        + (m.getCommentCount() != null ? m.getCommentCount() : 0)
                        + (m.getSaveCount() != null ? m.getSaveCount() : 0)
                        + (m.getShareCount() != null ? m.getShareCount() : 0);
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
        long imageCount = posts.stream().filter(p -> "IMAGE".equalsIgnoreCase(p.getFormat())).count();
        long videoCount = posts.stream().filter(p -> "VIDEO".equalsIgnoreCase(p.getFormat())).count();
        long total = imageCount + videoCount;
        if (total == 0) return null;

        OptionalDouble imageEr = posts.stream()
            .filter(p -> "IMAGE".equalsIgnoreCase(p.getFormat()))
            .map(Post::getMetrics)
            .filter(m -> m != null && m.getEngagementRate() != null && m.getEngagementRate() <= 100.0)
            .mapToDouble(PostMetrics::getEngagementRate)
            .average();

        OptionalDouble videoEr = posts.stream()
            .filter(p -> "VIDEO".equalsIgnoreCase(p.getFormat()))
            .map(Post::getMetrics)
            .filter(m -> m != null && m.getEngagementRate() != null && m.getEngagementRate() <= 100.0)
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
                questionHeavyLikes += post.getMetrics().getLikeCount();
                questionHeavyCount++;
            } else {
                statementHeavyLikes += post.getMetrics().getLikeCount();
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
                .postPermalink(c.getPost().getUrl())
                .commentedAt(c.getCommentedAt())
                .build())
            .collect(Collectors.toList());
    }

    // --- #7: CTA Insight ---
    private CtaInsightDTO buildCtaInsight(List<Post> posts) {
        List<Post> ctaPosts = posts.stream().filter(p -> Boolean.TRUE.equals(p.getHasCta())).collect(Collectors.toList());
        List<Post> noCtaPosts = posts.stream().filter(p -> !Boolean.TRUE.equals(p.getHasCta())).collect(Collectors.toList());

        OptionalDouble ctaAvg = ctaPosts.stream()
            .mapToDouble(p -> p.getMetrics().getLikeCount() + p.getMetrics().getCommentCount())
            .average();

        OptionalDouble noCtaAvg = noCtaPosts.stream()
            .mapToDouble(p -> p.getMetrics().getLikeCount() + p.getMetrics().getCommentCount())
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
            .filter(m -> m.getReachOrganicCount() != null && m.getReachOrganicCount() > 0)
            .mapToDouble(m -> {
                int likes = m.getLikeCount() != null ? m.getLikeCount() : 0;
                int comments = m.getCommentCount() != null ? m.getCommentCount() : 0;
                int saves = m.getSaveCount() != null ? m.getSaveCount() : 0;
                int shares = m.getShareCount() != null ? m.getShareCount() : 0;
                return (likes + comments + saves + shares) * 100.0 / m.getReachOrganicCount();
            })
            .average()
            .orElse(0.0);
    }

    private Double avgErForPosts(List<Post> posts) {
        if (posts.isEmpty()) return null;
        OptionalDouble avg = posts.stream()
            .map(Post::getMetrics)
            .filter(m -> m != null && m.getEngagementRate() != null && m.getEngagementRate() <= 100.0)
            .mapToDouble(PostMetrics::getEngagementRate)
            .average();
        return avg.isPresent() ? round(avg.getAsDouble()) : null;
    }

    private Double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
