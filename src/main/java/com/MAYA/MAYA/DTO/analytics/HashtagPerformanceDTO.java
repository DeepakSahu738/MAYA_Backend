package com.MAYA.MAYA.DTO.analytics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HashtagPerformanceDTO {
    private String hashtag;
    private Integer usageCount;
    private Double avgEngagementRate;
    private Double avgReach;
    private Integer totalLikes;
}
