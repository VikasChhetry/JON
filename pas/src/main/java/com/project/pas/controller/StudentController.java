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
            redirect.addFlashAttribute("error",
                    "You already have an active project. Complete it before starting a new one.");
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
            redirect.addFlashAttribute("error",
                    "You already have an active project. Complete it before selecting a new topic.");
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

    // ==================== Stage 1: Submit Proposal ====================

    @GetMapping("/project/{id}/proposal")
    public String proposalForm(Authentication auth, @PathVariable Long id, Model model, RedirectAttributes redirect) {
        User student = getCurrentUser(auth);
        Project project = projectService.getProjectById(id)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));

        if (!project.getStudent().getId().equals(student.getId())) {
            redirect.addFlashAttribute("error", "Access denied");
            return "redirect:/student/dashboard";
        }

        // Only allow proposal when idea is approved or topic is selected
        if (project.getStatus() != ProjectStatus.PROJECT_IDEA_APPROVED &&
                project.getStatus() != ProjectStatus.TOPIC_SELECTED) {
            redirect.addFlashAttribute("error",
                    "Cannot submit proposal in current status: " + project.getStatus().getDisplayName());
            return "redirect:/student/project/" + id;
        }

        model.addAttribute("project", project);
        model.addAttribute("student", student);
        return "student/project-proposal";
    }

    @PostMapping("/project/{id}/proposal")
    public String submitProposal(Authentication auth, @PathVariable Long id,
            @RequestParam String synopsis,
            @RequestParam(required = false) MultipartFile pptFile,
            @RequestParam String problemStatement,
            @RequestParam String objectives,
            @RequestParam(required = false) String literatureReview,
            @RequestParam String methodology,
            @RequestParam String systemDesign,
            @RequestParam(required = false) String futureWork,
            RedirectAttributes redirect) {
        User student = getCurrentUser(auth);
        try {
            Project project = projectService.getProjectById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Project not found"));

            String pptFilePath = null;
            if (pptFile != null && !pptFile.isEmpty()) {
                pptFilePath = fileStorageService.storeFile(pptFile, "proposals/" + id);
            }

            projectService.submitProposal(project, student, synopsis, pptFilePath,
                    problemStatement, objectives, literatureReview, methodology,
                    systemDesign, futureWork);
            redirect.addFlashAttribute("success", "Project proposal submitted for faculty review!");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/student/project/" + id;
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

    // ==================== Stage 2: Submit Final Project ====================

    @GetMapping("/project/{id}/submit")
    public String submitForm(Authentication auth, @PathVariable Long id, Model model, RedirectAttributes redirect) {
        User student = getCurrentUser(auth);
        Project project = projectService.getProjectById(id)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));

        if (!project.getStudent().getId().equals(student.getId())) {
            redirect.addFlashAttribute("error", "Access denied");
            return "redirect:/student/dashboard";
        }

        // Backend enforcement: only allow final submission from STUDENT_WORKING
        if (project.getStatus() != ProjectStatus.STUDENT_WORKING) {
            redirect.addFlashAttribute("error",
                    "Cannot submit final project in current status. Your proposal must be approved and you must start working first.");
            return "redirect:/student/project/" + id;
        }

        model.addAttribute("project", project);
        model.addAttribute("student", student);
        return "student/project-submit";
    }

    @PostMapping("/project/{id}/submit")
    public String submitProject(Authentication auth, @PathVariable Long id,
            @RequestParam(required = false) MultipartFile reportFile,
            @RequestParam(required = false) MultipartFile projectFile,
            @RequestParam(required = false) String githubUrl,
            @RequestParam(required = false) String videoUrl,
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

            projectService.submitForReview(project, student, reportPath, projectPath, githubUrl, videoUrl);
            redirect.addFlashAttribute("success", "Final project submitted for faculty review!");
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

        boolean isLocked = projectService.isProjectDetailsLocked(id);

        model.addAttribute("project", project);
        model.addAttribute("history", history);
        model.addAttribute("student", student);
        model.addAttribute("isLocked", isLocked);
        return "student/project-edit";
    }

    @PostMapping("/project/{id}/edit")
    public String editAndResubmit(Authentication auth, @PathVariable Long id,
            // NOTE: title, description, category, techStack are intentionally NOT accepted
            // here.
            // Core project details are read-only during resubmission and must never be
            // updated.
            @RequestParam(required = false) String comments,
            @RequestParam(required = false) MultipartFile reportFile,
            @RequestParam(required = false) MultipartFile projectFile,
            @RequestParam(required = false) String githubUrl,
            @RequestParam(required = false) String videoUrl,
            // Proposal fields for resubmission
            @RequestParam(required = false) String synopsis,
            @RequestParam(required = false) MultipartFile pptFile,
            @RequestParam(required = false) String problemStatement,
            @RequestParam(required = false) String objectives,
            @RequestParam(required = false) String literatureReview,
            @RequestParam(required = false) String methodology,
            @RequestParam(required = false) String systemDesign,
            @RequestParam(required = false) String futureWork,
            RedirectAttributes redirect) {
        User student = getCurrentUser(auth);
        try {
            Project project = projectService.getProjectById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Project not found"));

            // Core project details (title, description, category, techStack) are NOT
            // updated here.
            // They are read-only during resubmission - only proposal/file corrections are
            // allowed.

            // If rejected at proposal stage, also update proposal fields
            if (project.getRejectedAtStage() == ProjectStatus.PROPOSAL_PENDING_FACULTY) {
                String pptFilePath = null;
                if (pptFile != null && !pptFile.isEmpty()) {
                    pptFilePath = fileStorageService.storeFile(pptFile, "proposals/" + id);
                }
                projectService.editProposal(project, student, synopsis, pptFilePath,
                        problemStatement, objectives, literatureReview, methodology,
                        systemDesign, futureWork);
            }

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
            projectService.resubmit(project, student, reportPath, projectPath, githubUrl, videoUrl, comments);
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
