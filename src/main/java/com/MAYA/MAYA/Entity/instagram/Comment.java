package com.MAYA.MAYA.Entity.instagram;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "comments", indexes = {
    @Index(name = "idx_comments_creator_id", columnList = "creator_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Comment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "comment_seq")
    @SequenceGenerator(name = "comment_seq", sequenceName = "comments_id_seq", allocationSize = 50)
    private Long id;
    
    @Column(name = "instagram_id", nullable = false, unique = true)
    private String instagramId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;
    
    @Column(name = "creator_id", nullable = false)
    private Long creatorId;
    
    @Column(name = "username", nullable = false)
    private String username;
    
    @Column(name = "text", columnDefinition = "TEXT", nullable = false)
    private String text;
    
    @Column(name = "intent")
    private String intent;
    
    @Column(name = "sentiment")
    private String sentiment;
    
    @Column(name = "sentiment_score")
    private Double sentimentScore;
    
    @Column(name = "is_question")
    private Boolean isQuestion = false;
    
    @Column(name = "has_cta")
    private Boolean hasCta = false;
    
    @Column(name = "like_count")
    private Integer likeCount = 0;
    
    @Column(name = "reply_count")
    private Integer replyCount = 0;
    
    @Column(name = "parent_comment_id")
    private Long parentCommentId;
    
    @Column(name = "is_reply")
    private Boolean isReply = false;
    
    @Column(name = "commented_at", nullable = false)
    private LocalDateTime commentedAt;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
