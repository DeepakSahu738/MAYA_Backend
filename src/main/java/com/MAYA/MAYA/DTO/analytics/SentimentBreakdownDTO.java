package com.MAYA.MAYA.DTO.analytics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SentimentBreakdownDTO {
    private Integer positiveCount;
    private Integer neutralCount;
    private Integer negativeCount;
    private Double positivePercentage;
    private Double neutralPercentage;
    private Double negativePercentage;
}
