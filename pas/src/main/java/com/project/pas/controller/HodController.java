package com.project.pas.controller;

import com.project.pas.model.*;
import com.project.pas.service.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/hod")
public class HodController {

    private final ProjectService projectService;
    private final ProjectTopicService topicService;
    private final UserService userService;
    private final GuideSelectionService guideSelectionService;

    public HodController(ProjectService projectService, ProjectTopicService topicService,
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
        User hod = getCurrentUser(auth);
        Branch branch = hod.getBranch();

        List<Project> pendingReviews = projectService.getProjectsByBranchAndStatuses(branch,
                List.of(ProjectStatus.PENDING_HOD_REVIEW));

        long totalProjects = projectService.countByBranch(branch);
        long completedProjects = projectService.countByBranchAndStatus(branch, ProjectStatus.COMPLETED);
        long topicCount = topicService.countByBranch(branch);
        long studentCount = userService.getUsersByBranchAndRole(branch, Role.STUDENT).size();
        long facultyCount = userService.getUsersByBranchAndRole(branch, Role.FACULTY).size();

        // Guide selection info
        Optional<GuideSelectionForm> activeForm = guideSelectionService.getActiveForm(branch);
        long assignedStudents = guideSelectionService.getAssignmentsByBranch(branch).size();

        model.addAttribute("hod", hod);
        model.addAttribute("pendingReviews", pendingReviews);
        model.addAttribute("totalProjects", totalProjects);
        model.addAttribute("completedProjects", completedProjects);
        model.addAttribute("topicCount", topicCount);
        model.addAttribute("studentCount", studentCount);
        model.addAttribute("facultyCount", facultyCount);
        model.addAttribute("activeForm", activeForm.orElse(null));
        model.addAttribute("assignedStudents", assignedStudents);

        return "hod/dashboard";
    }

    // ==================== Project Management ====================

    @GetMapping("/projects")
    public String listProjects(Authentication auth, Model model) {
        User hod = getCurrentUser(auth);
        List<Project> projects = projectService.getProjectsByBranch(hod.getBranch());
        model.addAttribute("projects", projects);
        model.addAttribute("hod", hod);
        return "hod/projects";
    }

