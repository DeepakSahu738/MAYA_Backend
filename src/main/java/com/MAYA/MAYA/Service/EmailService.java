package com.MAYA.MAYA.Service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.from:noreply@mayamanage.com}")
    private String fromAddress;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Send OTP email for registration verification.
     *
     * @param toEmail recipient email
     * @param otp     the 6-digit OTP code (plain text — NOT the hash)
     * @return true if sent successfully, false otherwise
     */
    public boolean sendOtpEmail(String toEmail, String otp) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject("Maya - Verify your email");
            helper.setText(buildOtpEmailBody(otp), true);

            mailSender.send(message);
            log.info("OTP email sent to: {}", toEmail);
            return true;
        } catch (Exception e) {
            log.error("Failed to send OTP email to {}: {}", toEmail, e.getMessage());
            return false;
        }
    }

    private String buildOtpEmailBody(String otp) {
        return """
            <div style="font-family: 'Segoe UI', Arial, sans-serif; max-width: 480px; margin: 0 auto; padding: 32px;">
                <h2 style="color: #1a1a2e; margin-bottom: 8px;">Verify your email</h2>
                <p style="color: #555; font-size: 15px; line-height: 1.5;">
                    Use the code below to complete your Maya registration. This code expires in 5 minutes.
                </p>
                <div style="background: #f4f4f8; border-radius: 12px; padding: 24px; text-align: center; margin: 24px 0;">
                    <span style="font-size: 32px; font-weight: 700; letter-spacing: 6px; color: #4f46e5;">%s</span>
                </div>
                <p style="color: #888; font-size: 13px;">
                    If you didn't request this, you can safely ignore this email.
                </p>
                <hr style="border: none; border-top: 1px solid #eee; margin: 24px 0;">
                <p style="color: #aaa; font-size: 12px;">Maya - Your AI Growth Partner</p>
            </div>
            """.formatted(otp);
    }

    /**
     * Send notification when data sync is complete.
     */
    public boolean sendSyncCompleteEmail(String toEmail, String username, String platform, int postsCount) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject("Maya - Your " + platform + " data is ready!");
            helper.setText(buildSyncCompleteBody(username, platform, postsCount), true);

            mailSender.send(message);
            log.info("Sync complete email sent to: {}", toEmail);
            return true;
        } catch (Exception e) {
            log.error("Failed to send sync complete email to {}: {}", toEmail, e.getMessage());
            return false;
        }
    }

    private String buildSyncCompleteBody(String username, String platform, int postsCount) {
        return """
            <div style="font-family: 'Segoe UI', Arial, sans-serif; max-width: 480px; margin: 0 auto; padding: 32px;">
                <h2 style="color: #1a1a2e; margin-bottom: 8px;">Your data is ready!</h2>
                <p style="color: #555; font-size: 15px; line-height: 1.5;">
                    We've finished syncing your %s account <strong>@%s</strong>.
                </p>
                <div style="background: #f0fdf4; border-radius: 12px; padding: 20px; margin: 24px 0; border-left: 4px solid #22c55e;">
                    <p style="margin: 0; color: #166534; font-size: 15px;">
                        <strong>%d posts</strong> synced and analyzed successfully.
                    </p>
                </div>
                <p style="color: #555; font-size: 15px; line-height: 1.5;">
                    Your analytics dashboard, AI insights, and content recommendations are now available.
                </p>
                <a href="https://mayamanage.com/dashboard" style="display: inline-block; background: #4f46e5; color: white; padding: 12px 24px; border-radius: 8px; text-decoration: none; font-weight: 600; margin-top: 16px;">
                    View Your Dashboard
                </a>
                <hr style="border: none; border-top: 1px solid #eee; margin: 24px 0;">
                <p style="color: #aaa; font-size: 12px;">Maya - Your AI Growth Partner</p>
            </div>
            """.formatted(platform, username, postsCount);
    }

    /**
     * Send a password reset link email.
     *
     * @param toEmail   recipient email
     * @param resetLink the full reset URL containing the raw token
     * @return true if sent successfully, false otherwise
     */
    public boolean sendPasswordResetEmail(String toEmail, String resetLink) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject("Maya - Reset your password");
            helper.setText(buildPasswordResetBody(resetLink), true);

            mailSender.send(message);
            log.info("Password reset email sent to: {}", toEmail);
            return true;
        } catch (Exception e) {
            log.error("Failed to send password reset email to {}: {}", toEmail, e.getMessage());
            return false;
        }
    }

    private String buildPasswordResetBody(String resetLink) {
        return """
            <div style="font-family: 'Segoe UI', Arial, sans-serif; max-width: 480px; margin: 0 auto; padding: 32px;">
                <h2 style="color: #1a1a2e; margin-bottom: 8px;">Reset your password</h2>
                <p style="color: #555; font-size: 15px; line-height: 1.5;">
                    We received a request to reset your Maya password. Click the button below to choose a new one. This link expires in 30 minutes.
                </p>
                <a href="%s" style="display: inline-block; background: #4f46e5; color: white; padding: 12px 24px; border-radius: 8px; text-decoration: none; font-weight: 600; margin: 24px 0;">
                    Reset Password
                </a>
                <p style="color: #888; font-size: 13px; line-height: 1.5;">
                    If the button doesn't work, copy and paste this link into your browser:<br>
                    <span style="color: #4f46e5; word-break: break-all;">%s</span>
                </p>
                <p style="color: #888; font-size: 13px;">
                    If you didn't request this, you can safely ignore this email — your password won't change.
                </p>
                <hr style="border: none; border-top: 1px solid #eee; margin: 24px 0;">
                <p style="color: #aaa; font-size: 12px;">Maya - Your AI Growth Partner</p>
            </div>
            """.formatted(resetLink, resetLink);
    }
}
