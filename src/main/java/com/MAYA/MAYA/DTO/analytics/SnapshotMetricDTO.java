package com.MAYA.MAYA.DTO.analytics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SnapshotMetricDTO {
    private String metricName;
    private Object currentValue;
    private LocalDateTime calculatedAt;
}
