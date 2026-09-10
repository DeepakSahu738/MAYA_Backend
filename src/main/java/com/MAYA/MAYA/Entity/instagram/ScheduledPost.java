package com.MAYA.MAYA.Entity.instagram;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * A single item on the weekly (Jira-style) content board.
 *
 * Two kinds of items live in this one table, distinguished by {@link #itemType}:
 *  - POST: a content post draft (uses approvalStatus lifecycle: PENDING → APPROVED → PUBLISHED)
 *  - TASK: a to-do around content (uses taskStatus lifecycle: TODO → IN_PROGRESS → DONE)
 *
 * Both share creator, caption (title/text), scheduledFor (the day it sits on), and
 * timestamps. Fields that don't apply to a type are simply left at their defaults
 * (e.g. a TASK carries mediaType = "NONE" and a null approvalStatus is not used).
 */
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

    // POST (default) or TASK — discriminates the two board item kinds.
    // NOT marked nullable=false at the DB level: ddl-auto=update added this column
    // to a table that already had rows, so pre-existing rows have NULL here. We
    // treat NULL as POST on read (see getItemType) rather than fail the query.
    @Enumerated(EnumType.STRING)
    @Column(name = "item_type")
    private ItemType itemType = ItemType.POST;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String caption;
    
    @Column(columnDefinition = "TEXT")
    private String hashtags;
    
    @Column(nullable = false)
    private String mediaType;
    
    private String mediaUrl;
    
    @Column(nullable = false)
    private LocalDateTime scheduledFor;
    
    // Post lifecycle — used only when itemType == POST
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApprovalStatus approvalStatus = ApprovalStatus.PENDING;

    // Task lifecycle — used only when itemType == TASK (nullable for posts)
    @Enumerated(EnumType.STRING)
    @Column(name = "task_status")
    private TaskStatus taskStatus;
    
    private String rejectionReason;
    
    private LocalDateTime approvedAt;
    
    private LocalDateTime publishedAt;
    
    private String publishedExternalId;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    // Null-safe getter: pre-existing rows (added before item_type existed) have
    // NULL — treat them as POST. Overrides Lombok's generated getter.
    public ItemType getItemType() {
        return itemType != null ? itemType : ItemType.POST;
    }

    public enum ItemType {
        POST,
        TASK
    }

    public enum ApprovalStatus {
        PENDING,
        APPROVED,
        REJECTED,
        PUBLISHED,
        FAILED
    }

    public enum TaskStatus {
        TODO,
        IN_PROGRESS,
        DONE
    }
}
