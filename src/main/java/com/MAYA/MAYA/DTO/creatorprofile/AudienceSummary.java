package com.MAYA.MAYA.DTO.creatorprofile;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Aggregated audience snapshot across the creator's connected accounts.
 * Stored as the creator_profile.audience_summary JSONB object. Auto-computed
 * on sync unless the user has edited it.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AudienceSummary {
    private String topAgeRange;              // "18-24"
    private Map<String, Integer> genderSplit; // {"female":62,"male":38}
    private List<String> topCountries;        // ["IN","US"]
    private List<String> topCities;           // ["Mumbai","Delhi"]
    private String note;                      // free-text summary
}
