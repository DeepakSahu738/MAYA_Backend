package com.MAYA.MAYA.Repository;

import com.MAYA.MAYA.Entity.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {
    Optional<OtpVerification> findTopByEmailOrderByCreatedAtDesc(String email);
    List<OtpVerification> findByEmailAndCreatedAtAfter(String email, LocalDateTime after);
    void deleteByEmail(String email);
    void deleteByExpiresAtBefore(LocalDateTime now);
}
