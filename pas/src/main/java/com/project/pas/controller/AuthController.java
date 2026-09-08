package com.project.pas.controller;

import com.project.pas.model.User;
import com.project.pas.service.PasswordResetService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
public class AuthController {

    private final PasswordResetService passwordResetService;

    public AuthController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/login";
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String processForgotPassword(@RequestParam("email") String email, RedirectAttributes redirectAttributes) {
        try {
            passwordResetService.processForgotPassword(email);
            String maskedEmail = maskEmail(email);
            redirectAttributes.addFlashAttribute("successEmail", maskedEmail);
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error sending email: " + e.getMessage());
        }
        return "redirect:/forgot-password";
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        String[] parts = email.split("@");
        String name = parts[0];
        String domain = parts[1];
        if (name.length() <= 2) {
            name = name.charAt(0) + "***";
        } else {
            name = name.charAt(0) + "***" + name.charAt(name.length() - 1);
        }
        return name + "@" + domain;
    }

    @GetMapping("/reset-password")
    public String resetPasswordPage(@RequestParam("token") String token, Model model) {
        Optional<User> userOpt = passwordResetService.validatePasswordResetToken(token);
        if (userOpt.isPresent()) {
            model.addAttribute("token", token);
            return "reset-password";
        } else {
            model.addAttribute("errorMessage", "Invalid or expired password reset token.");
            return "login";
        }
    }

    @PostMapping("/reset-password")
    public String processResetPassword(@RequestParam("token") String token,
            @RequestParam("password") String password,
            @RequestParam("confirmPassword") String confirmPassword,
            RedirectAttributes redirectAttributes) {
        if (!password.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Passwords do not match.");
            return "redirect:/reset-password?token=" + token;
        }

        try {
            passwordResetService.resetPassword(token, password);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Password has been successfully reset. You can now login.");
            return "redirect:/login";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/login";
        }
    }
}
