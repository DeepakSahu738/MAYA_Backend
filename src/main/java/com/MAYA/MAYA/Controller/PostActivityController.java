package com.MAYA.MAYA.Controller;

import com.MAYA.MAYA.Entity.instagram.Post;
import com.MAYA.MAYA.Repository.instagram.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostActivityController {

    private final PostRepository postRepository;

    /**
     * GET /api/posts/activity?creatorId=9
     * Returns posting activity for streak tracker and weekly goal progress.
     * Queries actual synced posts (from Phyllo), NOT scheduled_posts.
     */
    @GetMapping("/activity")
    public ResponseEntity<?> getPostActivity(@RequestParam Long creatorId, @AuthenticationPrincipal Jwt jwt) {
        List<Post> allPosts = postRepository.findByCreatorIdOrderByPostedAtDesc(creatorId);

        if (allPosts.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                "postDates", List.of(),
                "thisWeekCount", 0,
                "totalPosts", 0
            ));
        }

        LocalDate today = LocalDate.now();
        LocalDate ninetyDaysAgo = today.minusDays(90);
        LocalDate weekStart = today.with(DayOfWeek.MONDAY);

        // Distinct dates with posts in last 90 days, sorted descending
        List<String> postDates = allPosts.stream()
            .filter(p -> p.getPostedAt() != null)
            .map(p -> p.getPostedAt().toLocalDate())
            .filter(d -> !d.isBefore(ninetyDaysAgo))
            .distinct()
            .sorted((a, b) -> b.compareTo(a))
            .map(LocalDate::toString)
            .collect(Collectors.toList());

        // Posts this week (Monday through today)
        long thisWeekCount = allPosts.stream()
            .filter(p -> p.getPostedAt() != null)
            .filter(p -> !p.getPostedAt().toLocalDate().isBefore(weekStart)
                && !p.getPostedAt().toLocalDate().isAfter(today))
            .count();

        return ResponseEntity.ok(Map.of(
            "postDates", postDates,
            "thisWeekCount", thisWeekCount,
            "totalPosts", allPosts.size()
        ));
    }
}
