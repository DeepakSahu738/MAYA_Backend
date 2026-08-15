package com.MAYA.MAYA.Controller;

import com.MAYA.MAYA.Entity.UserSocialAccount;
import com.MAYA.MAYA.Entity.instagram.Creator;
import com.MAYA.MAYA.Repository.UserSocialAccountRepository;
import com.MAYA.MAYA.Repository.instagram.CommentRepository;
import com.MAYA.MAYA.Repository.instagram.CreatorRepository;
import com.MAYA.MAYA.Repository.instagram.HashtagPerformanceRepository;
import com.MAYA.MAYA.Repository.instagram.PostRepository;
import com.MAYA.MAYA.Repository.instagram.ScheduledPostRepository;
import com.MAYA.MAYA.Repository.instagram.TopCommenterRepository;
import com.MAYA.MAYA.Repository.instagram.WeeklyReportRepository;
import com.MAYA.MAYA.Service.phyllo.PhylloService;
import com.MAYA.MAYA.Service.phyllo.PhylloSyncService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Phyllo integration endpoints.
 *
 * Flow:
 * 1. Frontend calls POST /api/phyllo/connect   → creates Phyllo user + returns SDK token
 * 2. Frontend opens Phyllo Connect SDK with the token
 * 3. User connects their account in the SDK
 * 4. Frontend calls POST /api/phyllo/account-connected  → stores the link + triggers sync
 * 5. Frontend calls GET  /api/phyllo/accounts           → lists connected accounts
 */
@RestController
@RequestMapping("/api/phyllo")
@RequiredArgsConstructor
@Slf4j
public class PhylloController {

    private final PhylloService phylloService;
    private final PhylloSyncService phylloSyncService;
    private final UserSocialAccountRepository socialAccountRepository;
    private final CreatorRepository creatorRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final ScheduledPostRepository scheduledPostRepository;
    private final WeeklyReportRepository weeklyReportRepository;
    private final HashtagPerformanceRepository hashtagPerformanceRepository;
    private final TopCommenterRepository topCommenterRepository;

