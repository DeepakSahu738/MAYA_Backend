package com.MAYA.MAYA.Service.analytics;

import com.MAYA.MAYA.DTO.analytics.AccountHealthScoreDTO;
import com.MAYA.MAYA.DTO.analytics.SentimentBreakdownDTO;
import com.MAYA.MAYA.DTO.analytics.TimeSeriesMetricDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Metric #16: Account Health Score (0-100 composite).
 * 
 * Formula:
 *   A = MIN(overall_engagement_rate / 5.0, 1.0) × 100  [weight: 30%]
 *   B = MIN(avg_posts_per_week / 3.0, 1.0) × 100       [weight: 20%]
 *   C = MIN(reach_efficiency / 70.0, 1.0) × 100         [weight: 20%]
 *   D = MIN(MAX(positive_pct - negative_pct, 0) / 80.0, 1.0) × 100 [weight: 15%]
 *   E = MIN(save_rate / 3.0, 1.0) × 100                 [weight: 15%]
 *   
 *   health_score = (A×0.30) + (B×0.20) + (C×0.20) + (D×0.15) + (E×0.15)
 *
 * If fewer than 3 posts have reach data, returns 'insufficient data'.
 */
@Service
@RequiredArgsConstructor
public class AccountHealthService {
    
    private final AnalyticsService analyticsService;
    
    public AccountHealthScoreDTO calculateAccountHealthScore(Long creatorId) {
        // Gather component inputs
        TimeSeriesMetricDTO engagementMetric = analyticsService.calculateOverallEngagement(creatorId, null);
        TimeSeriesMetricDTO frequencyMetric = analyticsService.calculatePostingFrequency(creatorId);
        TimeSeriesMetricDTO reachMetric = analyticsService.calculateReachEfficiency(creatorId, null);
        TimeSeriesMetricDTO saveMetric = analyticsService.calculateSaveRate(creatorId, null);
        SentimentBreakdownDTO sentiment = analyticsService.getSentimentBreakdown(creatorId);
        
        Double engagementRate = engagementMetric.getCurrentValue();
        Double postsPerWeek = frequencyMetric.getCurrentValue();
        Double reachEfficiency = reachMetric.getCurrentValue();
        Double saveRate = saveMetric.getCurrentValue();
        Double positivePct = sentiment.getPositivePercentage();
        Double negativePct = sentiment.getNegativePercentage();
        
        // Insufficient data check: need engagement rate (gated by reach)
        if (engagementRate == null) {
            return buildInsufficientData();
        }
        
        // Compute components
        double A = Math.min(engagementRate / 5.0, 1.0) * 100;
        double B = postsPerWeek != null ? Math.min(postsPerWeek / 3.0, 1.0) * 100 : 0;
        double C = reachEfficiency != null ? Math.min(reachEfficiency / 70.0, 1.0) * 100 : 0;
        double D = (positivePct != null && negativePct != null)
            ? Math.min(Math.max(positivePct - negativePct, 0) / 80.0, 1.0) * 100 : 0;
        double E = saveRate != null ? Math.min(saveRate / 3.0, 1.0) * 100 : 0;
        
        double healthScore = (A * 0.30) + (B * 0.20) + (C * 0.20) + (D * 0.15) + (E * 0.15);
        int score = (int) Math.round(healthScore);
        
        // Grade
        String grade = getGrade(score);
        
        // Component scores map
        Map<String, Double> components = new LinkedHashMap<>();
        components.put("engagement", round(A));
        components.put("consistency", round(B));
        components.put("reach_distribution", round(C));
        components.put("sentiment", round(D));
        components.put("content_value", round(E));
        
        // Strengths and improvements
        List<String> strengths = new ArrayList<>();
        List<String> improvements = new ArrayList<>();
        
        if (A >= 70) strengths.add("Strong engagement rate");
        else improvements.add("Engagement rate needs improvement");
        
        if (B >= 70) strengths.add("Consistent posting schedule");
        else improvements.add("Post more consistently (aim for 3+/week)");
        
        if (C >= 70) strengths.add("Good reach distribution to new audiences");
        else if (reachEfficiency != null) improvements.add("Content not reaching new audiences — review hashtag strategy");
        
        if (D >= 70) strengths.add("Positive community sentiment");
        else if (positivePct != null) improvements.add("Sentiment trending negative — review recent content tone");
        
        if (E >= 70) strengths.add("High save rate — content has lasting value");
        else if (saveRate != null) improvements.add("Low save rate — create more educational/evergreen content");
        
        return new AccountHealthScoreDTO(
            score,
            grade,
            components,
            strengths,
            improvements,
            LocalDateTime.now()
        );
    }
    
    private String getGrade(int score) {
        if (score >= 80) return "Excellent";
        if (score >= 60) return "Good";
        if (score >= 40) return "Fair";
        return "Critical";
    }
    
    private AccountHealthScoreDTO buildInsufficientData() {
        return new AccountHealthScoreDTO(
            null,
            "Insufficient Data",
            Collections.emptyMap(),
            Collections.emptyList(),
            List.of("Not enough posts with reach data to calculate health score (need at least 3)"),
            LocalDateTime.now()
        );
    }
    
    private Double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
