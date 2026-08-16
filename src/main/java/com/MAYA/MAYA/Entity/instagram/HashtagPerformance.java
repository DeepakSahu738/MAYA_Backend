package com.MAYA.MAYA.Entity.instagram;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "hashtag_performance",
       uniqueConstraints = @UniqueConstraint(columnNames = {"creator_id", "hashtag"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HashtagPerformance {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hashtag_seq")
    @SequenceGenerator(name = "hashtag_seq", sequenceName = "hashtag_performance_id_seq", allocationSize = 50)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private Creator creator;
    
    @Column(name = "hashtag", nullable = false)
    private String hashtag;
    
    @Column(name = "usage_count")
    private Integer usageCount = 0;
    
    @Column(name = "avg_reach_when_used")
    private Double avgReachWhenUsed;
    
    @Column(name = "avg_engagement_when_used")
    private Double avgEngagementWhenUsed;
    
    @Column(name = "avg_save_rate_when_used")
    private Double avgSaveRateWhenUsed;
    
    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;
    
    @Column(name = "performance_score")
    private Double performanceScore;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
}
