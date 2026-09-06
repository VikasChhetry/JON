package com.project.pas.controller;

import com.project.pas.model.*;
import com.project.pas.service.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping("/faculty")
public class FacultyController {

    private final ProjectService projectService;
    private final ProjectTopicService topicService;
    private final UserService userService;
    private final GuideSelectionService guideSelectionService;

    public FacultyController(ProjectService projectService, ProjectTopicService topicService,
                              UserService userService, GuideSelectionService guideSelectionService) {
        this.projectService = projectService;
        this.topicService = topicService;
        this.userService = userService;
        this.guideSelectionService = guideSelectionService;
    }

    private User getCurrentUser(Authentication auth) {
        return userService.getUserByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication auth, Model model) {
        User faculty = getCurrentUser(auth);
        Branch branch = faculty.getBranch();

        // Pending idea reviews
        List<Project> pendingIdeas = projectService.getProjectsByBranchAndStatuses(branch,
                List.of(ProjectStatus.PROJECT_IDEA_PENDING_FACULTY));

        // Pending project reviews
        List<Project> pendingReviews = projectService.getProjectsByBranchAndStatuses(branch,
                List.of(ProjectStatus.PENDING_FACULTY_REVIEW));

        long totalProjects = projectService.countByBranch(branch);
        long completedProjects = projectService.countByBranchAndStatus(branch, ProjectStatus.COMPLETED);
        long topicCount = topicService.countByBranch(branch);

        model.addAttribute("faculty", faculty);
        model.addAttribute("pendingIdeas", pendingIdeas);
        model.addAttribute("pendingReviews", pendingReviews);
        model.addAttribute("totalProjects", totalProjects);
        model.addAttribute("completedProjects", completedProjects);
        model.addAttribute("topicCount", topicCount);

        // Guide info
        var assignedStudents = guideSelectionService.getAssignedStudents(faculty);
        model.addAttribute("guidedStudentCount", assignedStudents.size());
        model.addAttribute("maxCapacity", faculty.getMaxGuidingCapacity());
        model.addAttribute("vacancies", Math.max(0, faculty.getMaxGuidingCapacity() - assignedStudents.size()));

        return "faculty/dashboard";
    }

    // ==================== Project Management ====================

    @GetMapping("/projects")
    public String listProjects(Authentication auth, Model model) {
        User faculty = getCurrentUser(auth);
        List<Project> projects = projectService.getProjectsByBranch(faculty.getBranch());
        model.addAttribute("projects", projects);
        model.addAttribute("faculty", faculty);
        return "faculty/projects";
    }

