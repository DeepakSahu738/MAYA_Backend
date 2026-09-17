package com.MAYA.MAYA.DTO.creatorprofile;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Visual/branding assets for the shareable media kit page.
 * Stored as the creator_profile.media_kit_assets JSONB object.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MediaKitAssets {
    private String logoUrl;
    private String coverUrl;
    private String brandColor;   // hex, e.g. "#4f46e5"
}
