package com.MAYA.MAYA.Controller;

import com.MAYA.MAYA.DTO.analytics.DashboardResponseDTO;
import com.MAYA.MAYA.Entity.instagram.Creator;
import com.MAYA.MAYA.Repository.instagram.CreatorRepository;
import com.MAYA.MAYA.Repository.instagram.WeeklyReportRepository;
import com.MAYA.MAYA.Service.CreatorAccessService;
import com.MAYA.MAYA.Service.analytics.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsDashboardController {

    private final DashboardService dashboardService;
    private final CreatorRepository creatorRepository;
    private final CreatorAccessService creatorAccessService;
    private final WeeklyReportRepository weeklyReportRepository;

    /**
     * Full dashboard for a specific creator — all 24 metrics in one call.
     * Demo creators (5-8): public, no auth needed.
     * Real creators (9+): requires JWT + ownership.
     */
    @GetMapping("/dashboard/{creatorId}")
    public ResponseEntity<?> getDashboard(@PathVariable Long creatorId, @AuthenticationPrincipal Jwt jwt) {
        if (!creatorAccessService.canAccess(creatorId, jwt)) {
            return ResponseEntity.status(403).body(Map.of(
                "error", "Access denied",
                "message", "You don't have access to this creator's data. Connect your account first."
            ));
        }

        DashboardResponseDTO dashboard = dashboardService.buildDashboard(creatorId);
        return ResponseEntity.ok(dashboard);
    }

    /**
     * List all available demo creators (public — for the demo profile selector).
     */
    @GetMapping("/creators")
    public ResponseEntity<List<CreatorSummary>> getCreators() {
        List<CreatorSummary> creators = creatorRepository.findAll().stream()
            .filter(c -> creatorAccessService.isDemoCreator(c.getId()))
            .map(c -> new CreatorSummary(c.getId(), c.getUsername(), c.getNiche()))
            .collect(Collectors.toList());
        return ResponseEntity.ok(creators);
    }

    /**
     * Get weekly reports history for a specific creator.
     * Returns up to 10 weekly reports (most recent first).
     * Frontend uses this to show week-over-week trends and historical comparison.
     *
     * GET /api/analytics/weekly-reports/{creatorId}
     */
    @GetMapping("/weekly-reports/{creatorId}")
    public ResponseEntity<?> getWeeklyReports(@PathVariable Long creatorId, @AuthenticationPrincipal Jwt jwt) {
        if (!creatorAccessService.canAccess(creatorId, jwt)) {
            return ResponseEntity.status(403).body(Map.of(
                "error", "Access denied",
                "message", "You don't have access to this creator's data."
            ));
        }

        List<WeeklyReportSummary> reports = weeklyReportRepository
            .findByCreatorIdOrderByWeekStartDateDesc(creatorId).stream()
            .map(r -> new WeeklyReportSummary(
                r.getId(),
                r.getWeekStartDate().toString(),
                r.getWeekEndDate().toString(),
                r.getGeneratedAt().toString(),
                r.getHealthScore(),
                r.getHealthScoreDelta(),
                r.getAvgEngagementRate(),
                r.getAvgSaveRate(),
                r.getAvgShareRate(),
                r.getAvgReachEfficiency(),
                r.getPostsPublished(),
                r.getSentimentPositivePct(),
                r.getSentimentNeutralPct(),
                r.getSentimentNegativePct(),
                r.getUnansweredQuestionsCount()
            ))
            .collect(Collectors.toList());

        return ResponseEntity.ok(reports);
    }

    record CreatorSummary(Long id, String username, String niche) {}

    record WeeklyReportSummary(
        Long id,
        String weekStartDate,
        String weekEndDate,
        String generatedAt,
        Double healthScore,
        Double healthScoreDelta,
        Double avgEngagementRate,
        Double avgSaveRate,
        Double avgShareRate,
        Double avgReachEfficiency,
        Integer postsPublished,
        Double sentimentPositivePct,
        Double sentimentNeutralPct,
        Double sentimentNegativePct,
        Integer unansweredQuestionsCount
    ) {}
}
