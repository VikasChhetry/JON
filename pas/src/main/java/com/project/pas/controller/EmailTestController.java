package com.project.pas.controller;

import com.project.pas.service.EmailService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/email")
public class EmailTestController {

    private final EmailService emailService;

    public EmailTestController(EmailService emailService) {
        this.emailService = emailService;
    }

    /**
     * Development-only endpoint to test SMTP email delivery externally.
     * Protected by a simple secret to prevent public abuse.
     */
    @PostMapping("/test")
    public ResponseEntity<?> testEmailDelivery(@RequestBody Map<String, String> request) {
        // Enforce a simple secret key so this test endpoint isn't abused
        String secret = request.get("secret");
        if (!"dev-secret-2026".equals(secret)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Unauthorized. Invalid secret."));
        }

        String toEmail = request.get("to");
        if (toEmail == null || toEmail.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "The 'to' field is required."));
        }

        try {
            // Re-use the email service but pass a dummy token for testing
            emailService.sendPasswordResetEmail(toEmail, "Test User", "TEST-DIAGNOSTIC-TOKEN", 15);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "recipient", toEmail,
                    "message", "Email submitted successfully to Gmail SMTP server.",
                    "diagnostic_note",
                    "SUCCESS ONLY MEANS GMAIL ACCEPTED THE MESSAGE. It DOES NOT prove that the recipient's downstream server (e.g., niet.co.in) actually delivered it to the inbox. Please check the target inbox/spam, and check with NIET IT admins if it never arrives."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "status", "error",
                    "recipient", toEmail,
                    "message", "Failed to submit email to SMTP server.",
                    "error_details", e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
        }
    }
}
