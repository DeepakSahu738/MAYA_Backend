package com.MAYA.MAYA.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * A single-use, short-lived token for the "forgot password" flow.
 *
 * The raw token is emailed to the user as part of a reset link and NEVER stored.
 * Only its hash lives in the DB — so a DB leak can't be used to reset passwords.
 * A token is valid only if it exists, hasn't expired, and hasn't been used.
 */
@Entity
@Table(name = "password_reset_tokens", indexes = {
    @Index(name = "idx_prt_email", columnList = "email")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The Maya user's email this reset is for (lowercased)
    @Column(nullable = false)
    private String email;

    // BCrypt hash of the raw token (raw token only exists in the emailed link)
    @Column(name = "token_hash", nullable = false)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used", nullable = false)
    private boolean used = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
