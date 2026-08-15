package com.MAYA.MAYA.Entity.instagram;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostMetrics {
    
    private Integer likes = 0;
    
    private Integer comments = 0;
    
    // Nullable — null means data not returned, NOT zero saves
    private Integer saves;
    
    // Nullable — null means data not returned, NOT zero shares
    private Integer shares;
    
    private Integer reposts;
    
    // Nullable — null means data not returned (79% available)
    private Integer reach;
    
    // Nullable — null means data not returned (79% available)
    private Integer impressions;
    
    // Nullable — VIDEO only, always null for IMAGE posts
    private Integer plays;
    
    private Double engagementRate;
    
    private Double saveRate;
    
    private Double shareRate;
}
