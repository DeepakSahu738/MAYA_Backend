package com.MAYA.MAYA.Service;

import com.MAYA.MAYA.Entity.PasswordResetToken;
import com.MAYA.MAYA.Entity.user;
import com.MAYA.MAYA.Repository.PasswordResetTokenRepository;
import com.MAYA.MAYA.Repository.userRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

/**
 * Forgot-password (reset-link) flow.
 *
 * 1. requestReset(email): if the user exists, generate a secure random token,
 *    store only its hash with a short expiry, and email a reset link containing
 *    the raw token. Always behaves the same to the caller so it never reveals
 *    whether an email is registered.
 * 2. resetPassword(rawToken, newPassword): validate the token (exists, not
 *    expired, not used), update the user's password, and consume the token.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private final PasswordResetTokenRepository tokenRepository;
    private final userRepository userRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.frontend-base-url}")
    private String frontendBaseUrl;

    private static final int TOKEN_BYTES = 32;                 // 256-bit token
    private static final int TOKEN_EXPIRY_MINUTES = 30;
    private static final int RATE_LIMIT_WINDOW_MINUTES = 10;
    private static final int MAX_REQUESTS_PER_WINDOW = 3;
    private static final int MIN_PASSWORD_LENGTH = 6;

    public enum ResetStatus { SUCCESS, INVALID_TOKEN, EXPIRED, WEAK_PASSWORD }

    /**
     * Request a password reset. Always returns normally (no info leak about
     * whether the email exists). Returns "RATE_LIMITED" only to let the caller
     * optionally surface a soft warning.
     */
    @Transactional
    public String requestReset(String email) {
        String normalized = email.toLowerCase().trim();

        Optional<user> maybeUser = userRepository.findByEmailIgnoreCase(normalized);
        if (maybeUser.isEmpty()) {
            // Don't reveal that the email isn't registered — behave as if sent.
            log.info("Password reset requested for non-existent email (ignored): {}", normalized);
            return "SENT";
        }

        // Rate limit: max N requests per email per window
        LocalDateTime windowStart = LocalDateTime.now().minusMinutes(RATE_LIMIT_WINDOW_MINUTES);
        List<PasswordResetToken> recent = tokenRepository.findByEmailAndCreatedAtAfter(normalized, windowStart);
        if (recent.size() >= MAX_REQUESTS_PER_WINDOW) {
            log.warn("Password reset rate-limited for email: {}", normalized);
            return "RATE_LIMITED";
        }

        // Generate a URL-safe random token; store only its hash
        String rawToken = generateToken();
        PasswordResetToken token = new PasswordResetToken();
        token.setEmail(normalized);
        token.setTokenHash(passwordEncoder.encode(rawToken));
        token.setExpiresAt(LocalDateTime.now().plusMinutes(TOKEN_EXPIRY_MINUTES));
        token.setUsed(false);
        token.setCreatedAt(LocalDateTime.now());
        tokenRepository.save(token);

        String resetLink = buildResetLink(rawToken);
        emailService.sendPasswordResetEmail(normalized, resetLink);

        log.info("Password reset link sent to: {}", normalized);
        return "SENT";
    }

    /**
     * Complete the reset. Validates the raw token against stored hashes.
     */
    @Transactional
    public ResetStatus resetPassword(String rawToken, String newPassword) {
        if (rawToken == null || rawToken.isBlank()) return ResetStatus.INVALID_TOKEN;
        if (newPassword == null || newPassword.length() < MIN_PASSWORD_LENGTH) return ResetStatus.WEAK_PASSWORD;

        // Match the raw token against all active (unused, non-expired) token hashes.
        List<PasswordResetToken> candidates = tokenRepository.findByUsedFalseAndExpiresAtAfter(LocalDateTime.now());
        PasswordResetToken match = null;
        for (PasswordResetToken t : candidates) {
            if (passwordEncoder.matches(rawToken, t.getTokenHash())) {
                match = t;
                break;
            }
        }

        if (match == null) {
            // Either wrong token, already used, or expired — all indistinguishable to the client
            return ResetStatus.INVALID_TOKEN;
        }

        Optional<user> maybeUser = userRepository.findByEmailIgnoreCase(match.getEmail());
        if (maybeUser.isEmpty()) {
            return ResetStatus.INVALID_TOKEN;
        }

        user u = maybeUser.get();
        u.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(u);

        // Consume this token and clear any other outstanding tokens for this email
        match.setUsed(true);
        tokenRepository.save(match);
        tokenRepository.deleteByEmail(match.getEmail());

        log.info("Password successfully reset for: {}", match.getEmail());
        return ResetStatus.SUCCESS;
    }

    /** Remove expired tokens (housekeeping). */
    @Transactional
    public void cleanupExpired() {
        tokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String buildResetLink(String rawToken) {
        String base = frontendBaseUrl.endsWith("/") ? frontendBaseUrl.substring(0, frontendBaseUrl.length() - 1) : frontendBaseUrl;
        return base + "/reset-password?token=" + rawToken;
    }
}
