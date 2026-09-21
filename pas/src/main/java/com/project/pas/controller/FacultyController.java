package com.project.pas.controller;

import com.project.pas.model.*;
import com.project.pas.service.*;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping("/faculty")
public class FacultyController {

    private final ProjectService projectService;
    private final ProjectTopicService topicService;
    private final UserService userService;
    private final GuideSelectionService guideSelectionService;
    private final FileStorageService fileStorageService;

    public FacultyController(ProjectService projectService, ProjectTopicService topicService,
            UserService userService, GuideSelectionService guideSelectionService,
            FileStorageService fileStorageService) {
        this.projectService = projectService;
        this.topicService = topicService;
        this.userService = userService;
        this.guideSelectionService = guideSelectionService;
        this.fileStorageService = fileStorageService;
    }

    private User getCurrentUser(Authentication auth) {
        return userService.getUserByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication auth, Model model) {
        User faculty = getCurrentUser(auth);
        Branch branch = faculty.getBranch();

        // Pending idea and proposal reviews — scoped to THIS faculty's guided students
        // only
        List<Project> pendingIdeas = projectService.getProjectsByFacultyGuideAndStatuses(faculty,
                List.of(ProjectStatus.PROJECT_IDEA_PENDING_FACULTY, ProjectStatus.PROPOSAL_PENDING_FACULTY));

        // Pending project reviews — scoped to THIS faculty's guided students only
        List<Project> pendingReviews = projectService.getProjectsByFacultyGuideAndStatuses(faculty,
                List.of(ProjectStatus.PENDING_FACULTY_REVIEW));

        // Stats: count only projects guided by this faculty
        long totalProjects = projectService.countByFacultyGuide(faculty);
        long completedProjects = projectService.countByFacultyGuideAndStatus(faculty, ProjectStatus.COMPLETED);
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
    public String listProjects(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) ProjectStatus status,
            @RequestParam(defaultValue = "createdAt,desc") String[] sort,
            Authentication auth, Model model) {
        User faculty = getCurrentUser(auth);
        Page<Project> projectPage = projectService.searchProjects(
                keyword, null, faculty, status, null, PageRequest.of(page, size, parseSort(sort)));
        model.addAttribute("page", projectPage);
        model.addAttribute("statusOptions", Arrays.asList(ProjectStatus.values()));
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

        // Guide enforcement: faculty may only view projects where they are the assigned
        // guide
        if (project.getFacultyGuide() == null ||
                !project.getFacultyGuide().getId().equals(faculty.getId())) {
            redirect.addFlashAttribute("error", "Access denied: you are not the assigned guide for this project");
            return "redirect:/faculty/projects";
        }

        List<ApprovalHistory> history = projectService.getApprovalHistory(id);
        List<ProjectTeamMember> teamMembers = projectService.getTeamMembers(project);

        model.addAttribute("project", project);
        model.addAttribute("history", history);
        model.addAttribute("teamMembers", teamMembers);
        model.addAttribute("faculty", faculty);
        return "faculty/project-review";
    }

    // ==================== Idea Review ====================