    /**
     * Step 1: Initiate Phyllo Connect.
     * Creates a Phyllo user (or reuses existing) and returns an SDK token.
     *
     * POST /api/phyllo/connect
     * Body: { "userId": 1, "userName": "Deepa" }
     * Response: { "phylloUserId": "...", "sdkToken": "...", "environment": "staging" }
     */
    @PostMapping("/connect")
    public ResponseEntity<?> initiateConnect(@RequestBody ConnectRequest request, @AuthenticationPrincipal Jwt jwt) {
        Long jwtUserId = extractUserId(jwt);

        // Verify the JWT user matches the request
        if (!jwtUserId.equals(request.userId())) {
            return ResponseEntity.status(403).body(Map.of("error", "User ID mismatch — you can only connect your own account"));
        }

        log.info("Initiating Phyllo connect for Maya user: {}", request.userId());

        try {
            // Check if we already have a Phyllo user for this Maya user
            Optional<UserSocialAccount> existing = socialAccountRepository.findFirstByUserId(request.userId());
            String phylloUserId;

            if (existing.isPresent()) {
                // Reuse existing Phyllo user ID
                phylloUserId = existing.get().getPhylloUserId();
                log.info("Reusing existing Phyllo user: {} for Maya user: {}", phylloUserId, request.userId());
            } else {
                // Create new Phyllo user
                phylloUserId = phylloService.createPhylloUser(request.userId(), request.userName());
            }

            // Generate fresh SDK token
            String sdkToken = phylloService.generateSdkToken(phylloUserId);

            return ResponseEntity.ok(Map.of(
                "phylloUserId", phylloUserId,
                "sdkToken", sdkToken,
                "environment", "staging"
            ));
        } catch (Exception e) {
            log.error("Phyllo connect failed for user {}: {}", request.userId(), e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                "error", "Phyllo connection failed",
                "detail", e.getMessage() != null ? e.getMessage() : "Unknown error"
            ));
        }
    }

    /**
     * Step 2: Called by frontend after Phyllo SDK fires "accountConnected" event.
     * Stores the link and fetches account details from Phyllo.
     *
     * POST /api/phyllo/account-connected
     * Body: { "userId": 1, "phylloUserId": "...", "accountId": "...", "workPlatformId": "..." }
     * Response: { "message": "...", "creatorId": 5, "platform": "Instagram", "username": "..." }
     */
    @PostMapping("/account-connected")
    public ResponseEntity<?> accountConnected(@RequestBody AccountConnectedRequest request, @AuthenticationPrincipal Jwt jwt) {
        Long jwtUserId = extractUserId(jwt);

        if (!jwtUserId.equals(request.userId())) {
            return ResponseEntity.status(403).body(Map.of("error", "User ID mismatch"));
        }

        log.info("Account connected callback — userId: {}, accountId: {}", request.userId(), request.accountId());

        // Check if this Phyllo account is already linked to ANY user
        Optional<UserSocialAccount> existing = socialAccountRepository.findByPhylloAccountId(request.accountId());
        if (existing.isPresent()) {
            UserSocialAccount existingAccount = existing.get();

            // Case 1: Same user reconnecting their own account → allow (re-activate)
            if (existingAccount.getUserId().equals(request.userId())) {
                if ("DISCONNECTED".equals(existingAccount.getStatus())) {
                    existingAccount.setStatus("CONNECTED");
                    existingAccount.setConnectedAt(LocalDateTime.now());
                    if (existingAccount.getCreator() != null) {
                        existingAccount.getCreator().setIsActive(true);
                        creatorRepository.save(existingAccount.getCreator());
                    }
                    socialAccountRepository.save(existingAccount);
                    phylloSyncService.syncAccount(request.accountId(), existingAccount.getCreator().getId());
                    log.info("Re-activated account {} for user {}", request.accountId(), request.userId());
                }
                return ResponseEntity.ok(Map.of(
                    "message", "Account connected",
                    "creatorId", existingAccount.getCreator() != null ? existingAccount.getCreator().getId() : 0,
                    "platform", existingAccount.getPlatform(),
                    "username", existingAccount.getPlatformUsername() != null ? existingAccount.getPlatformUsername() : ""
                ));
            }

            // Case 2: Account is CONNECTED to a different user → block
            if ("CONNECTED".equals(existingAccount.getStatus())) {
                return ResponseEntity.status(409).body(Map.of(
                    "error", "Account already in use",
                    "message", "This social media account is already connected to another Maya user. The other user must disconnect it first."
                ));
            }

            // Case 3: Account was DISCONNECTED by another user → allow new user to claim it
            // Transfer ownership: update the existing record to the new user
            existingAccount.setUserId(request.userId());
            existingAccount.setPhylloUserId(request.phylloUserId());
            existingAccount.setStatus("CONNECTED");
            existingAccount.setConnectedAt(LocalDateTime.now());
            if (existingAccount.getCreator() != null) {
                existingAccount.getCreator().setIsActive(true);
                creatorRepository.save(existingAccount.getCreator());
            }
            socialAccountRepository.save(existingAccount);
            phylloSyncService.syncAccount(request.accountId(), existingAccount.getCreator().getId());
            log.info("Transferred account {} from previous user to user {}", request.accountId(), request.userId());

            return ResponseEntity.ok(Map.of(
                "message", "Account connected successfully",
                "creatorId", existingAccount.getCreator() != null ? existingAccount.getCreator().getId() : 0,
                "platform", existingAccount.getPlatform(),
                "username", existingAccount.getPlatformUsername() != null ? existingAccount.getPlatformUsername() : ""
            ));
        }

        // Fetch account details from Phyllo
        JsonNode accountDetails = phylloService.getAccountDetails(request.accountId());
        String platform = extractPlatform(accountDetails);
        String username = extractUsername(accountDetails);

        // Create a Creator entity for this connected account
        Creator creator = new Creator();
        creator.setInstagramId(request.accountId());
        creator.setUsername(username != null ? username : "unknown");
        creator.setNiche("Not set");
        creator.setConnectedAt(LocalDateTime.now());
        creator.setLastSyncedAt(LocalDateTime.now());
        creator.setIsActive(true);
        creator = creatorRepository.save(creator);

        // Store the link
        UserSocialAccount socialAccount = UserSocialAccount.builder()
            .userId(request.userId())
            .creator(creator)
            .phylloUserId(request.phylloUserId())
            .phylloAccountId(request.accountId())
            .platform(platform)
            .platformUsername(username)
            .status("CONNECTED")
            .connectedAt(LocalDateTime.now())
            .build();
        socialAccountRepository.save(socialAccount);

        // Trigger async data sync (posts, comments, profile)
        phylloSyncService.syncAccount(request.accountId(), creator.getId());

        log.info("Linked Maya user {} → {} account @{} (creator_id: {})",
            request.userId(), platform, username, creator.getId());

        return ResponseEntity.ok(Map.of(
            "message", "Account connected successfully",
            "creatorId", creator.getId(),
            "platform", platform,
            "username", username != null ? username : ""
        ));
    }

    /**
     * Get all connected social accounts for a Maya user.
     *
     * GET /api/phyllo/accounts?userId=1
     * Response: [ { "id": 1, "creatorId": 5, "platform": "INSTAGRAM", "username": "fitlife_by_meera", "status": "CONNECTED" } ]
     */
    @GetMapping("/accounts")
    public ResponseEntity<?> getConnectedAccounts(@RequestParam Long userId, @AuthenticationPrincipal Jwt jwt) {
        Long jwtUserId = extractUserId(jwt);

        if (!jwtUserId.equals(userId)) {
            return ResponseEntity.status(403).body(Map.of("error", "User ID mismatch — you can only view your own accounts"));
        }

        List<UserSocialAccount> accounts = socialAccountRepository.findByUserId(userId);

        List<Map<String, Object>> response = accounts.stream()
            .map(a -> {
                Map<String, Object> map = new java.util.LinkedHashMap<>();
                map.put("id", a.getId());
                map.put("creatorId", a.getCreator() != null ? a.getCreator().getId() : 0);
                map.put("platform", a.getPlatform());
                map.put("username", a.getPlatformUsername() != null ? a.getPlatformUsername() : "");
                map.put("status", a.getStatus());
                map.put("connectedAt", a.getConnectedAt() != null ? a.getConnectedAt().toString() : "");
                // Profile data from creator entity
                if (a.getCreator() != null) {
                    map.put("followerCount", a.getCreator().getFollowerCount());
                    map.put("followingCount", a.getCreator().getFollowingCount());
                    map.put("mediaCount", a.getCreator().getMediaCount());
                    map.put("profilePictureUrl", a.getCreator().getProfilePictureUrl());
                    map.put("niche", a.getCreator().getNiche());
                    map.put("isVerified", a.getCreator().getIsVerified());
                }
                return map;
            })
            .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Disconnect a social account from the Maya user.
     * Doesn't delete the data — just marks as DISCONNECTED and deactivates the creator.
     * User can reconnect later.
     *
     * DELETE /api/phyllo/disconnect/{creatorId}
     * Response: { "message": "Account disconnected", "platform": "INSTAGRAM" }
     */
    @DeleteMapping("/disconnect/{creatorId}")
    public ResponseEntity<?> disconnectAccount(@PathVariable Long creatorId, @AuthenticationPrincipal Jwt jwt) {
        Long jwtUserId = extractUserId(jwt);

        // Find the social account link by creatorId and userId
        List<UserSocialAccount> userAccounts = socialAccountRepository.findByUserId(jwtUserId);
        UserSocialAccount account = userAccounts.stream()
            .filter(a -> a.getCreator() != null && a.getCreator().getId().equals(creatorId))
            .findFirst()
            .orElse(null);

        if (account == null) {
            return ResponseEntity.status(404).body(Map.of("error", "Account not found or you don't own it"));
        }

        // Mark as disconnected (don't delete — preserve data history)
        account.setStatus("DISCONNECTED");
        socialAccountRepository.save(account);

        // Deactivate the creator so it doesn't show in active queries
        if (account.getCreator() != null) {
            Creator creator = account.getCreator();
            creator.setIsActive(false);
            creatorRepository.save(creator);
        }

        log.info("Disconnected creator {} ({}) for user {}", creatorId, account.getPlatform(), jwtUserId);

        return ResponseEntity.ok(Map.of(
            "message", "Account disconnected successfully",
            "platform", account.getPlatform(),
            "username", account.getPlatformUsername() != null ? account.getPlatformUsername() : ""
        ));
    }

    /**
     * PERMANENTLY DELETE a social account and ALL its data from Maya.
     * This is irreversible — deletes posts, comments, analytics, reports, scheduled posts, and the creator record.
     * The user_social_accounts link is also removed.
     *
     * DELETE /api/phyllo/delete-account/{creatorId}
     * Response: { "message": "...", "deletedData": { "posts": 49, "comments": 120, ... } }
     */
    @DeleteMapping("/delete-account/{creatorId}")
    @Transactional
    public ResponseEntity<?> deleteAccountPermanently(@PathVariable Long creatorId, @AuthenticationPrincipal Jwt jwt) {
        Long jwtUserId = extractUserId(jwt);

        // Find the social account link
        List<UserSocialAccount> userAccounts = socialAccountRepository.findByUserId(jwtUserId);
        UserSocialAccount account = userAccounts.stream()
            .filter(a -> a.getCreator() != null && a.getCreator().getId().equals(creatorId))
            .findFirst()
            .orElse(null);

        if (account == null) {
            return ResponseEntity.status(404).body(Map.of("error", "Account not found or you don't own it"));
        }

        String platform = account.getPlatform();
        String username = account.getPlatformUsername();

        // Count data before deletion (for response)
        long postsCount = postRepository.findByCreatorIdOrderByPostedAtDesc(creatorId).size();
        long commentsCount = commentRepository.findByCreatorIdOrderByCommentedAtDesc(creatorId).size();

        // Delete order matters (FK constraints — delete children first)
        commentRepository.deleteByCreatorId(creatorId);
        scheduledPostRepository.deleteByCreatorId(creatorId);
        weeklyReportRepository.deleteByCreatorId(creatorId);
        hashtagPerformanceRepository.deleteByCreatorId(creatorId);
        topCommenterRepository.deleteByCreatorId(creatorId);
        postRepository.deleteByCreatorId(creatorId);

        // Delete the social account link
        socialAccountRepository.delete(account);

        // Delete the creator entity
        creatorRepository.deleteById(creatorId);

        log.info("PERMANENTLY DELETED account {} (@{}) for user {} — {} posts, {} comments erased",
            creatorId, username, jwtUserId, postsCount, commentsCount);

        return ResponseEntity.ok(Map.of(
            "message", "Account and all data permanently deleted",
            "platform", platform != null ? platform : "",
            "username", username != null ? username : "",
            "deletedData", Map.of(
                "posts", postsCount,
                "comments", commentsCount
            )
        ));
    }

    /**
     * Check sync status for a connected account.
     * Frontend polls this after account connection to know when data is ready.
     *
     * GET /api/phyllo/sync-status/{creatorId}
     * Response: { "status": "SYNCING" | "READY" | "NOT_CONNECTED", "postsCount": 0, "commentsCount": 0, "message": "..." }
     */
    @GetMapping("/sync-status/{creatorId}")
    public ResponseEntity<?> getSyncStatus(@PathVariable Long creatorId, @AuthenticationPrincipal Jwt jwt) {
        Long jwtUserId = extractUserId(jwt);

        // Verify ownership
        Creator creator = creatorRepository.findById(creatorId).orElse(null);
        if (creator == null) {
            return ResponseEntity.ok(Map.of(
                "status", "NOT_CONNECTED",
                "message", "Creator not found"
            ));
        }

        // Check if this user owns this creator
        boolean owns = socialAccountRepository.findByUserId(jwtUserId).stream()
            .anyMatch(a -> a.getCreator() != null && a.getCreator().getId().equals(creatorId));
        if (!owns) {
            return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
        }

        // Count data to determine sync status
        long postsCount = postRepository.findByCreatorIdOrderByPostedAtDesc(creatorId).size();
        long commentsCount = commentRepository.findByCreatorIdOrderByCommentedAtDesc(creatorId).size();

        String status;
        String message;

        if (postsCount == 0) {
            status = "SYNCING";
            message = "Fetching your posts from Instagram... This usually takes 30-60 seconds.";
        } else if (commentsCount == 0) {
            status = "SYNCING";
            message = "Posts loaded! Now fetching your comments... Almost there.";
        } else {
            status = "READY";
            message = String.format("All synced! %d posts and %d comments loaded.", postsCount, commentsCount);
        }

        return ResponseEntity.ok(Map.of(
            "status", status,
            "postsCount", postsCount,
            "commentsCount", commentsCount,
            "message", message
        ));
    }

    /**
     * Reconnect a previously disconnected social account (without going through Phyllo SDK again).
     * Only works if the account was soft-disconnected from Maya but still exists in Phyllo.
     *
     * PUT /api/phyllo/reconnect/{creatorId}
     * Response: { "message": "Account reconnected", "creatorId": 9, "platform": "INSTAGRAM" }
     */
    @PutMapping("/reconnect/{creatorId}")
    public ResponseEntity<?> reconnectAccount(@PathVariable Long creatorId, @AuthenticationPrincipal Jwt jwt) {
        Long jwtUserId = extractUserId(jwt);

        // Find the disconnected account for this user + creatorId
        List<UserSocialAccount> userAccounts = socialAccountRepository.findByUserId(jwtUserId);
        UserSocialAccount account = userAccounts.stream()
            .filter(a -> a.getCreator() != null && a.getCreator().getId().equals(creatorId))
            .findFirst()
            .orElse(null);

        if (account == null) {
            return ResponseEntity.status(404).body(Map.of("error", "No account found with this creator ID for your user"));
        }

        if ("CONNECTED".equals(account.getStatus())) {
            return ResponseEntity.ok(Map.of(
                "message", "Account is already connected",
                "creatorId", creatorId,
                "platform", account.getPlatform()
            ));
        }

        // Re-activate
        account.setStatus("CONNECTED");
        account.setConnectedAt(LocalDateTime.now());
        socialAccountRepository.save(account);

        // Re-activate the creator
        if (account.getCreator() != null) {
            Creator creator = account.getCreator();
            creator.setIsActive(true);
            creatorRepository.save(creator);
        }

        // Trigger a fresh sync to pick up any new data since disconnect
        phylloSyncService.syncAccount(account.getPhylloAccountId(), creatorId);

        log.info("Reconnected creator {} ({}) for user {}", creatorId, account.getPlatform(), jwtUserId);

        return ResponseEntity.ok(Map.of(
            "message", "Account reconnected successfully",
            "creatorId", creatorId,
            "platform", account.getPlatform(),
            "username", account.getPlatformUsername() != null ? account.getPlatformUsername() : ""
        ));
    }

    /**
     * Get disconnected accounts that can be reconnected without going through Phyllo again.
     *
     * GET /api/phyllo/disconnected?userId={id}
     * Response: [ { "creatorId": 9, "platform": "INSTAGRAM", "username": "...", "disconnectedAt": "..." } ]
     */
    @GetMapping("/disconnected")
    public ResponseEntity<?> getDisconnectedAccounts(@RequestParam Long userId, @AuthenticationPrincipal Jwt jwt) {
        Long jwtUserId = extractUserId(jwt);
        if (!jwtUserId.equals(userId)) {
            return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
        }

        List<UserSocialAccount> disconnected = socialAccountRepository.findByUserIdAndStatus(userId, "DISCONNECTED");

        List<Map<String, Object>> response = disconnected.stream()
            .map(a -> Map.<String, Object>of(
                "creatorId", a.getCreator() != null ? a.getCreator().getId() : 0,
                "platform", a.getPlatform(),
                "username", a.getPlatformUsername() != null ? a.getPlatformUsername() : "",
                "disconnectedAt", a.getConnectedAt() != null ? a.getConnectedAt().toString() : ""
            ))
            .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    // --- Request DTOs ---

    record ConnectRequest(Long userId, String userName) {}
    record AccountConnectedRequest(Long userId, String phylloUserId, String accountId, String workPlatformId) {}

    // --- Helpers ---

    private String extractPlatform(JsonNode accountDetails) {
        if (accountDetails.has("work_platform") && accountDetails.get("work_platform").has("name")) {
            return accountDetails.get("work_platform").get("name").asText().toUpperCase();
        }
        return "UNKNOWN";
    }

    private String extractUsername(JsonNode accountDetails) {
        if (accountDetails.has("username")) {
            return accountDetails.get("username").asText();
        }
        if (accountDetails.has("platform_username")) {
            return accountDetails.get("platform_username").asText();
        }
        return null;
    }

    /**
     * Safely extract userID from JWT — handles both Integer and Long claim types.
     */
    private Long extractUserId(Jwt jwt) {
        Object claim = jwt.getClaim("userID");
        if (claim instanceof Long) return (Long) claim;
        if (claim instanceof Integer) return ((Integer) claim).longValue();
        if (claim instanceof Number) return ((Number) claim).longValue();
        throw new RuntimeException("Invalid userID claim in JWT");
    }
}
