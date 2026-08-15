package com.MAYA.MAYA.DTO.analytics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsDashboardDTO {
    private Long creatorId;
    private String username;
    private AccountHealthScoreDTO healthScore;
    private List<TimeSeriesMetricDTO> timeSeriesMetrics;
    private List<PostPerformanceDTO> bestPosts;
    private List<PostPerformanceDTO> worstPosts;
    private List<HashtagPerformanceDTO> topHashtags;
    private DayHourRecommendationDTO bestPostingTime;
    private List<CommentInsightDTO> commonWords;
    private List<TopCommenterDTO> topCommenters;
    private SentimentBreakdownDTO sentimentBreakdown;
    private LocalDateTime generatedAt;
}
