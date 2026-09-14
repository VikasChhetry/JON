package com.project.pas.controller;

import com.project.pas.model.User;
import com.project.pas.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Common profile controller accessible by all authenticated users.
 * Each user can only view and update their own profile.
 * Role and branch cannot be changed by the user — only by Admin.
 */
@Controller
public class ProfileController {

    private final UserService userService;

    public ProfileController(UserService userService) {
        this.userService = userService;
    }

    private User getCurrentUser(Authentication auth) {
        return userService.getUserByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @GetMapping("/profile")
    public String viewProfile(Authentication auth, Model model) {
        User user = getCurrentUser(auth);
        model.addAttribute("user", user);
        return "profile";
    }

    @PostMapping("/profile")
    public String updateProfile(Authentication auth,
            @RequestParam String fullName,
            @RequestParam(required = false) String erpId,
            @RequestParam(required = false) String rollNumber,
            RedirectAttributes redirect) {
        User user = getCurrentUser(auth);
        try {
            userService.updateOwnProfile(user.getId(), fullName, erpId, rollNumber);
            redirect.addFlashAttribute("success", "Profile updated successfully!");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/profile";
    }

    @PostMapping("/profile/change-password")
    public String changePassword(Authentication auth,
            @RequestParam String currentPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            RedirectAttributes redirect) {
        User user = getCurrentUser(auth);
        try {
            if (!newPassword.equals(confirmPassword)) {
                throw new IllegalArgumentException("New password and confirmation do not match");
            }
            userService.changeOwnPassword(user.getId(), currentPassword, newPassword);
            redirect.addFlashAttribute("success", "Password changed successfully!");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/profile";
    }
}
