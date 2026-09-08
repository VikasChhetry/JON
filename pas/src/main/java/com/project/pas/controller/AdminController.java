package com.project.pas.controller;

import com.project.pas.model.Branch;
import com.project.pas.model.Role;
import com.project.pas.model.User;
import com.project.pas.service.BranchService;
import com.project.pas.service.ProjectService;
import com.project.pas.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final BranchService branchService;
    private final UserService userService;
    private final ProjectService projectService;

    public AdminController(BranchService branchService, UserService userService, ProjectService projectService) {
        this.branchService = branchService;
        this.userService = userService;
        this.projectService = projectService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("totalBranches", branchService.count());
        model.addAttribute("totalUsers", userService.count());
        model.addAttribute("totalStudents", userService.countByRole(Role.STUDENT));
        model.addAttribute("totalFaculty", userService.countByRole(Role.FACULTY));
        model.addAttribute("totalHods", userService.countByRole(Role.HOD));
        model.addAttribute("totalProjects", projectService.countAll());
        return "admin/dashboard";
    }

    // ==================== Branch Management ====================

    @GetMapping("/branches")
    public String listBranches(Model model) {
        model.addAttribute("branches", branchService.getAllBranches());
        return "admin/branches";
    }

    @GetMapping("/branches/new")
    public String newBranchForm(Model model) {
        model.addAttribute("branch", new Branch());
        model.addAttribute("isEdit", false);
        return "admin/branch-form";
    }

    @PostMapping("/branches/new")
    public String createBranch(@RequestParam String name, @RequestParam String code,
            RedirectAttributes redirect) {
        try {
            branchService.createBranch(name, code);
            redirect.addFlashAttribute("success", "Branch created successfully");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/branches";
    }

    @GetMapping("/branches/edit/{id}")
    public String editBranchForm(@PathVariable Long id, Model model) {
        Branch branch = branchService.getBranchById(id)
                .orElseThrow(() -> new IllegalArgumentException("Branch not found"));
        model.addAttribute("branch", branch);
        model.addAttribute("isEdit", true);
        return "admin/branch-form";
    }

    @PostMapping("/branches/edit/{id}")
    public String updateBranch(@PathVariable Long id, @RequestParam String name,
            @RequestParam String code, RedirectAttributes redirect) {
        try {
            branchService.updateBranch(id, name, code);
            redirect.addFlashAttribute("success", "Branch updated successfully");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/branches";
    }

    // ==================== User Management ====================

    @GetMapping("/users")
    public String listUsers(Model model) {
        model.addAttribute("users", userService.getAllUsers());
        return "admin/users";
    }

    @GetMapping("/users/new")
    public String newUserForm(Model model) {
        model.addAttribute("user", new User());
        model.addAttribute("branches", branchService.getAllBranches());
        model.addAttribute("roles", Role.values());
        model.addAttribute("isEdit", false);
        return "admin/user-form";
    }

    @PostMapping("/users/upload")
    public String uploadUsers(@RequestParam("file") org.springframework.web.multipart.MultipartFile file, RedirectAttributes redirect) {
        try {
            UserService.BulkUserUploadResult result = userService.uploadUsersFromCsv(file);
            redirect.addFlashAttribute("bulkResult", result);
            redirect.addFlashAttribute("success", "Bulk upload completed. Processed: " + result.getTotalProcessed() + 
                    ", Success: " + result.getSuccessCount() + ", Failed: " + result.getFailedCount());
        } catch (Exception e) {
            redirect.addFlashAttribute("error", "Upload failed: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/users/template")
    public void downloadTemplate(jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=\"user_upload_template.csv\"");
        java.io.PrintWriter writer = response.getWriter();
        writer.println("fullName,email,role,branch,password");
        writer.println("John Doe,john@example.com,STUDENT,CS,password123");
        writer.println("Jane Smith,jane@example.com,FACULTY,IT,password123");
        writer.println("Admin User,admin2@example.com,ADMIN,,password123");
        writer.flush();
    }

    @PostMapping("/users/new")
    public String createUser(@RequestParam String fullName, @RequestParam String email,
            @RequestParam String password, @RequestParam Role role,
            @RequestParam(required = false) Long branchId,
            RedirectAttributes redirect) {
        try {
            Branch branch = null;
            if (branchId != null) {
                branch = branchService.getBranchById(branchId)
                        .orElseThrow(() -> new IllegalArgumentException("Branch not found"));
            }
            userService.createUser(fullName, email, password, role, branch);
            redirect.addFlashAttribute("success", "User created successfully");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/users/edit/{id}")
    public String editUserForm(@PathVariable Long id, Model model) {
        User user = userService.getUserById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        model.addAttribute("user", user);
        model.addAttribute("branches", branchService.getAllBranches());
        model.addAttribute("roles", Role.values());
        model.addAttribute("isEdit", true);
        return "admin/user-form";
    }

    @PostMapping("/users/edit/{id}")
    public String updateUser(@PathVariable Long id, @RequestParam String fullName,
            @RequestParam String email, @RequestParam Role role,
            @RequestParam(required = false) Long branchId,
            RedirectAttributes redirect) {
        try {
            Branch branch = null;
            if (branchId != null) {
                branch = branchService.getBranchById(branchId)
                        .orElseThrow(() -> new IllegalArgumentException("Branch not found"));
            }
            userService.updateUser(id, fullName, email, role, branch);
            redirect.addFlashAttribute("success", "User updated successfully");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/toggle")
    public String toggleUser(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            userService.toggleEnabled(id);
            redirect.addFlashAttribute("success", "User status updated");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/reset-password")
    public String resetPassword(@PathVariable Long id, @RequestParam String newPassword,
            RedirectAttributes redirect) {
        try {
            userService.updatePassword(id, newPassword);
            redirect.addFlashAttribute("success", "Password reset successfully");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    // ==================== Delete Operations ====================

    @PostMapping("/branches/{id}/delete")
    public String deleteBranch(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            branchService.deleteBranch(id);
            redirect.addFlashAttribute("success", "Branch deleted successfully");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", "Cannot delete branch: " + e.getMessage());
        }
        return "redirect:/admin/branches";
    }

    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            userService.deleteUser(id);
            redirect.addFlashAttribute("success", "User deleted successfully");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", "Cannot delete user: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    // ==================== Project Management ====================

    @GetMapping("/projects")
    public String listProjects(Model model) {
        model.addAttribute("projects", projectService.getAllProjects());
        return "admin/projects";
    }

    @PostMapping("/projects/{id}/delete")
    public String deleteProject(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            projectService.adminDeleteProject(id);
            redirect.addFlashAttribute("success", "Project deleted successfully");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", "Cannot delete project: " + e.getMessage());
        }
        return "redirect:/admin/projects";
    }
}
