package com.MAYA.MAYA.DTO.creatorprofile;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A service the creator offers (e.g. UGC video, brand shoutout).
 * Stored inside creator_profile.services JSONB array.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceOffering {
    private String title;         // "UGC Video"
    private String description;
    private Double startingPrice;
    private String currency;
}
