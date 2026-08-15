package com.MAYA.MAYA.Entity.instagram;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "posts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Post {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "instagram_id", nullable = false, unique = true)
    private String instagramId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private Creator creator;
    
    @Column(name = "caption", columnDefinition = "TEXT")
    private String caption;
    
    @Column(name = "media_type", nullable = false)
    private String mediaType;
    
    @Column(name = "media_url", columnDefinition = "TEXT")
    private String mediaUrl;
    
    @Column(name = "permalink", columnDefinition = "TEXT")
    private String permalink;
    
    @Column(name = "shortcode")
    private String shortcode;
    
    @Column(name = "thumbnail_url", columnDefinition = "TEXT")
    private String thumbnailUrl;
    
    @Column(name = "hashtags", columnDefinition = "TEXT")
    private String hashtags;
    
    @Column(name = "is_comment_enabled")
    private Boolean isCommentEnabled = true;
    
    @Column(name = "media_product_type")
    private String mediaProductType;
    
    @Column(name = "is_shared_to_feed")
    private Boolean isSharedToFeed = false;
    
    @Column(name = "caption_length")
    private Integer captionLength = 0;
    
    @Column(name = "has_question")
    private Boolean hasQuestion = false;
    
    @Column(name = "has_cta")
    private Boolean hasCta = false;
    
    @Column(name = "cta_type")
    private String ctaType;
    
    @Column(name = "hashtag_count")
    private Integer hashtagCount = 0;
    
    // Nullable — VIDEO only, null for IMAGE posts
    @Column(name = "view_count")
    private Long viewCount;
    
    @Column(name = "play_through_rate")
    private Double playThroughRate;
    
    @Column(name = "comment_rate")
    private Double commentRate;
    
    @Column(name = "like_to_comment_ratio")
    private Double likeToCommentRatio;
    
    @Column(name = "reach_efficiency")
    private Double reachEfficiency;
    
    @Embedded
    private PostMetrics metrics = new PostMetrics();
    
    @Column(name = "posted_at", nullable = false)
    private LocalDateTime postedAt;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    // Override setter to auto-compute caption length
    public void setCaption(String caption) {
        this.caption = caption;
        this.captionLength = (caption != null) ? caption.length() : 0;
    }
}
