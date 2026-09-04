package com.MAYA.MAYA.Repository;

import com.MAYA.MAYA.Entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    // All non-expired, unused tokens (we match the raw token against their hashes)
    List<PasswordResetToken> findByUsedFalseAndExpiresAtAfter(LocalDateTime now);

    // Rate limiting: how many resets requested for this email recently
    List<PasswordResetToken> findByEmailAndCreatedAtAfter(String email, LocalDateTime after);

    void deleteByEmail(String email);

    void deleteByExpiresAtBefore(LocalDateTime now);
}
