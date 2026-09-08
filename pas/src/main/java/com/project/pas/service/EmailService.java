package com.project.pas.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.base-url}")
    private String baseUrl;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Sends a password reset email with an HTML body containing the reset link,
     * token expiry information, and a security warning.
     *
     * @param toEmail       the recipient's email address
     * @param recipientName the recipient's display name
     * @param token         the password reset token
     * @param expiryMinutes how many minutes the token is valid
     */
    public void sendPasswordResetEmail(String toEmail, String recipientName, String token, long expiryMinutes) {
        String resetUrl = baseUrl + "/reset-password?token=" + token;

        String subject = "Password Reset Request – Project Approval System";

        String htmlBody = """
                <div style="font-family: 'Segoe UI', Arial, sans-serif; max-width: 600px; margin: auto; padding: 24px; border: 1px solid #e0e0e0; border-radius: 8px;">
                    <h2 style="color: #1a73e8;">Password Reset Request</h2>
                    <p>Hello <strong>%s</strong>,</p>
                    <p>We received a request to reset the password for your account associated with <strong>%s</strong>.</p>
                    <p>Click the button below to reset your password:</p>
                    <p style="text-align: center; margin: 28px 0;">
                        <a href="%s"
                           style="background-color: #1a73e8; color: #ffffff; padding: 12px 28px; text-decoration: none; border-radius: 6px; font-weight: bold; display: inline-block;">
                           Reset Password
                        </a>
                    </p>
                    <p>Or copy and paste this link into your browser:</p>
                    <p style="word-break: break-all; color: #555;">%s</p>
                    <p><strong>This link will expire in %d minutes.</strong></p>
                    <hr style="border: none; border-top: 1px solid #e0e0e0; margin: 24px 0;">
                    <p style="color: #d93025; font-size: 13px;">
                        ⚠️ If you did not request a password reset, please ignore this email.
                        Your password will remain unchanged and no action is required.
                    </p>
                    <p style="font-size: 12px; color: #888;">This is an automated message from the Project Approval System. Please do not reply.</p>
                </div>
                """
                .formatted(recipientName, toEmail, resetUrl, resetUrl, expiryMinutes);

        try {
            logger.info("Attempting to submit email to SMTP server. Recipient: {}", toEmail);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            mailSender.send(message);
            logger.info(
                    "Email submitted successfully to SMTP server for recipient: {}. NOTE: This means Gmail accepted the message. It DOES NOT guarantee that the recipient's institutional mail server (e.g. niet.co.in) will deliver it to their inbox.",
                    toEmail);

        } catch (MessagingException | MailException e) {
            String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            logger.error(
                    "Email submission failed for recipient {}. SMTP Server rejected the message or connection failed. Error Details: {}",
                    toEmail, errorMsg);
            throw new RuntimeException("Failed to send password reset email. Please try again later.", e);
        }
    }
}
