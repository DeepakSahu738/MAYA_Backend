package com.MAYA.MAYA.DTO.analytics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountHealthScoreDTO {
    private Integer score;
    private String grade;
    private Map<String, Double> componentScores;
    private List<String> strengths;
    private List<String> improvements;
    private LocalDateTime calculatedAt;
}
