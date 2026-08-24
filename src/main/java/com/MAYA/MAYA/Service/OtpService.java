package com.MAYA.MAYA.Service;

import com.MAYA.MAYA.Entity.OtpVerification;
import com.MAYA.MAYA.Repository.OtpVerificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private final OtpVerificationRepository otpRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    private static final int OTP_LENGTH = 6;
    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final int MAX_ATTEMPTS = 3;
    private static final int MAX_SENDS_PER_WINDOW = 3;
    private static final int RATE_LIMIT_WINDOW_MINUTES = 10;

    /**
     * Generate OTP, store pending registration, and send email.
     *
     * @return "SENT" on success, error message on failure
     */
    @Transactional
    public String generateAndSendOtp(String email, String name, String firstname, String lastname, String rawPassword) {
        // Rate limit: max 3 OTP sends per email per 10 minutes
        LocalDateTime windowStart = LocalDateTime.now().minusMinutes(RATE_LIMIT_WINDOW_MINUTES);
        List<OtpVerification> recentSends = otpRepository.findByEmailAndCreatedAtAfter(email, windowStart);
        if (recentSends.size() >= MAX_SENDS_PER_WINDOW) {
            return "RATE_LIMITED";
        }

        // Generate 6-digit OTP
        String otp = generateOtp();

        // Store OTP record (hash the OTP and password)
        OtpVerification verification = new OtpVerification();
        verification.setEmail(email.toLowerCase().trim());
        verification.setOtpHash(passwordEncoder.encode(otp));
        verification.setName(name);
        verification.setFirstname(firstname);
        verification.setLastname(lastname);
        verification.setPasswordHash(passwordEncoder.encode(rawPassword));
        verification.setAttempts(0);
        verification.setExpiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
        verification.setCreatedAt(LocalDateTime.now());
        otpRepository.save(verification);

        // Send email
        boolean sent = emailService.sendOtpEmail(email, otp);
        if (!sent) {
            return "EMAIL_FAILED";
        }

        log.info("OTP generated and sent for email: {}", email);
        return "SENT";
    }

    /**
     * Verify the OTP for a given email.
     *
     * @return OtpVerification record if valid, null otherwise. Sets error context via return codes.
     */
    @Transactional
    public VerifyResult verifyOtp(String email, String otpInput) {
        Optional<OtpVerification> latest = otpRepository.findTopByEmailOrderByCreatedAtDesc(email.toLowerCase().trim());

        if (latest.isEmpty()) {
            return new VerifyResult(Status.NOT_FOUND, null);
        }

        OtpVerification record = latest.get();

        // Check expiry
        if (record.getExpiresAt().isBefore(LocalDateTime.now())) {
            return new VerifyResult(Status.EXPIRED, null);
        }

        // Check max attempts
        if (record.getAttempts() >= MAX_ATTEMPTS) {
            return new VerifyResult(Status.MAX_ATTEMPTS, null);
        }

        // Verify OTP hash
        if (!passwordEncoder.matches(otpInput, record.getOtpHash())) {
            record.setAttempts(record.getAttempts() + 1);
            otpRepository.save(record);
            int remaining = MAX_ATTEMPTS - record.getAttempts();
            return new VerifyResult(Status.INVALID, remaining);
        }

        // Success — clean up all OTP records for this email
        otpRepository.deleteByEmail(email.toLowerCase().trim());
        return new VerifyResult(Status.SUCCESS, record);
    }

    /**
     * Clean up expired OTPs (called periodically or on demand).
     */
    @Transactional
    public void cleanupExpired() {
        otpRepository.deleteByExpiresAtBefore(LocalDateTime.now());
    }

    private String generateOtp() {
        SecureRandom random = new SecureRandom();
        int otp = 100000 + random.nextInt(900000); // 6-digit: 100000 to 999999
        return String.valueOf(otp);
    }

    // --- Result types ---

    public enum Status {
        SUCCESS, INVALID, EXPIRED, MAX_ATTEMPTS, NOT_FOUND
    }

    public record VerifyResult(Status status, Object data) {
        @SuppressWarnings("unchecked")
        public OtpVerification getRecord() {
            return (data instanceof OtpVerification) ? (OtpVerification) data : null;
        }

        public Integer getRemainingAttempts() {
            return (data instanceof Integer) ? (Integer) data : null;
        }
    }
}
