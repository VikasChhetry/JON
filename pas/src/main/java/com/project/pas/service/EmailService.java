package com.project.pas.service;

import jakarta.mail.MessagingException;
import jakarta.mail.SendFailedException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.Arrays;

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

        // ── Pre-send validation: ensure To address is set and looks plausible ──
        if (toEmail == null || toEmail.isBlank()) {
            logger.error("[EMAIL-FAIL] [CATEGORY-A] To-address is null or blank. No email sent.");
            throw new RuntimeException("Cannot send email: recipient address is empty.");
        }
        logger.info("[EMAIL-INIT] Building MIME message. From: {} | To: {} | Subject: {}",
                fromEmail, toEmail, subject);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            // Verify the headers actually carried through before handing off to SMTP
            logger.info("[EMAIL-HEADERS] From: {} | To: {} | Subject: {}",
                    message.getFrom() != null ? Arrays.toString(message.getFrom()) : "<not set>",
                    message.getRecipients(MimeMessage.RecipientType.TO) != null
                            ? Arrays.toString(message.getRecipients(MimeMessage.RecipientType.TO)) : "<not set>",
                    message.getSubject());

            logger.info("[EMAIL-SUBMIT] Handing message to Gmail SMTP (smtp.gmail.com:587). Recipient: {}", toEmail);
            mailSender.send(message);

            // ── CATEGORY C: Gmail accepted the message ──────────────────────────────
            // This log line means the Gmail SMTP server issued a 250 OK response.
            // It does NOT mean the remote MX server (e.g. niet.co.in) delivered it.
            // The message is now in Gmail's outbound queue; delivery to @niet.co.in
            // depends entirely on NIET's mail server accepting it.
            logger.info("[EMAIL-ACCEPTED] [CATEGORY-C-POSSIBLE] Gmail SMTP accepted the message for: {}"
                    + " Gmail will now attempt delivery to the recipient domain."
                    + " If @niet.co.in does not receive it, the issue is DOWNSTREAM"
                    + " (NIET mail server filtering, SPF/DKIM rejection, or quarantine).",
                    toEmail);

        } catch (MessagingException e) {
            // ── CATEGORY A: Local message construction failed ───────────────────────
            logger.error("[EMAIL-FAIL] [CATEGORY-A] Local MIME construction error for recipient: {}. "
                    + "Exception: {} | Message: {}",
                    toEmail, e.getClass().getName(), e.getMessage());
            throw new RuntimeException("Failed to build password reset email. Please try again later.", e);

        } catch (MailAuthenticationException e) {
            // ── CATEGORY B: Gmail rejected our credentials ──────────────────────────
            logger.error("[EMAIL-FAIL] [CATEGORY-B] Gmail SMTP authentication failed. "
                    + "Check that MAIL_USERNAME env var is the correct Gmail address "
                    + "and MAIL_PASSWORD is a valid App Password (not your Gmail login password). "
                    + "Exception: {}", e.getClass().getName());
            // NOTE: intentionally NOT logging e.getMessage() — it may contain credential hints
            throw new RuntimeException("Email authentication failed. Check server SMTP credentials.", e);

        } catch (MailSendException e) {
            // ── CATEGORY B: Gmail SMTP rejected the send command ───────────────────
            // Extract per-recipient failure info from the exception
            StringBuilder recipientErrors = new StringBuilder();
            if (e.getFailedMessages() != null) {
                e.getFailedMessages().forEach((msg, ex) -> {
                    String recipientList = "unknown";
                    try {
                        if (msg instanceof MimeMessage mimeMsg) {
                            InternetAddress[] addrs =
                                    (InternetAddress[]) mimeMsg.getRecipients(MimeMessage.RecipientType.TO);
                            if (addrs != null) {
                                recipientList = Arrays.toString(addrs);
                            }
                        }
                    } catch (MessagingException ignored) { }

                    // Dig into the root cause to find the SMTP response code
                    String smtpCode = "N/A";
                    String smtpMsg  = ex.getMessage();
                    Throwable cause = ex.getCause();
                    if (cause instanceof SendFailedException sfe) {
                        // jakarta.mail.SendFailedException carries the SMTP response string
                        smtpMsg  = sfe.getMessage();
                        // Parse the 3-digit SMTP code from the start of the message if present
                        if (smtpMsg != null && smtpMsg.length() >= 3 && Character.isDigit(smtpMsg.charAt(0))) {
                            smtpCode = smtpMsg.substring(0, 3);
                        }
                    }
                    recipientErrors.append("Recipient=").append(recipientList)
                            .append(" | SMTP-Code=").append(smtpCode)
                            .append(" | SMTP-Response=").append(smtpMsg)
                            .append(" | ExceptionClass=").append(ex.getClass().getName())
                            .append("; ");
                });
            }
            logger.error("[EMAIL-FAIL] [CATEGORY-B] Gmail SMTP rejected the send command for recipient: {}. "
                    + "Per-recipient SMTP details: [{}] "
                    + "Common causes: recipient domain MX blocked by Gmail, "
                    + "invalid address format, or Gmail daily send limit exceeded.",
                    toEmail, recipientErrors);
            throw new RuntimeException("Failed to send password reset email. Please try again later.", e);

        } catch (MailException e) {
            // ── CATEGORY B fallback: other Spring Mail transport error ─────────────
            logger.error("[EMAIL-FAIL] [CATEGORY-B] Spring MailException for recipient: {}. "
                    + "ExceptionClass: {} | Message: {}",
                    toEmail, e.getClass().getName(), e.getMessage());
            throw new RuntimeException("Failed to send password reset email. Please try again later.", e);
        }
    }

}
