package com.MAYA.MAYA.DTO.creatorprofile;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A past brand collaboration. Stored inside creator_profile.collaborations JSONB array.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Collaboration {
    private String brand;      // "Nike"
    private String campaign;   // "Summer Fit"
    private Integer year;      // 2025
    private String link;       // optional URL to the post/campaign
    private String logoUrl;    // optional brand logo
}
