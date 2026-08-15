package com.MAYA.MAYA.Controller;

import com.MAYA.MAYA.DTO.strategy.WeeklyPlanDTO;
import com.MAYA.MAYA.Entity.instagram.Creator;
import com.MAYA.MAYA.Entity.instagram.ScheduledPost;
import com.MAYA.MAYA.Repository.instagram.CreatorRepository;
import com.MAYA.MAYA.Repository.instagram.ScheduledPostRepository;
import com.MAYA.MAYA.Service.CreatorAccessService;
import com.MAYA.MAYA.Service.RateLimiterService;
import com.MAYA.MAYA.Service.strategy.WeeklyStrategyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Strategy endpoints — the core "wow" feature.
 *
 * POST /api/strategy/generate          → Generate a 7-day content plan
 * POST /api/strategy/generate-and-save → Generate + auto-save all 7 days to calendar
 */
@RestController
@RequestMapping("/api/strategy")
@RequiredArgsConstructor
@Slf4j
public class StrategyController {

    private final WeeklyStrategyService strategyService;
    private final ScheduledPostRepository scheduledPostRepository;
    private final CreatorRepository creatorRepository;
    private final CreatorAccessService creatorAccessService;
    private final RateLimiterService rateLimiterService;

    // Rate limits: 10 strategy generations per hour per session
    private static final int STRATEGY_MAX_REQUESTS = 10;
    private static final long STRATEGY_WINDOW_MS = 3_600_000; // 1 hour

    /**
     * Generate a 7-day content plan (does NOT save to calendar).
     * User reviews the plan, then can save individual days or the whole thing.
     *
     * POST /api/strategy/generate
     * Body: { "creatorId": 9 }
     */
    @PostMapping("/generate")
    public ResponseEntity<?> generatePlan(@RequestBody GenerateRequest request, @AuthenticationPrincipal Jwt jwt) {
        if (!creatorAccessService.canAccess(request.creatorId(), jwt)) {
            return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
        }

        // Rate limit: 5 per hour per creator
        String rateLimitKey = "strategy:" + request.creatorId();
        if (!rateLimiterService.isAllowed(rateLimitKey, STRATEGY_MAX_REQUESTS, STRATEGY_WINDOW_MS)) {
            return ResponseEntity.status(429).body(Map.of(
                "error", "Rate limit exceeded",
                "message", "You can generate up to 5 plans per hour. Please wait before trying again.",
                "remaining", rateLimiterService.getRemaining(rateLimitKey, STRATEGY_MAX_REQUESTS, STRATEGY_WINDOW_MS)
            ));
        }

        log.info("Generating weekly strategy for creator: {}", request.creatorId());
        WeeklyPlanDTO plan = strategyService.generateWeeklyPlan(request.creatorId());
        return ResponseEntity.ok(plan);
    }

    /**
     * Generate a 7-day plan AND auto-save all days to the content calendar as drafts.
     *
     * POST /api/strategy/generate-and-save
     * Body: { "creatorId": 9 }
     * Response: { "plan": {...}, "savedDrafts": 7 }
     */
    @PostMapping("/generate-and-save")
    public ResponseEntity<?> generateAndSave(@RequestBody GenerateRequest request, @AuthenticationPrincipal Jwt jwt) {
        if (!creatorAccessService.canAccess(request.creatorId(), jwt)) {
            return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
        }

        // Rate limit: same bucket as generate
        String rateLimitKey = "strategy:" + request.creatorId();
        if (!rateLimiterService.isAllowed(rateLimitKey, STRATEGY_MAX_REQUESTS, STRATEGY_WINDOW_MS)) {
            return ResponseEntity.status(429).body(Map.of(
                "error", "Rate limit exceeded",
                "message", "You can generate up to 5 plans per hour. Please wait before trying again."
            ));
        }

        log.info("Generating + saving weekly strategy for creator: {}", request.creatorId());
        WeeklyPlanDTO plan = strategyService.generateWeeklyPlan(request.creatorId());

        // Save each day as a calendar draft
        Creator creator = creatorRepository.findById(request.creatorId()).orElse(null);
        int saved = 0;

        if (creator != null && plan.getDays() != null) {
            for (WeeklyPlanDTO.DayPlanDTO day : plan.getDays()) {
                try {
                    ScheduledPost draft = new ScheduledPost();
                    draft.setCreator(creator);
                    draft.setCaption(day.getCaptionDraft() != null ? day.getCaptionDraft() : day.getPostIdea());
                    draft.setHashtags(day.getHashtags() != null ? String.join(",", day.getHashtags()) : "");
                    draft.setMediaType(day.getFormat() != null ? day.getFormat() : "IMAGE");
                    draft.setApprovalStatus(ScheduledPost.ApprovalStatus.PENDING);

                    // Parse date + time
                    LocalDate date = LocalDate.parse(day.getDate());
                    LocalTime time = day.getBestTime() != null ? LocalTime.parse(day.getBestTime()) : LocalTime.of(9, 0);
                    draft.setScheduledFor(LocalDateTime.of(date, time));

                    scheduledPostRepository.save(draft);
                    saved++;
                } catch (Exception e) {
                    log.warn("Failed to save draft for {}: {}", day.getDay(), e.getMessage());
                }
            }
        }

        log.info("Saved {} calendar drafts for creator: {}", saved, request.creatorId());

        return ResponseEntity.ok(Map.of(
            "plan", plan,
            "savedDrafts", saved,
            "message", String.format("Generated plan + saved %d drafts to your calendar!", saved)
        ));
    }

    record GenerateRequest(Long creatorId) {}
}
