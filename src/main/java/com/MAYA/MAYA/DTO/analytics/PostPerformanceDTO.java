package com.MAYA.MAYA.DTO.analytics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostPerformanceDTO {
    private Long postId;
    private String instagramId;
    private String caption;
    private String mediaType;
    private Double engagementRate;
    private Integer likes;
    private Integer comments;
    private Integer saves;
    private LocalDateTime postedAt;
}
