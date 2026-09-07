package com.project.pas.controller;

import com.project.pas.model.*;
import com.project.pas.service.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/student")
public class StudentController {

    private final ProjectService projectService;
    private final ProjectTopicService topicService;
    private final UserService userService;
    private final FileStorageService fileStorageService;
    private final GuideSelectionService guideSelectionService;

    public StudentController(ProjectService projectService, ProjectTopicService topicService,
                              UserService userService, FileStorageService fileStorageService,
                              GuideSelectionService guideSelectionService) {
        this.projectService = projectService;
        this.topicService = topicService;
        this.userService = userService;
        this.fileStorageService = fileStorageService;
        this.guideSelectionService = guideSelectionService;
    }

    private User getCurrentUser(Authentication auth) {
        return userService.getUserByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication auth, Model model) {
        User student = getCurrentUser(auth);
        Optional<Project> activeProject = projectService.getActiveProject(student);
        List<Project> allProjects = projectService.getProjectsByStudent(student);

        // Guide selection info
        var guideAssignment = guideSelectionService.getStudentAssignment(student);
        var activeForm = guideSelectionService.getActiveForm(student.getBranch());

        model.addAttribute("student", student);
        model.addAttribute("activeProject", activeProject.orElse(null));
        model.addAttribute("hasActiveProject", activeProject.isPresent());
        model.addAttribute("allProjects", allProjects);
        model.addAttribute("completedCount", allProjects.stream()
                .filter(p -> p.getStatus() == ProjectStatus.COMPLETED).count());
        model.addAttribute("guideAssignment", guideAssignment.orElse(null));
        model.addAttribute("hasGuide", guideAssignment.isPresent());
        model.addAttribute("activeForm", activeForm.orElse(null));

        return "student/dashboard";
    }

    // ==================== Submit Own Project Idea ====================

    @GetMapping("/project/new")
    public String newProjectForm(Authentication auth, Model model, RedirectAttributes redirect) {
        User student = getCurrentUser(auth);
        if (projectService.hasActiveProject(student)) {
            redirect.addFlashAttribute("error", "You already have an active project. Complete it before starting a new one.");
            return "redirect:/student/dashboard";
        }
        model.addAttribute("student", student);
        return "student/project-form";
    }

    @PostMapping("/project/new")
    public String submitProjectIdea(Authentication auth,
                                     @RequestParam String title, @RequestParam String description,
                                     @RequestParam(required = false) String category,
                                     @RequestParam(required = false) String techStack,
                                     RedirectAttributes redirect) {
        User student = getCurrentUser(auth);
        try {
            Project project = projectService.submitOwnIdea(student, title, description, category, techStack);
            redirect.addFlashAttribute("success", "Project idea submitted successfully! Awaiting faculty review.");
            return "redirect:/student/project/" + project.getId();
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/student/dashboard";
        }
    }

    // ==================== Browse & Select Topics ====================

    @GetMapping("/topics")
    public String browseTopics(Authentication auth, Model model, RedirectAttributes redirect) {
        User student = getCurrentUser(auth);
        if (projectService.hasActiveProject(student)) {
            redirect.addFlashAttribute("error", "You already have an active project. Complete it before selecting a new topic.");
            return "redirect:/student/dashboard";
        }
        List<ProjectTopic> topics = topicService.getAvailableTopics(student.getBranch());
        model.addAttribute("topics", topics);
        model.addAttribute("student", student);
        return "student/topics";
    }

    @PostMapping("/topics/{id}/select")
    public String selectTopic(Authentication auth, @PathVariable Long id, RedirectAttributes redirect) {
        User student = getCurrentUser(auth);
        try {
            ProjectTopic topic = topicService.getTopicById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Topic not found"));
            Project project = projectService.selectTopic(student, topic);
            redirect.addFlashAttribute("success", "Topic selected successfully!");
            return "redirect:/student/project/" + project.getId();
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/student/topics";
        }
    }

    // ==================== View Project ====================

