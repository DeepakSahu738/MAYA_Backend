package com.MAYA.MAYA.DTO.creatorprofile;

import com.MAYA.MAYA.Entity.CreatorProfile;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * The full media-kit payload returned to the frontend: the stored person-level
 * CV (CreatorProfile) plus a LIVE list of the user's connected accounts and their
 * current stats (computed from Creator rows, not stored). This means the media kit
 * always reflects the current set of connected accounts.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MediaKitResponse {

    private CreatorProfile profile;
    private List<ConnectedAccountSummary> accounts;
    private Long totalFollowers; // summed across accounts (best-effort)

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConnectedAccountSummary {
        private Long creatorId;
        private String platform;
        private String handle;
        private Integer followers;      // follower/subscriber count
        private Double engagementRate;  // live, may be null
        private Boolean verified;
        private String profilePictureUrl;
        private String status;          // CONNECTED / DISCONNECTED
        private String description;     // user-provided per-account note, nullable
    }
}
