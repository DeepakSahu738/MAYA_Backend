package com.MAYA.MAYA.Entity.instagram;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "creator_insights_snapshots",
       uniqueConstraints = @UniqueConstraint(columnNames = {"creator_id", "snapshot_date"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatorInsightsSnapshot {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private Creator creator;
    
    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;
    
    @Column(name = "followers_count")
    private Long followersCount;
    
    @Column(name = "following_count")
    private Long followingCount;
    
    @Column(name = "media_count")
    private Integer mediaCount;
    
    @Column(name = "profile_views_count")
    private Long profileViewsCount;
    
    @Column(name = "website_clicks")
    private Long websiteClicks;
    
    @Column(name = "email_contacts")
    private Long emailContacts;
    
    @Column(name = "reach")
    private Long reach;
    
    @Column(name = "impressions")
    private Long impressions;
    
    @Column(name = "profile_view_to_follow_rate")
    private Double profileViewToFollowRate;
    
    @Column(name = "reach_efficiency")
    private Double reachEfficiency;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
