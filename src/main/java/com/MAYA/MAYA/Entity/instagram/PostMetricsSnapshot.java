package com.MAYA.MAYA.Entity.instagram;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "post_metrics_snapshots", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"post_id", "snapshot_date"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostMetricsSnapshot {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;
    
    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;
    
    @Column(name = "likes")
    private Long likes;
    
    @Column(name = "comments")
    private Long comments;
    
    @Column(name = "saves")
    private Long saves;
    
    @Column(name = "shares")
    private Long shares;
    
    @Column(name = "reposts")
    private Long reposts;
    
    @Column(name = "reach")
    private Long reach;
    
    @Column(name = "impressions")
    private Long impressions;
    
    @Column(name = "plays")
    private Long plays;
    
    @Column(name = "view_count")
    private Long viewCount;
    
    @Column(name = "engagement_rate")
    private Double engagementRate;
    
    @Column(name = "save_rate")
    private Double saveRate;
    
    @Column(name = "share_rate")
    private Double shareRate;
    
    @Column(name = "comment_rate")
    private Double commentRate;
    
    @Column(name = "like_to_comment_ratio")
    private Double likeToCommentRatio;
    
    @Column(name = "play_through_rate")
    private Double playThroughRate;
    
    @Column(name = "reach_efficiency")
    private Double reachEfficiency;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
