package com.project.pas.service;

import com.project.pas.model.PasswordResetToken;
import com.project.pas.model.User;
import com.project.pas.repository.PasswordResetTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class PasswordResetService {

    private static final Logger logger = LoggerFactory.getLogger(PasswordResetService.class);
    private static final long TOKEN_EXPIRY_MINUTES = 15;

    private final PasswordResetTokenRepository tokenRepository;
    private final UserService userService;
    private final EmailService emailService;

    public PasswordResetService(PasswordResetTokenRepository tokenRepository,
            UserService userService,
            EmailService emailService) {
        this.tokenRepository = tokenRepository;
        this.userService = userService;
        this.emailService = emailService;
    }

    public void processForgotPassword(String email) {
        Optional<User> userOpt = userService.getUserByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            // Delete any existing tokens for this user
            tokenRepository.deleteByUser(user);

            // Create a new token with 15 minutes expiry
            String tokenString = UUID.randomUUID().toString();
            PasswordResetToken token = new PasswordResetToken(
                    tokenString, user, LocalDateTime.now().plusMinutes(TOKEN_EXPIRY_MINUTES));
            tokenRepository.save(token);

            // Send real password reset email via SMTP
            try {
                emailService.sendPasswordResetEmail(
                        user.getEmail(),
                        user.getFullName(),
                        tokenString,
                        TOKEN_EXPIRY_MINUTES);
            } catch (RuntimeException e) {
                // Log the failure but do NOT delete the token — the user can still
                // retry from the forgot-password page which will issue a new token.
                logger.error("Email delivery failed for password reset: {}", e.getMessage());
                throw new RuntimeException("Failed to send reset email. Please try again later.", e);
            }
        }
        // If user not found, silently do nothing (prevents email enumeration)
    }

    public Optional<User> validatePasswordResetToken(String tokenString) {
        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByToken(tokenString);
        if (tokenOpt.isPresent()) {
            PasswordResetToken token = tokenOpt.get();
            if (!token.isExpired()) {
                return Optional.of(token.getUser());
            } else {
                // Cleanup expired token
                tokenRepository.delete(token);
            }
        }
        return Optional.empty();
    }

    public void resetPassword(String tokenString, String newPassword) {
        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByToken(tokenString);
        if (tokenOpt.isPresent()) {
            PasswordResetToken token = tokenOpt.get();
            if (!token.isExpired()) {
                User user = token.getUser();
                userService.updatePassword(user.getId(), newPassword);
                tokenRepository.delete(token); // Token used and invalidated
            } else {
                throw new IllegalArgumentException("Token has expired");
            }
        } else {
            throw new IllegalArgumentException("Invalid token");
        }
    }
}