    @GetMapping("/projects/{id}")
    public String reviewProject(Authentication auth, @PathVariable Long id, Model model, RedirectAttributes redirect) {
        User hod = getCurrentUser(auth);
        Project project = projectService.getProjectById(id)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));

        if (!project.getBranch().getId().equals(hod.getBranch().getId())) {
            redirect.addFlashAttribute("error", "Access denied: project belongs to another branch");
            return "redirect:/hod/dashboard";
        }

        List<ApprovalHistory> history = projectService.getApprovalHistory(id);
        model.addAttribute("project", project);
        model.addAttribute("history", history);
        model.addAttribute("hod", hod);
        return "hod/project-review";
    }

    @PostMapping("/projects/{id}/approve")
    public String approveProject(Authentication auth, @PathVariable Long id,
                                  @RequestParam(required = false) String comments,
                                  RedirectAttributes redirect) {
        User hod = getCurrentUser(auth);
        try {
            Project project = projectService.getProjectById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Project not found"));
            projectService.hodApprove(project, hod, comments);
            redirect.addFlashAttribute("success", "Project approved and marked as COMPLETED!");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/hod/projects/" + id;
    }

    @PostMapping("/projects/{id}/reject")
    public String rejectProject(Authentication auth, @PathVariable Long id,
                                 @RequestParam(required = false) String comments,
                                 @RequestParam String rejectionReason,
                                 RedirectAttributes redirect) {
        User hod = getCurrentUser(auth);
        try {
            Project project = projectService.getProjectById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Project not found"));
            projectService.hodReject(project, hod, comments, rejectionReason);
            redirect.addFlashAttribute("success", "Project rejected. Student will be notified.");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/hod/projects/" + id;
    }

    // ==================== Topic Management ====================

    @GetMapping("/topics")
    public String listTopics(Authentication auth, Model model) {
        User hod = getCurrentUser(auth);
        List<ProjectTopic> topics = topicService.getTopicsByBranch(hod.getBranch());
        model.addAttribute("topics", topics);
        model.addAttribute("hod", hod);
        return "hod/topics";
    }

    @GetMapping("/topics/new")
    public String newTopicForm(Authentication auth, Model model) {
        User hod = getCurrentUser(auth);
        model.addAttribute("hod", hod);
        return "hod/topic-form";
    }

    @PostMapping("/topics/new")
    public String createTopic(Authentication auth,
                               @RequestParam String title, @RequestParam String description,
                               @RequestParam(required = false) String category,
                               @RequestParam(required = false) String techStack,
                               RedirectAttributes redirect) {
        User hod = getCurrentUser(auth);
        try {
            topicService.createTopic(title, description, category, techStack, hod);
            redirect.addFlashAttribute("success", "Topic created successfully!");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/hod/topics";
    }

    @PostMapping("/topics/upload")
    public String uploadTopics(Authentication auth, @RequestParam MultipartFile file,
                                RedirectAttributes redirect) {
        User hod = getCurrentUser(auth);
        try {
            List<ProjectTopic> topics = topicService.uploadTopicsFromCsv(file, hod);
            redirect.addFlashAttribute("success", topics.size() + " topics uploaded successfully!");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", "Upload failed: " + e.getMessage());
        }
        return "redirect:/hod/topics";
    }

    @PostMapping("/topics/{id}/archive")
    public String archiveTopic(Authentication auth, @PathVariable Long id, RedirectAttributes redirect) {
        User hod = getCurrentUser(auth);
        try {
            topicService.archiveTopic(id, hod);
            redirect.addFlashAttribute("success", "Topic archived");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/hod/topics";
    }

    // ==================== Guide Selection Management ====================

    @GetMapping("/guide-selection")
    public String guideSelectionDashboard(Authentication auth, Model model) {
        User hod = getCurrentUser(auth);
        Branch branch = hod.getBranch();

        Optional<GuideSelectionForm> activeForm = guideSelectionService.getActiveForm(branch);
        List<Map<String, Object>> facultyInfo = guideSelectionService.getFacultyCapacityInfo(branch);
        List<GuideAssignment> assignments = guideSelectionService.getAssignmentsByBranch(branch);
        List<User> students = userService.getUsersByBranchAndRole(branch, Role.STUDENT);
        long unassignedCount = students.stream()
                .filter(s -> guideSelectionService.getStudentAssignment(s).isEmpty())
                .count();

        model.addAttribute("hod", hod);
        model.addAttribute("activeForm", activeForm.orElse(null));
        model.addAttribute("facultyInfo", facultyInfo);
        model.addAttribute("assignments", assignments);
        model.addAttribute("totalStudents", students.size());
        model.addAttribute("assignedCount", assignments.size());
        model.addAttribute("unassignedCount", unassignedCount);
        model.addAttribute("isActive", guideSelectionService.isSelectionActive(branch));
        model.addAttribute("isEnded", guideSelectionService.isSelectionEnded(branch));

        return "hod/guide-selection";
    }

    @GetMapping("/guide-selection/form")
    public String guideFormPage(Authentication auth, Model model) {
        User hod = getCurrentUser(auth);
        Optional<GuideSelectionForm> activeForm = guideSelectionService.getActiveForm(hod.getBranch());
        List<Map<String, Object>> facultyInfo = guideSelectionService.getFacultyCapacityInfo(hod.getBranch());

        model.addAttribute("hod", hod);
        model.addAttribute("activeForm", activeForm.orElse(null));
        model.addAttribute("facultyInfo", facultyInfo);
        model.addAttribute("isEdit", activeForm.isPresent());

        return "hod/guide-form";
    }

    @PostMapping("/guide-selection/form")
    public String saveGuideForm(Authentication auth,
                                 @RequestParam String startDateTime,
                                 @RequestParam String endDateTime,
                                 RedirectAttributes redirect) {
        User hod = getCurrentUser(auth);
        try {
            LocalDateTime start = LocalDateTime.parse(startDateTime);
            LocalDateTime end = LocalDateTime.parse(endDateTime);
            guideSelectionService.createSelectionForm(hod, start, end);
            redirect.addFlashAttribute("success", "Guide selection form created successfully!");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/hod/guide-selection";
    }

    @PostMapping("/guide-selection/form/{id}/update")
    public String updateGuideForm(Authentication auth, @PathVariable Long id,
                                   @RequestParam String startDateTime,
                                   @RequestParam String endDateTime,
                                   RedirectAttributes redirect) {
        User hod = getCurrentUser(auth);
        try {
            LocalDateTime start = LocalDateTime.parse(startDateTime);
            LocalDateTime end = LocalDateTime.parse(endDateTime);
            guideSelectionService.updateSelectionForm(hod, id, start, end);
            redirect.addFlashAttribute("success", "Guide selection form updated!");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/hod/guide-selection";
    }

    @PostMapping("/guide-selection/faculty/{id}/capacity")
    public String setFacultyCapacity(Authentication auth, @PathVariable Long id,
                                      @RequestParam int maxCapacity,
                                      RedirectAttributes redirect) {
        User hod = getCurrentUser(auth);
        try {
            guideSelectionService.updateFacultyCapacity(hod, id, maxCapacity);
            redirect.addFlashAttribute("success", "Faculty capacity updated!");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/hod/guide-selection/form";
    }

    @GetMapping("/guide-selection/assignments")
    public String guideAssignments(Authentication auth, Model model) {
        User hod = getCurrentUser(auth);
        Branch branch = hod.getBranch();

        List<GuideAssignment> assignments = guideSelectionService.getAssignmentsByBranch(branch);
        List<User> students = userService.getUsersByBranchAndRole(branch, Role.STUDENT);
        List<User> faculty = userService.getUsersByBranchAndRole(branch, Role.FACULTY);
        List<User> unassignedStudents = students.stream()
                .filter(s -> guideSelectionService.getStudentAssignment(s).isEmpty())
                .toList();

        model.addAttribute("hod", hod);
        model.addAttribute("assignments", assignments);
        model.addAttribute("unassignedStudents", unassignedStudents);
        model.addAttribute("faculty", faculty);

        return "hod/guide-assignments";
    }

    @PostMapping("/guide-selection/assign")
    public String hodAssign(Authentication auth,
                             @RequestParam Long studentId,
                             @RequestParam Long facultyId,
                             @RequestParam String reason,
                             RedirectAttributes redirect) {
        User hod = getCurrentUser(auth);
        try {
            guideSelectionService.hodAssignStudent(hod, studentId, facultyId, reason);
            redirect.addFlashAttribute("success", "Student assigned to faculty successfully!");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/hod/guide-selection/assignments";
    }

    @PostMapping("/guide-selection/remove/{studentId}")
    public String hodRemove(Authentication auth, @PathVariable Long studentId,
                             @RequestParam String reason,
                             RedirectAttributes redirect) {
        User hod = getCurrentUser(auth);
        try {
            guideSelectionService.hodRemoveStudent(hod, studentId, reason);
            redirect.addFlashAttribute("success", "Student guide assignment removed!");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/hod/guide-selection/assignments";
    }

    @PostMapping("/guide-selection/reassign")
    public String hodReassign(Authentication auth,
                               @RequestParam Long studentId,
                               @RequestParam Long newFacultyId,
                               @RequestParam String reason,
                               RedirectAttributes redirect) {
        User hod = getCurrentUser(auth);
        try {
            guideSelectionService.hodReassignStudent(hod, studentId, newFacultyId, reason);
            redirect.addFlashAttribute("success", "Student reassigned successfully!");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/hod/guide-selection/assignments";
    }

    @PostMapping("/guide-selection/auto-assign")
    public String autoAssign(Authentication auth, RedirectAttributes redirect) {
        User hod = getCurrentUser(auth);
        try {
            int count = guideSelectionService.autoAssignUnassignedStudents(hod);
            if (count > 0) {
                redirect.addFlashAttribute("success", count + " student(s) auto-assigned to faculty!");
            } else {
                redirect.addFlashAttribute("info", "No unassigned students or no available faculty capacity.");
            }
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/hod/guide-selection/assignments";
    }

    @GetMapping("/guide-selection/history")
    public String guideHistory(Authentication auth, Model model) {
        User hod = getCurrentUser(auth);
        List<GuideAssignmentHistory> history = guideSelectionService.getHistory(hod.getBranch());
        model.addAttribute("hod", hod);
        model.addAttribute("history", history);
        return "hod/guide-history";
    }
}
