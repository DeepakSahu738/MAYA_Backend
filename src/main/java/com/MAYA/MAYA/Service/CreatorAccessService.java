package com.MAYA.MAYA.Service;

import com.MAYA.MAYA.Entity.instagram.Creator;
import com.MAYA.MAYA.Repository.UserSocialAccountRepository;
import com.MAYA.MAYA.Repository.instagram.CreatorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * Checks if a user can access a specific creator's data.
 *
 * Rules:
 * - Demo creators (identified by username) → always public, no auth needed
 * - Real creators → requires JWT + user must own that creatorId
 */
@Service
@RequiredArgsConstructor
public class CreatorAccessService {

    private static final Set<String> DEMO_USERNAMES = Set.of(
        "fitlife_by_meera"
    );

    private final UserSocialAccountRepository socialAccountRepository;
    private final CreatorRepository creatorRepository;

    /**
     * Check if the given JWT user can access this creatorId.
     */
    public boolean canAccess(Long creatorId, Jwt jwt) {
        // Demo creators are always public
        if (isDemoCreator(creatorId)) {
            return true;
        }

        // Real creators require a valid JWT
        if (jwt == null) {
            return false;
        }

        // Verify the user owns this creator
        Long userId = extractUserId(jwt);
        if (userId == null) {
            return false;
        }

        return socialAccountRepository.findByUserId(userId).stream()
            .anyMatch(account -> account.getCreator() != null
                && account.getCreator().getId().equals(creatorId));
    }

    /**
     * Check if a creatorId belongs to a demo creator (by username).
     */
    public boolean isDemoCreator(Long creatorId) {
        return creatorRepository.findById(creatorId)
            .map(creator -> DEMO_USERNAMES.contains(creator.getUsername()))
            .orElse(false);
    }

    private Long extractUserId(Jwt jwt) {
        Object claim = jwt.getClaim("userID");
        if (claim instanceof Long) return (Long) claim;
        if (claim instanceof Integer) return ((Integer) claim).longValue();
        if (claim instanceof Number) return ((Number) claim).longValue();
        return null;
    }
}
