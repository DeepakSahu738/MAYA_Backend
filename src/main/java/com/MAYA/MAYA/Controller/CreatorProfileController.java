package com.MAYA.MAYA.Controller;

import com.MAYA.MAYA.DTO.creatorprofile.MediaKitResponse;
import com.MAYA.MAYA.Entity.CreatorProfile;
import com.MAYA.MAYA.Service.CreatorProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Creator CV / Media Kit API — always scoped to the JWT user.
 *
 *  GET   /api/creator-profile        -> full media kit (stored CV + live account stats)
 *  PUT   /api/creator-profile        -> full update (all sent fields marked user-edited)
 *  PATCH /api/creator-profile        -> partial update (only sent fields, marked user-edited)
 */
@RestController
@RequestMapping("/api/creator-profile")
@RequiredArgsConstructor
@Slf4j
public class CreatorProfileController {

    private final CreatorProfileService profileService;

    @GetMapping
    public ResponseEntity<?> getMyProfile(@AuthenticationPrincipal Jwt jwt) {
        Long userId = extractUserId(jwt);
        if (userId == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        MediaKitResponse response = profileService.getMediaKit(userId);
        return ResponseEntity.ok(response);
    }

    /** Full update. Every field present in the body is treated as a user edit. */
    @PutMapping
    public ResponseEntity<?> updateProfile(@RequestBody CreatorProfile incoming,
                                           @RequestParam(required = false) List<String> changedFields,
                                           @AuthenticationPrincipal Jwt jwt) {
        Long userId = extractUserId(jwt);
        if (userId == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        CreatorProfile saved = profileService.updateProfile(userId, incoming, changedFields);
        return ResponseEntity.ok(profileService.getMediaKit(userId));
    }

    /** Partial update — same handler; only non-null fields in the body are applied. */
    @PatchMapping
    public ResponseEntity<?> patchProfile(@RequestBody CreatorProfile incoming,
                                          @RequestParam(required = false) List<String> changedFields,
                                          @AuthenticationPrincipal Jwt jwt) {
        Long userId = extractUserId(jwt);
        if (userId == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        CreatorProfile saved = profileService.updateProfile(userId, incoming, changedFields);
        return ResponseEntity.ok(profileService.getMediaKit(userId));
    }

    /**
     * Update the description of ONE connected account (per-account note).
     * Body: { "description": "my serious fitness page" }  ("" clears it)
     */
    @PatchMapping("/account/{creatorId}/description")
    public ResponseEntity<?> updateAccountDescription(@PathVariable Long creatorId,
                                                      @RequestBody Map<String, String> body,
                                                      @AuthenticationPrincipal Jwt jwt) {
        Long userId = extractUserId(jwt);
        if (userId == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));

        String description = body.get("description");
        boolean ok = profileService.updateAccountDescription(userId, creatorId, description);
        if (!ok) {
            return ResponseEntity.status(404).body(Map.of("error", "Account not found or not owned by you"));
        }
        return ResponseEntity.ok(profileService.getMediaKit(userId));
    }

    /**
     * Safely extract userID from JWT — handles both Integer and Long claim types.
     */
    private Long extractUserId(Jwt jwt) {
        if (jwt == null) return null;
        Object claim = jwt.getClaim("userID");
        if (claim == null) return null;
        if (claim instanceof Long) return (Long) claim;
        if (claim instanceof Integer) return ((Integer) claim).longValue();
        if (claim instanceof Number) return ((Number) claim).longValue();
        try {
            return Long.parseLong(claim.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
