package com.MAYA.MAYA.Entity.instagram;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "scheduled_posts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScheduledPost {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "scheduled_post_seq")
    @SequenceGenerator(name = "scheduled_post_seq", sequenceName = "scheduled_posts_id_seq", allocationSize = 50)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private Creator creator;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String caption;
    
    @Column(columnDefinition = "TEXT")
    private String hashtags;
    
    @Column(nullable = false)
    private String mediaType;
    
    private String mediaUrl;
    
    @Column(nullable = false)
    private LocalDateTime scheduledFor;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApprovalStatus approvalStatus = ApprovalStatus.PENDING;
    
    private String rejectionReason;
    
    private LocalDateTime approvedAt;
    
    private LocalDateTime publishedAt;
    
    private String publishedInstagramId;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    public enum ApprovalStatus {
        PENDING,
        APPROVED,
        REJECTED,
        PUBLISHED,
        FAILED
    }
}
