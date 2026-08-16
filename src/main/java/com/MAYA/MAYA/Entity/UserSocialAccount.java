package com.MAYA.MAYA.Entity;

import com.MAYA.MAYA.Entity.instagram.Creator;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Links a Maya user to their connected social media accounts via Phyllo.
 *
 * One Maya user can have multiple social accounts (Instagram, YouTube, etc.)
 * Each social account maps to one Creator entity (which holds the analytics data).
 *
 * Table: user_social_accounts
 * Unique constraint: one user can only connect one account per platform per phyllo_account_id
 */
@Entity
@Table(name = "user_social_accounts",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "phyllo_account_id"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSocialAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "social_account_seq")
    @SequenceGenerator(name = "social_account_seq", sequenceName = "user_social_accounts_id_seq", allocationSize = 50)
    private Long id;

    // FK to Maya's users table
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // FK to creators table (holds all analytics data)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id")
    private Creator creator;

    // Phyllo identifiers
    @Column(name = "phyllo_user_id", nullable = false)
    private String phylloUserId;

    @Column(name = "phyllo_account_id")
    private String phylloAccountId;

    // Platform info
    @Column(name = "platform", nullable = false)
    private String platform; // INSTAGRAM, YOUTUBE, TIKTOK, etc.

    @Column(name = "platform_username")
    private String platformUsername;

    // Status
    @Column(name = "status", nullable = false)
    private String status; // CONNECTED, DISCONNECTED, PENDING

    @Column(name = "connected_at")
    private LocalDateTime connectedAt;

    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
