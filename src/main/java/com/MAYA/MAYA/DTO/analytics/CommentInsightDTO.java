package com.MAYA.MAYA.DTO.analytics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentInsightDTO {
    private String word;
    private Integer frequency;
    private String sentiment;
}
