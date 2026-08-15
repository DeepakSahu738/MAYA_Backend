package com.MAYA.MAYA.DTO.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Full analytics dashboard response — all 24 metrics in one payload.
 * Served from pre-computed weekly_reports.report_json or computed on-demand.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponseDTO {

    // --- Creator Info ---
    private Long creatorId;
    private String username;
    private String niche;
    private LocalDateTime generatedAt;

    // --- #16: Account Health Score ---
    private AccountHealthScoreDTO healthScore;

    // --- Rate Cards (#1-5, #8, #10, #11) ---
    private List<TimeSeriesMetricDTO> rateCards;

    // --- #9: Engagement Trend (weekly data points) ---
    private List<WeeklyEngagementPointDTO> engagementTrend;
    private String trendDirection; // GROWING, DECLINING, FLAT

    // --- #17: Best / Worst Posts ---
    private List<PostPerformanceDTO> bestPosts;
    private List<PostPerformanceDTO> worstPosts;

    // --- #15: Content Mix ---
    private ContentMixDTO contentMix;

    // --- #21: Best Posting Time ---
    private BestPostingTimeDTO bestPostingTime;

    // --- #18: Most-Used Hashtags + #19: Top Performing Hashtags ---
    private List<HashtagPerformanceDTO> mostUsedHashtags;
    private List<HashtagPerformanceDTO> topPerformingHashtags;

    // --- #23: Top Commenters / Superfans ---
    private List<TopCommenterDetailDTO> topCommenters;

    // --- #13: Sentiment Breakdown ---
    private SentimentBreakdownDTO sentimentBreakdown;

    // --- #22: Common Words in Comments ---
    private List<CommentInsightDTO> commonWords;

    // --- #6: Questions vs Statements ---
    private QuestionsVsStatementsDTO questionsVsStatements;

    // --- #14: Questions Insight ---
    private QuestionsInsightDTO questionsInsight;

    // --- #24: Most Liked Comments ---
    private List<MostLikedCommentDTO> mostLikedComments;

    // --- #7: CTA Insight ---
    private CtaInsightDTO ctaInsight;

    // --- #20: Caption Length vs Engagement ---
    private CaptionLengthInsightDTO captionLengthInsight;

    // --- #12: Profile View → Follow Conversion (null when unavailable) ---
    private TimeSeriesMetricDTO profileConversion;

    // ======== Inner DTOs ========

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeeklyEngagementPointDTO {
        private String week; // e.g. "2026-W23"
        private String weekStart; // e.g. "2026-06-01"
        private Long totalEngagement;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContentMixDTO {
        private Integer imageCount;
        private Integer videoCount;
        private Double imagePct;
        private Double videoPct;
        private Double imageAvgEngagementRate;
        private Double videoAvgEngagementRate;
        private String betterFormat;
        private String recommendation;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BestPostingTimeDTO {
        private String bestDay;
        private Integer bestHour;
        private Double avgEngagementRate;
        private String timezone;
        private List<TimeSlotDTO> topSlots;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeSlotDTO {
        private String day;
        private Integer hour;
        private Double avgEngagementRate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopCommenterDetailDTO {
        private String username;
        private Integer commentCount;
        private Long totalLikesReceived;
        private Double superfanScore;
        private LocalDateTime firstCommentedAt;
        private LocalDateTime lastCommentedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionsVsStatementsDTO {
        private Long questionCount;
        private Long statementCount;
        private Double questionPct;
        private Double statementPct;
        private Double questionHeavyPostAvgLikes;
        private Double statementHeavyPostAvgLikes;
        private Double likeDelta; // question avg - statement avg
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionsInsightDTO {
        private Long totalQuestions;
        private Long unansweredCount;
        private Long questionsThisWeek;
        private Long deltaVsLastWeek;
        private List<TopQuestionDTO> topQuestions;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopQuestionDTO {
        private String text;
        private String username;
        private Integer likeCount;
        private LocalDateTime commentedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MostLikedCommentDTO {
        private String text;
        private String username;
        private Integer likeCount;
        private String postCaption;
        private String postPermalink;
        private LocalDateTime commentedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CtaInsightDTO {
        private Integer ctaPostCount;
        private Integer noCtaPostCount;
        private Double ctaAvgEngagement;
        private Double noCtaAvgEngagement;
        private Double engagementLift;
        private String topCtaType;
        private Map<String, Integer> ctaTypeBreakdown;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CaptionLengthInsightDTO {
        private Integer shortCount; // <100 chars
        private Integer mediumCount; // 100-300 chars
        private Integer longCount; // >300 chars
        private Double shortAvgEngagementRate;
        private Double mediumAvgEngagementRate;
        private Double longAvgEngagementRate;
        private String bestBucket;
        private String recommendation;
    }
}