    @GetMapping("/project/{id}")
    public String viewProject(Authentication auth, @PathVariable Long id, Model model, RedirectAttributes redirect) {
        User student = getCurrentUser(auth);
        Project project = projectService.getProjectById(id)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));

        // Branch enforcement
        if (!project.getStudent().getId().equals(student.getId())) {
            redirect.addFlashAttribute("error", "Access denied");
            return "redirect:/student/dashboard";
        }

        List<ApprovalHistory> history = projectService.getApprovalHistory(id);

        model.addAttribute("project", project);
        model.addAttribute("history", history);
        model.addAttribute("student", student);
        model.addAttribute("canDelete", projectService.canStudentDelete(project, student));
        return "student/project-detail";
    }

    // ==================== Start Working ====================

    @PostMapping("/project/{id}/start-working")
    public String startWorking(Authentication auth, @PathVariable Long id, RedirectAttributes redirect) {
        User student = getCurrentUser(auth);
        try {
            Project project = projectService.getProjectById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Project not found"));
            projectService.startWorking(project, student);
            redirect.addFlashAttribute("success", "You can now start working on your project!");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/student/project/" + id;
    }

    // ==================== Submit for Review ====================

    @GetMapping("/project/{id}/submit")
    public String submitForm(Authentication auth, @PathVariable Long id, Model model, RedirectAttributes redirect) {
        User student = getCurrentUser(auth);
        Project project = projectService.getProjectById(id)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));

        if (!project.getStudent().getId().equals(student.getId())) {
            redirect.addFlashAttribute("error", "Access denied");
            return "redirect:/student/dashboard";
        }

        model.addAttribute("project", project);
        model.addAttribute("student", student);
        return "student/project-submit";
    }

    @PostMapping("/project/{id}/submit")
    public String submitProject(Authentication auth, @PathVariable Long id,
                                 @RequestParam(required = false) MultipartFile reportFile,
                                 @RequestParam(required = false) MultipartFile projectFile,
                                 RedirectAttributes redirect) {
        User student = getCurrentUser(auth);
        try {
            Project project = projectService.getProjectById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Project not found"));

            String reportPath = null;
            String projectPath = null;
            if (reportFile != null && !reportFile.isEmpty()) {
                reportPath = fileStorageService.storeFile(reportFile, "reports/" + id);
            }
            if (projectFile != null && !projectFile.isEmpty()) {
                projectPath = fileStorageService.storeFile(projectFile, "projects/" + id);
            }

            projectService.submitForReview(project, student, reportPath, projectPath);
            redirect.addFlashAttribute("success", "Project submitted for faculty review!");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/student/project/" + id;
    }

    // ==================== Edit & Resubmit ====================

    @GetMapping("/project/{id}/edit")
    public String editForm(Authentication auth, @PathVariable Long id, Model model, RedirectAttributes redirect) {
        User student = getCurrentUser(auth);
        Project project = projectService.getProjectById(id)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));

        if (!project.getStudent().getId().equals(student.getId())) {
            redirect.addFlashAttribute("error", "Access denied");
            return "redirect:/student/dashboard";
        }

        if (project.getStatus() != ProjectStatus.FACULTY_REJECTED &&
            project.getStatus() != ProjectStatus.HOD_REJECTED) {
            redirect.addFlashAttribute("error", "Project can only be edited when rejected");
            return "redirect:/student/project/" + id;
        }

        List<ApprovalHistory> history = projectService.getApprovalHistory(id);
        model.addAttribute("project", project);
        model.addAttribute("history", history);
        model.addAttribute("student", student);
        return "student/project-edit";
    }

    @PostMapping("/project/{id}/edit")
    public String editAndResubmit(Authentication auth, @PathVariable Long id,
                                    @RequestParam String title, @RequestParam String description,
                                    @RequestParam(required = false) String category,
                                    @RequestParam(required = false) String techStack,
                                    @RequestParam(required = false) String comments,
                                    @RequestParam(required = false) MultipartFile reportFile,
                                    @RequestParam(required = false) MultipartFile projectFile,
                                    RedirectAttributes redirect) {
        User student = getCurrentUser(auth);
        try {
            Project project = projectService.getProjectById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Project not found"));

            // Edit project details
            projectService.editProject(project, student, title, description, category, techStack);

            // Handle file uploads
            String reportPath = null;
            String projectPath = null;
            if (reportFile != null && !reportFile.isEmpty()) {
                reportPath = fileStorageService.storeFile(reportFile, "reports/" + id);
            }
            if (projectFile != null && !projectFile.isEmpty()) {
                projectPath = fileStorageService.storeFile(projectFile, "projects/" + id);
            }

            // Resubmit
            projectService.resubmit(project, student, reportPath, projectPath, comments);
            redirect.addFlashAttribute("success", "Project resubmitted successfully!");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/student/project/" + id;
    }

    // ==================== Guide Selection ====================

    @GetMapping("/guide-selection")
    public String guideSelection(Authentication auth, Model model, RedirectAttributes redirect) {
        User student = getCurrentUser(auth);

        // Check if already assigned
        var existingAssignment = guideSelectionService.getStudentAssignment(student);
        if (existingAssignment.isPresent()) {
            model.addAttribute("assignment", existingAssignment.get());
        }

        var activeForm = guideSelectionService.getActiveForm(student.getBranch());
        var facultyInfo = guideSelectionService.getFacultyCapacityInfo(student.getBranch());

        model.addAttribute("student", student);
        model.addAttribute("activeForm", activeForm.orElse(null));
        model.addAttribute("facultyInfo", facultyInfo);
        model.addAttribute("hasGuide", existingAssignment.isPresent());
        model.addAttribute("isActive", guideSelectionService.isSelectionActive(student.getBranch()));
        model.addAttribute("isEnded", guideSelectionService.isSelectionEnded(student.getBranch()));

        return "student/guide-selection";
    }

    @PostMapping("/guide-selection/select/{facultyId}")
    public String selectGuide(Authentication auth, @PathVariable Long facultyId, RedirectAttributes redirect) {
        User student = getCurrentUser(auth);
        try {
            guideSelectionService.selectGuide(student, facultyId);
            redirect.addFlashAttribute("success", "Faculty guide selected successfully!");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/student/guide-selection";
    }

    @PostMapping("/guide-selection/remove")
    public String removeGuide(Authentication auth, RedirectAttributes redirect) {
        User student = getCurrentUser(auth);
        try {
            guideSelectionService.studentRemoveGuide(student);
            redirect.addFlashAttribute("success", "Guide removed. You can select a new guide.");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/student/guide-selection";
    }

    // ==================== Delete Project ====================

    @PostMapping("/project/{id}/delete")
    public String deleteProject(Authentication auth, @PathVariable Long id, RedirectAttributes redirect) {
        User student = getCurrentUser(auth);
        try {
            Project project = projectService.getProjectById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Project not found"));
            projectService.studentDeleteProject(project, student);
            redirect.addFlashAttribute("success", "Project deleted successfully.");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/student/dashboard";
    }
}
