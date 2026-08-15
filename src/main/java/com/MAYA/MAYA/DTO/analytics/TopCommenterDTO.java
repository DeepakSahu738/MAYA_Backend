package com.MAYA.MAYA.DTO.analytics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TopCommenterDTO {
    private String username;
    private Integer commentCount;
    private Double avgSentimentScore;
}
