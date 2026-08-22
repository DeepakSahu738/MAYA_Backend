package com.MAYA.MAYA.Controller;

import com.MAYA.MAYA.Entity.WeeklyGoal;
import com.MAYA.MAYA.Repository.WeeklyGoalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/goals")
@RequiredArgsConstructor
public class WeeklyGoalController {

    private final WeeklyGoalRepository weeklyGoalRepository;

    /**
     * GET /api/goals/current?creatorId=9
     * Returns the goal for the current week (Monday to Sunday).
     */
    @GetMapping("/current")
    public ResponseEntity<?> getCurrentGoal(@RequestParam Long creatorId, @AuthenticationPrincipal Jwt jwt) {
        Long userId = extractUserId(jwt);
        LocalDate weekStart = LocalDate.now().with(DayOfWeek.MONDAY);

        Optional<WeeklyGoal> goal = weeklyGoalRepository.findByUserIdAndCreatorIdAndWeekStart(userId, creatorId, weekStart);

        if (goal.isPresent()) {
            return ResponseEntity.ok(Map.of(
                "target", goal.get().getTarget(),
                "weekStart", weekStart.toString(),
                "exists", true
            ));
        } else {
            return ResponseEntity.ok(Map.of(
                "target", 0,
                "weekStart", weekStart.toString(),
                "exists", false
            ));
        }
    }

    /**
     * POST /api/goals/set
     * Body: { "creatorId": 9, "target": 5 }
     * Creates or updates the goal for the current week.
     */
    @PostMapping("/set")
    public ResponseEntity<?> setGoal(@RequestBody SetGoalRequest request, @AuthenticationPrincipal Jwt jwt) {
        Long userId = extractUserId(jwt);

        // Validate target
        if (request.target() < 1 || request.target() > 30) {
            return ResponseEntity.badRequest().body(Map.of("error", "Target must be between 1 and 30"));
        }

        LocalDate weekStart = LocalDate.now().with(DayOfWeek.MONDAY);

        // Upsert: find existing or create new
        WeeklyGoal goal = weeklyGoalRepository
            .findByUserIdAndCreatorIdAndWeekStart(userId, request.creatorId(), weekStart)
            .orElse(new WeeklyGoal());

        goal.setUserId(userId);
        goal.setCreatorId(request.creatorId());
        goal.setWeekStart(weekStart);
        goal.setTarget(request.target());

        weeklyGoalRepository.save(goal);

        return ResponseEntity.ok(Map.of(
            "message", "Goal set",
            "target", request.target(),
            "weekStart", weekStart.toString()
        ));
    }

    /**
     * DELETE /api/goals/reset?creatorId=9
     * Resets (deletes) the weekly goal for the current week back to 0.
     */
    @DeleteMapping("/reset")
    public ResponseEntity<?> resetGoal(@RequestParam Long creatorId, @AuthenticationPrincipal Jwt jwt) {
        Long userId = extractUserId(jwt);
        LocalDate weekStart = LocalDate.now().with(DayOfWeek.MONDAY);

        Optional<WeeklyGoal> goal = weeklyGoalRepository.findByUserIdAndCreatorIdAndWeekStart(userId, creatorId, weekStart);

        if (goal.isPresent()) {
            weeklyGoalRepository.delete(goal.get());
            return ResponseEntity.ok(Map.of(
                "message", "Goal reset successfully",
                "weekStart", weekStart.toString()
            ));
        } else {
            return ResponseEntity.ok(Map.of(
                "message", "No goal found for this week — nothing to reset",
                "weekStart", weekStart.toString()
            ));
        }
    }

    record SetGoalRequest(Long creatorId, Integer target) {}

    private Long extractUserId(Jwt jwt) {
        Object claim = jwt.getClaim("userID");
        if (claim instanceof Long) return (Long) claim;
        if (claim instanceof Integer) return ((Integer) claim).longValue();
        if (claim instanceof Number) return ((Number) claim).longValue();
        throw new RuntimeException("Invalid userID claim in JWT");
    }
}
