package com.project.pas.controller;

import com.project.pas.model.Branch;
import com.project.pas.model.Role;
import com.project.pas.model.User;
import com.project.pas.service.BranchService;
import com.project.pas.service.ProjectService;
import com.project.pas.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import com.project.pas.model.ProjectStatus;

import java.util.Arrays;
import java.util.Optional;

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

    private User getCurrentAdmin(Authentication auth) {
        return userService.getUserByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Admin not found"));
    }

    private void enforceGlobalAdmin(User admin) {
        if (admin.getRole() == Role.ADMIN && admin.getBranch() != null) {
            throw new SecurityException("Access Denied: Action restricted to Global Admin only.");
        }
    }

    private void enforceAdminBranchAccess(User admin, Branch targetBranch) {
        if (admin.getRole() == Role.ADMIN && admin.getBranch() != null) {
            if (targetBranch == null || !admin.getBranch().getId().equals(targetBranch.getId())) {
                throw new SecurityException(
                        "Access Denied: You can only manage resources within your assigned branch.");
            }
        }
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication auth, Model model) {
        User admin = getCurrentAdmin(auth);

        if (admin.getBranch() != null) {
            model.addAttribute("totalBranches", 1);
            model.addAttribute("totalUsers", userService.countByBranch(admin.getBranch()));
            model.addAttribute("totalStudents", userService.countByBranchAndRole(admin.getBranch(), Role.STUDENT));
            model.addAttribute("totalFaculty", userService.countByBranchAndRole(admin.getBranch(), Role.FACULTY));
            model.addAttribute("totalHods", userService.countByBranchAndRole(admin.getBranch(), Role.HOD));
            model.addAttribute("totalProjects", projectService.countByBranch(admin.getBranch()));
        } else {
            model.addAttribute("totalBranches", branchService.count());
            model.addAttribute("totalUsers", userService.count());
            model.addAttribute("totalStudents", userService.countByRole(Role.STUDENT));
            model.addAttribute("totalFaculty", userService.countByRole(Role.FACULTY));
            model.addAttribute("totalHods", userService.countByRole(Role.HOD));
            model.addAttribute("totalProjects", projectService.countAll());
        }
        return "admin/dashboard";
    }

    // ==================== Branch Management ====================

    @GetMapping("/branches")
    public String listBranches(
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "createdAt,desc") String[] sort,
            Model model) {
        User admin = getCurrentAdmin(auth);

        if (admin.getBranch() != null) {
            Page<Branch> branchPage = branchService.searchBranches(admin.getBranch().getName(),
                    PageRequest.of(page, size, parseSort(sort)));
            model.addAttribute("page", branchPage);
        } else {
            Page<Branch> branchPage = branchService.searchBranches(keyword,
                    PageRequest.of(page, size, parseSort(sort)));
            model.addAttribute("page", branchPage);
        }
        return "admin/branches";
    }

    @GetMapping("/branches/new")
    public String newBranchForm(Authentication auth, Model model) {
        enforceGlobalAdmin(getCurrentAdmin(auth));
        model.addAttribute("branch", new Branch());
        model.addAttribute("isEdit", false);
        return "admin/branch-form";
    }

    @PostMapping("/branches/new")
    public String createBranch(Authentication auth, @RequestParam String name, @RequestParam String code,
            RedirectAttributes redirect) {
        enforceGlobalAdmin(getCurrentAdmin(auth));
        try {
            branchService.createBranch(name, code);
            redirect.addFlashAttribute("success", "Branch created successfully");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/branches";
    }

    @GetMapping("/branches/edit/{id}")
    public String editBranchForm(Authentication auth, @PathVariable Long id, Model model) {
        User admin = getCurrentAdmin(auth);
        Branch branch = branchService.getBranchById(id)
                .orElseThrow(() -> new IllegalArgumentException("Branch not found"));
        enforceAdminBranchAccess(admin, branch);

        model.addAttribute("branch", branch);
        model.addAttribute("isEdit", true);
        return "admin/branch-form";
    }

    @PostMapping("/branches/edit/{id}")
    public String updateBranch(Authentication auth, @PathVariable Long id, @RequestParam String name,
            @RequestParam String code, RedirectAttributes redirect) {
        User admin = getCurrentAdmin(auth);
        try {
            Branch branch = branchService.getBranchById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Branch not found"));
            enforceAdminBranchAccess(admin, branch);

            branchService.updateBranch(id, name, code);
            redirect.addFlashAttribute("success", "Branch updated successfully");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/branches";
    }

    // ==================== User Management ====================

    @GetMapping("/users")
    public String listUsers(
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) Boolean status,
            @RequestParam(defaultValue = "createdAt,desc") String[] sort,
            Model model) {

        User admin = getCurrentAdmin(auth);

        Branch branchFilter = null;
        if (admin.getBranch() != null) {
            branchFilter = admin.getBranch();
        } else if (branchId != null) {
            branchFilter = branchService.getBranchById(branchId).orElse(null);
        }

        Page<User> userPage = userService.searchUsers(keyword, role, branchFilter, status,
                PageRequest.of(page, size, parseSort(sort)));
        model.addAttribute("page", userPage);

        if (admin.getBranch() != null) {
            model.addAttribute("branches", Arrays.asList(admin.getBranch()));
        } else {
            model.addAttribute("branches", branchService.getAllBranches());
        }

        model.addAttribute("roles", Role.values());
        return "admin/users";
    }

    @GetMapping("/users/new")
    public String newUserForm(Authentication auth, Model model) {
        User admin = getCurrentAdmin(auth);
        model.addAttribute("user", new User());

        if (admin.getBranch() != null) {
            model.addAttribute("branches", Arrays.asList(admin.getBranch()));
        } else {
            model.addAttribute("branches", branchService.getAllBranches());
        }

        model.addAttribute("roles", Role.values());
        model.addAttribute("isEdit", false);
        return "admin/user-form";
    }

    @PostMapping("/users/upload")
    public String uploadUsers(Authentication auth,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            RedirectAttributes redirect) {
        User admin = getCurrentAdmin(auth);
        try {
            UserService.BulkUserUploadResult result = userService.uploadUsersFromCsv(file, admin);
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
        writer.println("fullName,email,erpId,rollNumber,role,branch,password");
        writer.println("Student One,student1cs@niet.co.in,0231cs013,2301330120117,STUDENT,CS,Niet@123");
        writer.println("Student Two,student2cs@niet.co.in,0231cs014,2301330120118,STUDENT,CS,Niet@123");
        writer.println("Faculty CS,facultycs@niet.co.in,facultycs001,CSFAC001,FACULTY,CS,Niet@123");
        writer.println("HOD CS,hodcs@niet.co.in,hodcs001,CSHOD001,HOD,CS,Niet@123");
        writer.println("Admin User,admin@pas.com,,,ADMIN,,Niet@123");
        writer.flush();
    }

    @PostMapping("/users/new")
    public String createUser(Authentication auth, @RequestParam String fullName, @RequestParam String email,
            @RequestParam String password, @RequestParam Role role,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) String erpId,
            @RequestParam(required = false) String rollNumber,
            RedirectAttributes redirect) {
        User admin = getCurrentAdmin(auth);
        try {
            Branch branch = null;
            if (branchId != null) {
                branch = branchService.getBranchById(branchId)
                        .orElseThrow(() -> new IllegalArgumentException("Branch not found"));
            }
            if (admin.getBranch() != null) {
                branch = admin.getBranch();
            }
            userService.createUser(fullName, email, password, role, branch, erpId, rollNumber);
            redirect.addFlashAttribute("success", "User created successfully");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/users/edit/{id}")
    public String editUserForm(Authentication auth, @PathVariable Long id, Model model) {
        User admin = getCurrentAdmin(auth);
        User user = userService.getUserById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        enforceAdminBranchAccess(admin, user.getBranch());

        model.addAttribute("user", user);

        if (admin.getBranch() != null) {
            model.addAttribute("branches", Arrays.asList(admin.getBranch()));
        } else {
            model.addAttribute("branches", branchService.getAllBranches());
        }

        model.addAttribute("roles", Role.values());
        model.addAttribute("isEdit", true);
        return "admin/user-form";
    }

    @PostMapping("/users/edit/{id}")
    public String updateUser(Authentication auth, @PathVariable Long id, @RequestParam String fullName,
            @RequestParam String email, @RequestParam Role role,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) String erpId,
            @RequestParam(required = false) String rollNumber,
            RedirectAttributes redirect) {
        User admin = getCurrentAdmin(auth);
        try {
            User existingUser = userService.getUserById(id)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            enforceAdminBranchAccess(admin, existingUser.getBranch());

            Branch branch = null;
            if (branchId != null) {
                branch = branchService.getBranchById(branchId)
                        .orElseThrow(() -> new IllegalArgumentException("Branch not found"));
            }
            if (admin.getBranch() != null) {
                branch = admin.getBranch();
            }

            userService.updateUser(id, fullName, email, role, branch, erpId, rollNumber);
            redirect.addFlashAttribute("success", "User updated successfully");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/toggle")
    public String toggleUser(Authentication auth, @PathVariable Long id, RedirectAttributes redirect) {
        User admin = getCurrentAdmin(auth);
        try {
            User existingUser = userService.getUserById(id)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            enforceAdminBranchAccess(admin, existingUser.getBranch());

            userService.toggleEnabled(id);
            redirect.addFlashAttribute("success", "User status updated");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/reset-password")
    public String resetPassword(Authentication auth, @PathVariable Long id, @RequestParam String newPassword,
            RedirectAttributes redirect) {
        User admin = getCurrentAdmin(auth);
        try {
            User existingUser = userService.getUserById(id)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            enforceAdminBranchAccess(admin, existingUser.getBranch());

            userService.updatePassword(id, newPassword);
            redirect.addFlashAttribute("success", "Password reset successfully");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    // ==================== Delete Operations ====================

    @PostMapping("/branches/{id}/delete")
    public String deleteBranch(Authentication auth, @PathVariable Long id, RedirectAttributes redirect) {
        User admin = getCurrentAdmin(auth);
        enforceGlobalAdmin(admin);

        try {
            branchService.deleteBranch(id);
            redirect.addFlashAttribute("success", "Branch deleted successfully");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", "Cannot delete branch: " + e.getMessage());
        }
        return "redirect:/admin/branches";
    }

    @PostMapping("/users/{id}/delete")
    public String deleteUser(Authentication auth, @PathVariable Long id, RedirectAttributes redirect) {
        User admin = getCurrentAdmin(auth);
        try {
            User existingUser = userService.getUserById(id)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            enforceAdminBranchAccess(admin, existingUser.getBranch());

            userService.deleteUser(id);
            redirect.addFlashAttribute("success", "User deleted successfully");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", "Cannot delete user: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    // ==================== Project Management ====================

    @GetMapping("/projects")
    public String listProjects(
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) ProjectStatus status,
            @RequestParam(defaultValue = "createdAt,desc") String[] sort,
            Model model) {
        User admin = getCurrentAdmin(auth);

        Branch filterBranch = null;
        if (admin.getBranch() != null) {
            filterBranch = admin.getBranch();
        }

        Page<com.project.pas.model.Project> projectPage = projectService.searchProjects(
                keyword, filterBranch, null, status, null, PageRequest.of(page, size, parseSort(sort)));
        model.addAttribute("page", projectPage);
        model.addAttribute("statusOptions", Arrays.asList(ProjectStatus.values()));
        return "admin/projects";
    }

    @PostMapping("/projects/{id}/delete")
    public String deleteProject(Authentication auth, @PathVariable Long id, RedirectAttributes redirect) {
        User admin = getCurrentAdmin(auth);
        try {
            com.project.pas.model.Project project = projectService.getProjectById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Project not found"));
            enforceAdminBranchAccess(admin, project.getBranch());

            projectService.adminDeleteProject(id);
            redirect.addFlashAttribute("success", "Project deleted successfully");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", "Cannot delete project: " + e.getMessage());
        }
        return "redirect:/admin/projects";
    }

    private Sort parseSort(String[] sort) {
        if (sort != null && sort.length >= 2) {
            return Sort.by(Sort.Direction.fromString(sort[1]), sort[0]);
        } else if (sort != null && sort.length == 1 && sort[0].contains(",")) {
            String[] parts = sort[0].split(",");
            return Sort.by(Sort.Direction.fromString(parts[1]), parts[0]);
        }
        return Sort.by(Sort.Direction.DESC, "createdAt");
    }
}
