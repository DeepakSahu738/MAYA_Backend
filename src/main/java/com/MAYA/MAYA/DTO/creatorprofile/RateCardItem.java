package com.MAYA.MAYA.DTO.creatorprofile;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One priced deliverable on the creator's rate card.
 * Stored inside creator_profile.rate_card JSONB array.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RateCardItem {
    private String platform;     // INSTAGRAM, YOUTUBE, FACEBOOK, ...
    private String deliverable;  // "Reel", "Story", "Dedicated Video"
    private Double price;
    private String currency;     // "INR", "USD"
    private String notes;        // "1 reel + 2 stories bundle"
}