    @PostMapping("/projects/{id}/approve-idea")
    public String approveIdea(Authentication auth, @PathVariable Long id,
            @RequestParam(required = false) String comments,
            @RequestParam(required = false) Integer minTeamSize,
            @RequestParam(required = false) Integer maxTeamSize,
            RedirectAttributes redirect) {
        User faculty = getCurrentUser(auth);
        try {
            Project project = projectService.getProjectById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Project not found"));
            projectService.approveIdea(project, faculty, comments, minTeamSize, maxTeamSize);
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

    // ==================== Proposal Review (Stage 1) ====================

    @PostMapping("/projects/{id}/approve-proposal")
    public String approveProposal(Authentication auth, @PathVariable Long id,
            @RequestParam(required = false) String comments,
            RedirectAttributes redirect) {
        User faculty = getCurrentUser(auth);
        try {
            Project project = projectService.getProjectById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Project not found"));
            projectService.approveProposal(project, faculty, comments);
            redirect.addFlashAttribute("success", "Project proposal approved! Student can now start working.");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/faculty/projects/" + id;
    }

    @PostMapping("/projects/{id}/reject-proposal")
    public String rejectProposal(Authentication auth, @PathVariable Long id,
            @RequestParam(required = false) String comments,
            @RequestParam String rejectionReason,
            RedirectAttributes redirect) {
        User faculty = getCurrentUser(auth);
        try {
            Project project = projectService.getProjectById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Project not found"));
            projectService.rejectProposal(project, faculty, comments, rejectionReason);
            redirect.addFlashAttribute("success", "Project proposal rejected. Student will be notified to revise it.");
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
    public String listTopics(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) TopicStatus status,
            @RequestParam(defaultValue = "createdAt,desc") String[] sort,
            Authentication auth, Model model) {
        User faculty = getCurrentUser(auth);
        Page<ProjectTopic> topicPage = topicService.searchTopics(
                keyword, faculty.getBranch(), status, PageRequest.of(page, size, parseSort(sort)));
        model.addAttribute("page", topicPage);
        model.addAttribute("statusOptions", Arrays.asList(TopicStatus.values()));
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
            @RequestParam(defaultValue = "1") int minTeamSize,
            @RequestParam(defaultValue = "4") int maxTeamSize,
            RedirectAttributes redirect) {
        User faculty = getCurrentUser(auth);
        try {
            topicService.createTopic(title, description, category, techStack, minTeamSize, maxTeamSize, faculty);
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

    @PostMapping("/topics/{id}/delete")
    public String deleteTopic(Authentication auth, @PathVariable Long id, RedirectAttributes redirect) {
        User faculty = getCurrentUser(auth);
        try {
            topicService.deleteTopic(id, faculty);
            redirect.addFlashAttribute("success", "Topic permanently deleted");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/faculty/topics";
    }

    @PostMapping("/topics/{id}/restore")
    public String restoreTopic(Authentication auth, @PathVariable Long id, RedirectAttributes redirect) {
        User faculty = getCurrentUser(auth);
        try {
            topicService.restoreTopic(id, faculty);
            redirect.addFlashAttribute("success", "Topic restored and is now available");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/faculty/topics";
    }

    @GetMapping("/topics/{id}/edit")
    public String editTopicForm(Authentication auth, @PathVariable Long id, Model model, RedirectAttributes redirect) {
        User faculty = getCurrentUser(auth);
        try {
            ProjectTopic topic = topicService.getTopicById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Topic not found"));

            // Enforce AVAILABLE status
            if (topic.getStatus() != TopicStatus.AVAILABLE) {
                redirect.addFlashAttribute("error",
                        "Cannot edit topic: topic is currently '" + topic.getStatus().getDisplayName() +
                                "'. Only topics with 'Available' status can be edited.");
                return "redirect:/faculty/topics";
            }

            model.addAttribute("faculty", faculty);
            model.addAttribute("topic", topic);
            model.addAttribute("isEdit", true);
            return "faculty/topic-form";
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/faculty/topics";
        }
    }

    @PostMapping("/topics/{id}/edit")
    public String updateTopic(Authentication auth, @PathVariable Long id,
            @RequestParam String title, @RequestParam String description,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String techStack,
            @RequestParam(defaultValue = "1") int minTeamSize,
            @RequestParam(defaultValue = "4") int maxTeamSize,
            RedirectAttributes redirect) {
        User faculty = getCurrentUser(auth);
        try {
            topicService.updateTopic(id, title, description, category, techStack, minTeamSize, maxTeamSize, faculty);
            redirect.addFlashAttribute("success", "Topic updated successfully!");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/faculty/topics";
    }

    // ==================== File Download ====================

    @GetMapping("/projects/{id}/download/{type}")
    public ResponseEntity<Resource> downloadFile(Authentication auth, @PathVariable Long id,
            @PathVariable String type) {
        User faculty = getCurrentUser(auth);
        Project project = projectService.getProjectById(id)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));

        // Branch enforcement
        if (!project.getBranch().getId().equals(faculty.getBranch().getId())) {
            return ResponseEntity.status(403).build();
        }

        // Guide enforcement: only the assigned faculty guide can download
        if (project.getFacultyGuide() != null &&
                !project.getFacultyGuide().getId().equals(faculty.getId())) {
            return ResponseEntity.status(403).build();
        }

        String filePath;
        if ("report".equals(type)) {
            filePath = project.getReportFilePath();
        } else if ("source".equals(type)) {
            filePath = project.getProjectFilePath();
        } else if ("ppt".equals(type)) {
            filePath = project.getPptFilePath();
        } else {
            return ResponseEntity.badRequest().build();
        }

        if (filePath == null || filePath.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        try {
            Path path = fileStorageService.getFilePath(filePath);
            Resource resource = new UrlResource(path.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            String filename = path.getFileName().toString();
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + filename + "\"")
                    .body(resource);
        } catch (MalformedURLException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // ==================== My Students (Guide Assignments) ====================

    @GetMapping("/my-students")
    public String myStudents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "assignedAt,desc") String[] sort,
            Authentication auth, Model model) {
        User faculty = getCurrentUser(auth);

        Page<GuideAssignment> assignmentPage = guideSelectionService.searchGuideAssignments(
                keyword, faculty.getBranch(), faculty, PageRequest.of(page, size, parseSort(sort)));

        var assignments = guideSelectionService.getAssignedStudents(faculty); // Need this for unpaged count
        var activeForm = guideSelectionService.getActiveForm(faculty.getBranch());
        boolean isActive = guideSelectionService.isSelectionActive(faculty.getBranch());

        model.addAttribute("faculty", faculty);
        model.addAttribute("page", assignmentPage);
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
