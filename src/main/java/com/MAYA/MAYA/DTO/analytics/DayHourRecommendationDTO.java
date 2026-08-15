package com.MAYA.MAYA.DTO.analytics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DayHourRecommendationDTO {
    private String bestDay;
    private Integer bestHour;
    private Double avgEngagementRate;
    private String timezone;
}
