package com.MAYA.MAYA.Service.analytics;

import com.MAYA.MAYA.DTO.analytics.AccountHealthScoreDTO;
import com.MAYA.MAYA.DTO.analytics.SentimentBreakdownDTO;
import com.MAYA.MAYA.DTO.analytics.TimeSeriesMetricDTO;
import com.MAYA.MAYA.Entity.instagram.Creator;
import com.MAYA.MAYA.Enums.Platform;
import com.MAYA.MAYA.Repository.instagram.CreatorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Metric #16: Account Health Score (0-100 composite) — platform-aware.
 *
 * Five possible components:
 *   A = engagement     : MIN(overall_engagement_rate / 5.0, 1.0) × 100
 *   B = consistency    : MIN(avg_posts_per_week / 3.0, 1.0) × 100
 *   C = reach dist.    : MIN(reach_efficiency / 70.0, 1.0) × 100
 *   D = sentiment      : MIN(MAX(positive_pct - negative_pct, 0) / 80.0, 1.0) × 100
 *   E = content value  : MIN(save_rate / 3.0, 1.0) × 100
 *
 * Weights are chosen PER PLATFORM and re-normalized to sum to 1.0, so a platform
 * is never penalized for a component it structurally cannot provide (e.g. YouTube
 * has no reach/saves, Facebook has no comment sentiment). This keeps scores
 * comparable and fair across platforms instead of forcing missing components to 0.
 *
 *   Instagram: A=.30 B=.20 C=.20 D=.15 E=.15   (unchanged baseline)
 *   Facebook : A=.40 B=.25 C=.35               (no sentiment, no saves)
 *   YouTube  : A=.55 B=.30 D=.15               (no reach, no saves)
 *   Other    : same as Instagram
 *
 * If engagement (A) cannot be computed, returns 'insufficient data'.
 */
@Service
@RequiredArgsConstructor
public class AccountHealthService {
    
    private final AnalyticsService analyticsService;
    private final CreatorRepository creatorRepository;
    
    public AccountHealthScoreDTO calculateAccountHealthScore(Long creatorId) {
        Creator creator = creatorRepository.findById(creatorId).orElse(null);
        Platform platform = Platform.normalize(creator != null ? creator.getPlatform() : null);

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
        
        // Insufficient data check: need engagement rate (the one universal component)
        if (engagementRate == null) {
            return buildInsufficientData();
        }
        
        // Compute component scores (0-100). Missing inputs => null (excluded, not zeroed).
        Double A = Math.min(engagementRate / 5.0, 1.0) * 100;
        Double B = postsPerWeek != null ? Math.min(postsPerWeek / 3.0, 1.0) * 100 : null;
        Double C = reachEfficiency != null ? Math.min(reachEfficiency / 70.0, 1.0) * 100 : null;
        Double D = (positivePct != null && negativePct != null)
            ? Math.min(Math.max(positivePct - negativePct, 0) / 80.0, 1.0) * 100 : null;
        Double E = saveRate != null ? Math.min(saveRate / 3.0, 1.0) * 100 : null;
        
        // Per-platform weights for which components apply
        Weights w = weightsFor(platform);

        // Weighted average over ONLY the components that both apply to this platform
        // AND have data. Re-normalize by the sum of active weights so the score is 0-100.
        double weightedSum = 0.0;
        double activeWeight = 0.0;
        if (A != null && w.a > 0) { weightedSum += A * w.a; activeWeight += w.a; }
        if (B != null && w.b > 0) { weightedSum += B * w.b; activeWeight += w.b; }
        if (C != null && w.c > 0) { weightedSum += C * w.c; activeWeight += w.c; }
        if (D != null && w.d > 0) { weightedSum += D * w.d; activeWeight += w.d; }
        if (E != null && w.e > 0) { weightedSum += E * w.e; activeWeight += w.e; }

        int score = activeWeight > 0 ? (int) Math.round(weightedSum / activeWeight) : 0;
        String grade = getGrade(score);
        
        // Component scores map — only include components relevant to this platform
        Map<String, Double> components = new LinkedHashMap<>();
        if (w.a > 0 && A != null) components.put("engagement", round(A));
        if (w.b > 0 && B != null) components.put("consistency", round(B));
        if (w.c > 0 && C != null) components.put("reach_distribution", round(C));
        if (w.d > 0 && D != null) components.put("sentiment", round(D));
        if (w.e > 0 && E != null) components.put("content_value", round(E));
        
        // Strengths and improvements — only for components that apply to this platform
        List<String> strengths = new ArrayList<>();
        List<String> improvements = new ArrayList<>();
        
        if (w.a > 0 && A != null) {
            if (A >= 70) strengths.add("Strong engagement rate");
            else improvements.add("Engagement rate needs improvement");
        }
        if (w.b > 0 && B != null) {
            if (B >= 70) strengths.add("Consistent posting schedule");
            else improvements.add("Post more consistently (aim for 3+/week)");
        }
        if (w.c > 0 && C != null) {
            if (C >= 70) strengths.add("Good reach distribution to new audiences");
            else improvements.add("Content not reaching new audiences — review hashtag strategy");
        }
        if (w.d > 0 && D != null) {
            if (D >= 70) strengths.add("Positive community sentiment");
            else improvements.add("Sentiment trending negative — review recent content tone");
        }
        if (w.e > 0 && E != null) {
            if (E >= 70) strengths.add("High save rate — content has lasting value");
            else improvements.add("Low save rate — create more educational/evergreen content");
        }
        
        return new AccountHealthScoreDTO(
            score,
            grade,
            components,
            strengths,
            improvements,
            LocalDateTime.now()
        );
    }

    // --- Platform weight profiles (each set sums to 1.0 over its active components) ---

    private record Weights(double a, double b, double c, double d, double e) {}

    private Weights weightsFor(Platform platform) {
        return switch (platform) {
            case FACEBOOK -> new Weights(0.40, 0.25, 0.35, 0.0, 0.0); // no sentiment, no saves
            case YOUTUBE  -> new Weights(0.55, 0.30, 0.0, 0.15, 0.0); // no reach, no saves
            case INSTAGRAM, OTHER -> new Weights(0.30, 0.20, 0.20, 0.15, 0.15); // baseline
        };
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
            List.of("Not enough posts with engagement data to calculate health score"),
            LocalDateTime.now()
        );
    }
    
    private Double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
