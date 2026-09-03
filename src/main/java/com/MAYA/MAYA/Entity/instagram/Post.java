package com.MAYA.MAYA.Entity.instagram;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * A single piece of content synced from Phyllo (post, video, reel, tweet, etc.).
 *
 * Field names mirror Phyllo's content schema so the sync layer, DB, and
 * analytics all share one vocabulary. Platform-specific fields that a given
 * platform does not report are left null. "caption" is the one deliberate
 * exception — Phyllo calls it "description", but Maya reasons about captions
 * across scheduling, strategy, and AI, so we keep the Maya-domain name.
 */
@Entity
@Table(name = "posts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Post {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "post_seq")
    @SequenceGenerator(name = "post_seq", sequenceName = "posts_id_seq", allocationSize = 50)
    private Long id;
    
    // Phyllo's content UUID (unique per content item)
    @Column(name = "phyllo_id", nullable = false, unique = true)
    private String phylloId;
    
    // Platform-native content ID (e.g. YouTube video ID, tweet ID)
    @Column(name = "external_id")
    private String externalId;
    
    // Platform this content belongs to: INSTAGRAM, YOUTUBE, TIKTOK, etc.
    @Column(name = "platform")
    private String platform;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private Creator creator;
    
    // Phyllo's "title" for the content
    @Column(name = "title", columnDefinition = "TEXT")
    private String title;
    
    // Phyllo's "description" — kept as "caption" (Maya-domain concept)
    @Column(name = "caption", columnDefinition = "TEXT")
    private String caption;
    
    // Phyllo's "format": VIDEO | IMAGE | AUDIO | TEXT | OTHER
    @Column(name = "format")
    private String format;
    
    // Phyllo's "type": platform-specific (REELS, STORY, TWEET, VIDEO, POST, etc.)
    @Column(name = "type")
    private String type;
    
    // Phyllo's "url" — the permanent content URL
    @Column(name = "url", columnDefinition = "TEXT")
    private String url;
    
    @Column(name = "media_url", columnDefinition = "TEXT")
    private String mediaUrl;
    
    @Column(name = "shortcode")
    private String shortcode;
    
    @Column(name = "thumbnail_url", columnDefinition = "TEXT")
    private String thumbnailUrl;
    
    @Column(name = "persistent_thumbnail_url", columnDefinition = "TEXT")
    private String persistentThumbnailUrl;
    
    // Video duration in seconds (nullable — video content only)
    @Column(name = "duration")
    private Integer duration;
    
    @Column(name = "hashtags", columnDefinition = "TEXT")
    private String hashtags;
    
    // Mentioned accounts (JSON array stored as text)
    @Column(name = "mentions", columnDefinition = "TEXT")
    private String mentions;
    
    // Visibility: PUBLIC | PRIVATE | UNLISTED
    @Column(name = "visibility")
    private String visibility;
    
    @Column(name = "platform_profile_id")
    private String platformProfileId;
    
    @Column(name = "platform_profile_name")
    private String platformProfileName;
    
    @Column(name = "is_owned_by_platform_user")
    private Boolean isOwnedByPlatformUser;
    
    @Column(name = "is_comment_enabled")
    private Boolean isCommentEnabled = true;
    
    @Column(name = "is_shared_to_feed")
    private Boolean isSharedToFeed = false;
    
    // --- Maya-computed content fields ---
    
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
