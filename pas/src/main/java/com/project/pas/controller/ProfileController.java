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
    private final com.project.pas.service.FileStorageService fileStorageService;

    public ProfileController(UserService userService, com.project.pas.service.FileStorageService fileStorageService) {
        this.userService = userService;
        this.fileStorageService = fileStorageService;
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

    @PostMapping("/profile/photo")
    public String uploadPhoto(Authentication auth,
            @RequestParam("photo") org.springframework.web.multipart.MultipartFile file, RedirectAttributes redirect) {
        User user = getCurrentUser(auth);
        try {
            if (file == null || file.isEmpty()) {
                throw new IllegalArgumentException("Please select a file to upload");
            }

            String contentType = file.getContentType();
            if (contentType == null || (!contentType.equals("image/jpeg") && !contentType.equals("image/png")
                    && !contentType.equals("image/webp"))) {
                throw new IllegalArgumentException("Only JPG, PNG and WEBP images are allowed");
            }

            if (file.getSize() > 5 * 1024 * 1024) { // 5MB limit
                throw new IllegalArgumentException("File size exceeds 5MB limit");
            }

            String path = fileStorageService.storeFile(file, "profiles");
            userService.updateProfilePhoto(user.getId(), path);
            redirect.addFlashAttribute("success", "Profile photo updated successfully!");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/profile";
    }

    @PostMapping("/profile/photo/remove")
    public String removePhoto(Authentication auth, RedirectAttributes redirect) {
        User user = getCurrentUser(auth);
        try {
            userService.updateProfilePhoto(user.getId(), null);
            redirect.addFlashAttribute("success", "Profile photo removed.");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/profile";
    }

    @GetMapping("/profiles/{filename}")
    public org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> getProfilePhoto(
            @org.springframework.web.bind.annotation.PathVariable String filename) {
        try {
            java.nio.file.Path path = fileStorageService.getFilePath("profiles/" + filename);
            org.springframework.core.io.Resource resource = new org.springframework.core.io.UrlResource(path.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                return org.springframework.http.ResponseEntity.notFound().build();
            }

            return org.springframework.http.ResponseEntity.ok()
                    .header(org.springframework.http.HttpHeaders.CACHE_CONTROL, "max-age=86400")
                    .body(resource);
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.internalServerError().build();
        }
    }
}
