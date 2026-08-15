package com.MAYA.MAYA.Controller;

import com.MAYA.MAYA.Entity.instagram.Creator;
import com.MAYA.MAYA.Entity.instagram.ScheduledPost;
import com.MAYA.MAYA.Repository.instagram.CreatorRepository;
import com.MAYA.MAYA.Repository.instagram.ScheduledPostRepository;
import com.MAYA.MAYA.Service.CreatorAccessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Content calendar / post scheduling endpoints.
 *
 * Supports multi-platform: each scheduled post belongs to a creatorId
 * (which maps to a specific platform account via user_social_accounts).
 *
 * POST /api/schedule/create     → Create a draft/scheduled post
 * GET  /api/schedule/list       → List posts for a creator (calendar view)
 * PUT  /api/schedule/update/{id} → Edit a scheduled post
 * DELETE /api/schedule/delete/{id} → Remove a scheduled post
 * PUT  /api/schedule/approve/{id} → Approve a post for publishing
 */
@RestController
@RequestMapping("/api/schedule")
@RequiredArgsConstructor
@Slf4j
public class ScheduleController {

    private final ScheduledPostRepository scheduledPostRepository;
    private final CreatorRepository creatorRepository;
    private final CreatorAccessService creatorAccessService;

    /**
     * Create a new draft/scheduled post.
     */
    @PostMapping("/create")
    public ResponseEntity<?> createPost(@RequestBody CreateScheduledPostRequest request, @AuthenticationPrincipal Jwt jwt) {
        if (!creatorAccessService.canAccess(request.creatorId(), jwt)) {
            return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
        }

        Creator creator = creatorRepository.findById(request.creatorId()).orElse(null);
        if (creator == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Creator not found"));
        }

        ScheduledPost post = new ScheduledPost();
        post.setCreator(creator);
        post.setCaption(request.caption());
        post.setHashtags(request.hashtags());
        post.setMediaType(request.mediaType() != null ? request.mediaType() : "IMAGE");
        post.setMediaUrl(request.mediaUrl());
        post.setScheduledFor(request.scheduledFor());
        post.setApprovalStatus(ScheduledPost.ApprovalStatus.PENDING);

        post = scheduledPostRepository.save(post);
        log.info("Created scheduled post {} for creator {}", post.getId(), request.creatorId());

        return ResponseEntity.ok(toResponse(post));
    }

    /**
     * List all scheduled posts for a creator (for calendar view).
     */
    @GetMapping("/list")
    public ResponseEntity<?> listPosts(@RequestParam Long creatorId, @AuthenticationPrincipal Jwt jwt) {
        if (!creatorAccessService.canAccess(creatorId, jwt)) {
            return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
        }

        List<ScheduledPost> posts = scheduledPostRepository.findByCreatorIdOrderByScheduledForDesc(creatorId);

        List<Map<String, Object>> response = posts.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Update a scheduled post (edit caption, time, hashtags, etc.)
     */
    @PutMapping("/update/{id}")
    public ResponseEntity<?> updatePost(@PathVariable Long id, @RequestBody UpdateScheduledPostRequest request) {
        ScheduledPost post = scheduledPostRepository.findById(id).orElse(null);
        if (post == null) {
            return ResponseEntity.notFound().build();
        }

        if (request.caption() != null) post.setCaption(request.caption());
        if (request.hashtags() != null) post.setHashtags(request.hashtags());
        if (request.mediaType() != null) post.setMediaType(request.mediaType());
        if (request.mediaUrl() != null) post.setMediaUrl(request.mediaUrl());
        if (request.scheduledFor() != null) post.setScheduledFor(request.scheduledFor());
        post.setUpdatedAt(LocalDateTime.now());

        post = scheduledPostRepository.save(post);
        return ResponseEntity.ok(toResponse(post));
    }

    /**
     * Delete a scheduled post.
     */
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deletePost(@PathVariable Long id) {
        if (!scheduledPostRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        scheduledPostRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Deleted"));
    }

    /**
     * Approve a post (marks it ready for publishing).
     */
    @PutMapping("/approve/{id}")
    public ResponseEntity<?> approvePost(@PathVariable Long id) {
        ScheduledPost post = scheduledPostRepository.findById(id).orElse(null);
        if (post == null) return ResponseEntity.notFound().build();

        post.setApprovalStatus(ScheduledPost.ApprovalStatus.APPROVED);
        post.setApprovedAt(LocalDateTime.now());
        post.setUpdatedAt(LocalDateTime.now());
        scheduledPostRepository.save(post);

        return ResponseEntity.ok(Map.of("message", "Post approved", "id", id));
    }

    // --- Request DTOs ---

    record CreateScheduledPostRequest(
        Long creatorId,
        String caption,
        String hashtags,
        String mediaType,
        String mediaUrl,
        LocalDateTime scheduledFor
    ) {}

    record UpdateScheduledPostRequest(
        String caption,
        String hashtags,
        String mediaType,
        String mediaUrl,
        LocalDateTime scheduledFor
    ) {}

    // --- Response mapper ---

    private Map<String, Object> toResponse(ScheduledPost post) {
        return Map.of(
            "id", post.getId(),
            "creatorId", post.getCreator().getId(),
            "caption", post.getCaption() != null ? post.getCaption() : "",
            "hashtags", post.getHashtags() != null ? post.getHashtags() : "",
            "mediaType", post.getMediaType(),
            "scheduledFor", post.getScheduledFor().toString(),
            "status", post.getApprovalStatus().name(),
            "createdAt", post.getCreatedAt().toString()
        );
    }
}
