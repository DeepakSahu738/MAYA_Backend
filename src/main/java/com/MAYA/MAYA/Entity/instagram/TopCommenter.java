package com.MAYA.MAYA.Entity.instagram;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "top_commenters",
       uniqueConstraints = @UniqueConstraint(columnNames = {"creator_id", "username"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopCommenter {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "top_commenter_seq")
    @SequenceGenerator(name = "top_commenter_seq", sequenceName = "top_commenters_id_seq", allocationSize = 50)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private Creator creator;
    
    @Column(name = "username", nullable = false)
    private String username;
    
    @Column(name = "comment_count")
    private Integer commentCount = 0;
    
    @Column(name = "total_likes_received")
    private Long totalLikesReceived = 0L;
    
    @Column(name = "first_commented_at")
    private LocalDateTime firstCommentedAt;
    
    @Column(name = "last_commented_at")
    private LocalDateTime lastCommentedAt;
    
    @Column(name = "superfan_score")
    private Double superfanScore;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
}
