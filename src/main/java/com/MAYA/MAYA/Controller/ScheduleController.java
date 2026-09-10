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

        if (request.scheduledFor() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "scheduledFor is required"));
        }
        if (request.caption() == null || request.caption().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "caption/title is required"));
        }

        // Resolve item type (defaults to POST for backward compatibility)
        ScheduledPost.ItemType itemType = parseItemType(request.itemType());

        ScheduledPost post = new ScheduledPost();
        post.setCreator(creator);
        post.setItemType(itemType);
        post.setCaption(request.caption());
        post.setScheduledFor(request.scheduledFor());

        if (itemType == ScheduledPost.ItemType.TASK) {
            // Tasks: no media/hashtags; use the work lifecycle. mediaType kept as
            // a non-null placeholder ("NONE") since the column is NOT NULL.
            post.setMediaType("NONE");
            post.setHashtags(null);
            post.setMediaUrl(null);
            post.setTaskStatus(parseTaskStatus(request.taskStatus(), ScheduledPost.TaskStatus.TODO));
            // approvalStatus stays at its PENDING default but is unused for tasks
        } else {
            // Posts: normal content fields + approval lifecycle
            post.setHashtags(request.hashtags());
            post.setMediaType(request.mediaType() != null ? request.mediaType() : "IMAGE");
            post.setMediaUrl(request.mediaUrl());
            post.setApprovalStatus(ScheduledPost.ApprovalStatus.PENDING);
        }

        post = scheduledPostRepository.save(post);
        log.info("Created {} {} for creator {}", itemType, post.getId(), request.creatorId());

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

        // Task status change (e.g. dragging a task between TODO/IN_PROGRESS/DONE columns).
        // Only meaningful for TASK items; ignored for posts.
        if (request.taskStatus() != null && post.getItemType() == ScheduledPost.ItemType.TASK) {
            post.setTaskStatus(parseTaskStatus(request.taskStatus(), post.getTaskStatus()));
        }

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

    /**
     * Mark an approved post as published (manual publish confirmation by user).
     */
    @PutMapping("/publish/{id}")
    public ResponseEntity<?> publishPost(@PathVariable Long id) {
        ScheduledPost post = scheduledPostRepository.findById(id).orElse(null);
        if (post == null) return ResponseEntity.notFound().build();

        if (post.getApprovalStatus() != ScheduledPost.ApprovalStatus.APPROVED) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Only approved posts can be marked as published",
                "currentStatus", post.getApprovalStatus().name()
            ));
        }

        post.setApprovalStatus(ScheduledPost.ApprovalStatus.PUBLISHED);
        post.setPublishedAt(LocalDateTime.now());
        post.setUpdatedAt(LocalDateTime.now());
        scheduledPostRepository.save(post);

        log.info("Post {} marked as published for creator {}", id, post.getCreator().getId());

        return ResponseEntity.ok(Map.of("message", "Post marked as published", "id", id, "publishedAt", post.getPublishedAt().toString()));
    }

    // --- Request DTOs ---

    record CreateScheduledPostRequest(
        Long creatorId,
        String caption,
        String hashtags,
        String mediaType,
        String mediaUrl,
        LocalDateTime scheduledFor,
        String itemType,     // "POST" (default) or "TASK"
        String taskStatus    // "TODO" | "IN_PROGRESS" | "DONE" (tasks only, defaults TODO)
    ) {}

    record UpdateScheduledPostRequest(
        String caption,
        String hashtags,
        String mediaType,
        String mediaUrl,
        LocalDateTime scheduledFor,
        String taskStatus    // move a task between board columns
    ) {}

    // --- Helpers ---

    private ScheduledPost.ItemType parseItemType(String raw) {
        if (raw == null || raw.isBlank()) return ScheduledPost.ItemType.POST;
        try {
            return ScheduledPost.ItemType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ScheduledPost.ItemType.POST;
        }
    }

    private ScheduledPost.TaskStatus parseTaskStatus(String raw, ScheduledPost.TaskStatus fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        try {
            return ScheduledPost.TaskStatus.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    // --- Response mapper ---
    // Uses LinkedHashMap (not Map.of) because Map.of throws on null values and is
    // capped at 10 entries — several of these fields can be null (e.g. taskStatus
    // for posts, approvalStatus semantics for tasks).
    private Map<String, Object> toResponse(ScheduledPost post) {
        Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("id", post.getId());
        m.put("creatorId", post.getCreator().getId());
        m.put("itemType", post.getItemType() != null ? post.getItemType().name() : "POST");
        m.put("caption", post.getCaption() != null ? post.getCaption() : "");
        m.put("hashtags", post.getHashtags() != null ? post.getHashtags() : "");
        m.put("mediaType", post.getMediaType());
        m.put("mediaUrl", post.getMediaUrl());
        m.put("scheduledFor", post.getScheduledFor() != null ? post.getScheduledFor().toString() : null);
        // Post lifecycle status
        m.put("status", post.getApprovalStatus() != null ? post.getApprovalStatus().name() : null);
        // Task lifecycle status (null for posts)
        m.put("taskStatus", post.getTaskStatus() != null ? post.getTaskStatus().name() : null);
        m.put("createdAt", post.getCreatedAt() != null ? post.getCreatedAt().toString() : null);
        return m;
    }
}