    @GetMapping("/projects/{id}")
    public String reviewProject(Authentication auth, @PathVariable Long id, Model model, RedirectAttributes redirect) {
        User faculty = getCurrentUser(auth);
        Project project = projectService.getProjectById(id)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));

        // Branch enforcement
        if (!project.getBranch().getId().equals(faculty.getBranch().getId())) {
            redirect.addFlashAttribute("error", "Access denied: project belongs to another branch");
            return "redirect:/faculty/dashboard";
        }

        List<ApprovalHistory> history = projectService.getApprovalHistory(id);

        model.addAttribute("project", project);
        model.addAttribute("history", history);
        model.addAttribute("faculty", faculty);
        return "faculty/project-review";
    }

    // ==================== Idea Review ====================

    @PostMapping("/projects/{id}/approve-idea")
    public String approveIdea(Authentication auth, @PathVariable Long id,
                               @RequestParam(required = false) String comments,
                               RedirectAttributes redirect) {
        User faculty = getCurrentUser(auth);
        try {
            Project project = projectService.getProjectById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Project not found"));
            projectService.approveIdea(project, faculty, comments);
            redirect.addFlashAttribute("success", "Project idea approved!");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/faculty/projects/" + id;
    }

    @PostMapping("/projects/{id}/reject-idea")
    public String rejectIdea(Authentication auth, @PathVariable Long id,
                              @RequestParam(required = false) String comments,
                              @RequestParam String rejectionReason,
                              RedirectAttributes redirect) {
        User faculty = getCurrentUser(auth);
        try {
            Project project = projectService.getProjectById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Project not found"));
            projectService.rejectIdea(project, faculty, comments, rejectionReason);
            redirect.addFlashAttribute("success", "Project idea rejected. Student will be notified.");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/faculty/projects/" + id;
    }

    // ==================== Project Review ====================

    @PostMapping("/projects/{id}/approve")
    public String approveProject(Authentication auth, @PathVariable Long id,
                                  @RequestParam(required = false) String comments,
                                  RedirectAttributes redirect) {
        User faculty = getCurrentUser(auth);
        try {
            Project project = projectService.getProjectById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Project not found"));
            projectService.facultyApprove(project, faculty, comments);
            redirect.addFlashAttribute("success", "Project approved! Sent to HOD for final review.");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/faculty/projects/" + id;
    }

    @PostMapping("/projects/{id}/reject")
    public String rejectProject(Authentication auth, @PathVariable Long id,
                                 @RequestParam(required = false) String comments,
                                 @RequestParam String rejectionReason,
                                 RedirectAttributes redirect) {
        User faculty = getCurrentUser(auth);
        try {
            Project project = projectService.getProjectById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Project not found"));
            projectService.facultyReject(project, faculty, comments, rejectionReason);
            redirect.addFlashAttribute("success", "Project rejected. Student will be notified.");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/faculty/projects/" + id;
    }

    // ==================== Topic Management ====================

    @GetMapping("/topics")
    public String listTopics(Authentication auth, Model model) {
        User faculty = getCurrentUser(auth);
        List<ProjectTopic> topics = topicService.getTopicsByBranch(faculty.getBranch());
        model.addAttribute("topics", topics);
        model.addAttribute("faculty", faculty);
        return "faculty/topics";
    }

    @GetMapping("/topics/new")
    public String newTopicForm(Authentication auth, Model model) {
        User faculty = getCurrentUser(auth);
        model.addAttribute("faculty", faculty);
        return "faculty/topic-form";
    }

    @PostMapping("/topics/new")
    public String createTopic(Authentication auth,
                               @RequestParam String title, @RequestParam String description,
                               @RequestParam(required = false) String category,
                               @RequestParam(required = false) String techStack,
                               RedirectAttributes redirect) {
        User faculty = getCurrentUser(auth);
        try {
            topicService.createTopic(title, description, category, techStack, faculty);
            redirect.addFlashAttribute("success", "Topic created successfully!");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/faculty/topics";
    }

    @PostMapping("/topics/upload")
    public String uploadTopics(Authentication auth, @RequestParam MultipartFile file,
                                RedirectAttributes redirect) {
        User faculty = getCurrentUser(auth);
        try {
            List<ProjectTopic> topics = topicService.uploadTopicsFromCsv(file, faculty);
            redirect.addFlashAttribute("success", topics.size() + " topics uploaded successfully!");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", "Upload failed: " + e.getMessage());
        }
        return "redirect:/faculty/topics";
    }

    @PostMapping("/topics/{id}/archive")
    public String archiveTopic(Authentication auth, @PathVariable Long id, RedirectAttributes redirect) {
        User faculty = getCurrentUser(auth);
        try {
            topicService.archiveTopic(id, faculty);
            redirect.addFlashAttribute("success", "Topic archived");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/faculty/topics";
    }

    // ==================== My Students (Guide Assignments) ====================

    @GetMapping("/my-students")
    public String myStudents(Authentication auth, Model model) {
        User faculty = getCurrentUser(auth);
        var assignments = guideSelectionService.getAssignedStudents(faculty);
        var activeForm = guideSelectionService.getActiveForm(faculty.getBranch());
        boolean isActive = guideSelectionService.isSelectionActive(faculty.getBranch());

        model.addAttribute("faculty", faculty);
        model.addAttribute("assignments", assignments);
        model.addAttribute("activeForm", activeForm.orElse(null));
        model.addAttribute("isActive", isActive);
        model.addAttribute("maxCapacity", faculty.getMaxGuidingCapacity());
        model.addAttribute("assignedCount", assignments.size());
        model.addAttribute("vacancies", Math.max(0, faculty.getMaxGuidingCapacity() - assignments.size()));

        return "faculty/my-students";
    }

    @PostMapping("/my-students/{studentId}/remove")
    public String removeStudent(Authentication auth, @PathVariable Long studentId,
                                 @RequestParam String reason,
                                 RedirectAttributes redirect) {
        User faculty = getCurrentUser(auth);
        try {
            guideSelectionService.facultyRemoveStudent(faculty, studentId, reason);
            redirect.addFlashAttribute("success", "Student removed from your guidance.");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/faculty/my-students";
    }
}
