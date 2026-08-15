package com.MAYA.MAYA.Entity.instagram;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "creators")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Creator {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "instagram_id", nullable = false, unique = true)
    private String instagramId;
    
    @Column(nullable = false)
    private String username;
    
    @Column(name = "profile_picture_url", columnDefinition = "TEXT")
    private String profilePictureUrl;
    
    @Column(name = "biography", columnDefinition = "TEXT")
    private String biography;
    
    @Column(name = "website", columnDefinition = "TEXT")
    private String website;
    
    @Column(name = "email")
    private String email;
    
    @Column(name = "is_verified")
    private Boolean isVerified = false;
    
    @Column(name = "account_type")
    private String accountType;
    
    // WARNING: Must be encrypted before storing, never store in plaintext
    @Column(name = "access_token")
    private String accessToken;
    
    @Column(name = "token_expires_at")
    private LocalDateTime tokenExpiresAt;
    
    @Column(name = "follower_count")
    private Integer followerCount;
    
    @Column(name = "following_count")
    private Integer followingCount;
    
    @Column(name = "media_count")
    private Integer mediaCount;
    
    @Column(name = "profile_views_count")
    private Long profileViewsCount = 0L;
    
    @Column(name = "niche")
    private String niche;
    
    @Column(name = "content_goal")
    private String contentGoal;
    
    @Column(name = "target_audience")
    private String targetAudience;
    
    @Column(name = "tone_style")
    private String toneStyle;
    
    @Column(name = "preferred_content_types")
    private String preferredContentTypes;
    
    @Column(name = "brand_preferences", columnDefinition = "TEXT")
    private String brandPreferences;
    
    @Column(name = "connected_at")
    private LocalDateTime connectedAt;
    
    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
}
