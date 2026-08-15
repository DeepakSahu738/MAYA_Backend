package com.MAYA.MAYA.DTO.analytics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimeSeriesMetricDTO {
    private String metricName;
    private Double currentValue;
    private Double deltaVsLastWeek;
    private String unit;
    private LocalDateTime calculatedAt;
}
