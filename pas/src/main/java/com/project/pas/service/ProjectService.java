package com.project.pas.service;

import com.project.pas.model.*;
import com.project.pas.repository.ApprovalHistoryRepository;
import com.project.pas.repository.GuideAssignmentRepository;
import com.project.pas.repository.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Core workflow engine implementing the entire project approval state machine.
 * All branch enforcement and business rules are validated here at the service layer.
 */
@Service
@Transactional
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ApprovalHistoryRepository historyRepository;
    private final ProjectTopicService topicService;
    private final GuideAssignmentRepository guideAssignmentRepository;

    // Statuses that indicate a project is "finished" — student can start another one
    private static final List<ProjectStatus> TERMINAL_STATUSES = Arrays.asList(
            ProjectStatus.COMPLETED
    );

    public ProjectService(ProjectRepository projectRepository,
                          ApprovalHistoryRepository historyRepository,
                          ProjectTopicService topicService,
                          GuideAssignmentRepository guideAssignmentRepository) {
        this.projectRepository = projectRepository;
        this.historyRepository = historyRepository;
        this.topicService = topicService;
        this.guideAssignmentRepository = guideAssignmentRepository;
    }

    // ==================== Queries ====================

    public Optional<Project> getProjectById(Long id) {
        return projectRepository.findById(id);
    }

    public List<Project> getProjectsByStudent(User student) {
        return projectRepository.findByStudent(student);
    }

    public List<Project> getProjectsByBranch(Branch branch) {
        return projectRepository.findByBranch(branch);
    }

    public List<Project> getProjectsByBranchAndStatuses(Branch branch, List<ProjectStatus> statuses) {
        return projectRepository.findByBranchAndStatusIn(branch, statuses);
    }

    public List<ApprovalHistory> getApprovalHistory(Long projectId) {
        return historyRepository.findByProjectIdOrderByTimestampDesc(projectId);
    }

    /**
     * Check if student has an active (non-completed) project.
     */
    public boolean hasActiveProject(User student) {
        return projectRepository.existsByStudentAndStatusNotIn(student, TERMINAL_STATUSES);
    }

    /**
     * Get the student's active project (if any).
     */
    public Optional<Project> getActiveProject(User student) {
        List<Project> projects = projectRepository.findByStudent(student);
        return projects.stream()
                .filter(p -> !TERMINAL_STATUSES.contains(p.getStatus()))
                .findFirst();
    }

    public long countByBranch(Branch branch) {
        return projectRepository.countByBranch(branch);
    }

    public long countByBranchAndStatus(Branch branch, ProjectStatus status) {
        return projectRepository.countByBranchAndStatus(branch, status);
    }

    public long countAll() {
        return projectRepository.count();
    }

    public long countByStatus(ProjectStatus status) {
        return projectRepository.countByStatus(status);
    }

    // ==================== Custom Project Flow ====================

    /**
     * Student submits their own project idea.
     * Rule: Student can have only one active project at a time.
     */
    public Project submitOwnIdea(User student, String title, String description,
                                  String category, String techStack) {
        validateStudent(student);
        validateNoActiveProject(student);

        // Require assigned guide
        GuideAssignment guide = guideAssignmentRepository.findByStudent(student)
                .orElseThrow(() -> new IllegalStateException("You must have an assigned faculty guide before submitting a project. Please select a guide first."));

        Project project = new Project();
        project.setTitle(title);
        project.setDescription(description);
        project.setCategory(category);
        project.setTechStack(techStack);
        project.setStudent(student);
        project.setBranch(student.getBranch());
        project.setProjectType(ProjectType.CUSTOM);
        project.setFacultyGuide(guide.getFaculty());
        project.setStatus(ProjectStatus.PROJECT_IDEA_PENDING_FACULTY);

        project = projectRepository.save(project);
        recordHistory(project, "Submitted project idea", student, null, ProjectStatus.PROJECT_IDEA_PENDING_FACULTY, null, null);

        return project;
    }

    // ==================== Topic Selection Flow ====================

    /**
     * Student selects a faculty/HOD-provided topic.
     * Rules: Same branch, one active project only, topic must be available.
     */
    public Project selectTopic(User student, ProjectTopic topic) {
        validateStudent(student);
        validateNoActiveProject(student);
        validateBranchMatch(student, topic.getBranch());

        if (topic.getStatus() != TopicStatus.AVAILABLE) {
            throw new IllegalStateException("Topic is not available for selection");
        }

        // Require assigned guide
        GuideAssignment guide = guideAssignmentRepository.findByStudent(student)
                .orElseThrow(() -> new IllegalStateException("You must have an assigned faculty guide before selecting a topic. Please select a guide first."));

        Project project = new Project();
        project.setTitle(topic.getTitle());
        project.setDescription(topic.getDescription());
        project.setCategory(topic.getCategory());
        project.setTechStack(topic.getTechStack());
        project.setStudent(student);
        project.setBranch(student.getBranch());
        project.setProjectType(ProjectType.TOPIC_BASED);
        project.setTopic(topic);
        project.setFacultyGuide(guide.getFaculty());
        project.setStatus(ProjectStatus.TOPIC_SELECTED);

        project = projectRepository.save(project);

        // Mark topic as assigned
        topicService.markAsAssigned(topic);

        recordHistory(project, "Selected project topic", student, null, ProjectStatus.TOPIC_SELECTED, null, null);

        return project;
    }

    // ==================== Faculty Idea Review ====================

    /**
     * Faculty approves a custom project idea.
     */
    public void approveIdea(Project project, User faculty, String comments) {
        validateFaculty(faculty);
        validateBranchMatch(faculty, project.getBranch());
        validateAssignedGuide(project, faculty);
        validateStatus(project, ProjectStatus.PROJECT_IDEA_PENDING_FACULTY);

        ProjectStatus previousStatus = project.getStatus();
        project.setStatus(ProjectStatus.PROJECT_IDEA_APPROVED);
        projectRepository.save(project);

        recordHistory(project, "Approved project idea", faculty, previousStatus,
                ProjectStatus.PROJECT_IDEA_APPROVED, comments, null);
    }

    /**
     * Faculty rejects a custom project idea. Rejection reason is mandatory.
     */
    public void rejectIdea(Project project, User faculty, String comments, String rejectionReason) {
        validateFaculty(faculty);
        validateBranchMatch(faculty, project.getBranch());
        validateAssignedGuide(project, faculty);
        validateStatus(project, ProjectStatus.PROJECT_IDEA_PENDING_FACULTY);
        validateRejectionReason(rejectionReason);

        ProjectStatus previousStatus = project.getStatus();
        project.setStatus(ProjectStatus.FACULTY_REJECTED);
        project.setRejectedAtStage(ProjectStatus.PROJECT_IDEA_PENDING_FACULTY);
        projectRepository.save(project);

        recordHistory(project, "Rejected project idea", faculty, previousStatus,
                ProjectStatus.FACULTY_REJECTED, comments, rejectionReason);
    }

    // ==================== Student Working ====================

    /**
     * Student starts working on an approved idea.
     */
    public void startWorking(Project project, User student) {
        validateStudent(student);
        validateOwner(project, student);

        // Can start working after idea approval or topic selection
        if (project.getStatus() != ProjectStatus.PROJECT_IDEA_APPROVED &&
            project.getStatus() != ProjectStatus.TOPIC_SELECTED) {
            throw new IllegalStateException("Cannot start working in current status: " + project.getStatus().getDisplayName());
        }

        ProjectStatus previousStatus = project.getStatus();
        project.setStatus(ProjectStatus.STUDENT_WORKING);
        projectRepository.save(project);

        recordHistory(project, "Started working on project", student, previousStatus,
                ProjectStatus.STUDENT_WORKING, null, null);
    }

    /**
     * Student submits completed project for faculty review (with files).
     */
    public void submitForReview(Project project, User student, String reportPath, String projectPath) {
        validateStudent(student);
        validateOwner(project, student);

        if (project.getStatus() != ProjectStatus.STUDENT_WORKING) {
            throw new IllegalStateException("Can only submit from STUDENT_WORKING status");
        }

        ProjectStatus previousStatus = project.getStatus();
        if (reportPath != null) project.setReportFilePath(reportPath);
        if (projectPath != null) project.setProjectFilePath(projectPath);
        project.setStatus(ProjectStatus.PENDING_FACULTY_REVIEW);
        projectRepository.save(project);

        recordHistory(project, "Submitted project for faculty review", student, previousStatus,
                ProjectStatus.PENDING_FACULTY_REVIEW, null, null);
    }

    // ==================== Faculty Project Review ====================

    /**
     * Faculty approves a submitted project → moves to HOD review.
     */
    public void facultyApprove(Project project, User faculty, String comments) {
        validateFaculty(faculty);
        validateBranchMatch(faculty, project.getBranch());
        validateAssignedGuide(project, faculty);
        validateStatus(project, ProjectStatus.PENDING_FACULTY_REVIEW);

        ProjectStatus previousStatus = project.getStatus();
        project.setStatus(ProjectStatus.FACULTY_APPROVED);
        projectRepository.save(project);

        recordHistory(project, "Faculty approved project", faculty, previousStatus,
                ProjectStatus.FACULTY_APPROVED, comments, null);

        // Automatically move to HOD review
        project.setStatus(ProjectStatus.PENDING_HOD_REVIEW);
        projectRepository.save(project);

        recordHistory(project, "Sent to HOD for review", faculty, ProjectStatus.FACULTY_APPROVED,
                ProjectStatus.PENDING_HOD_REVIEW, null, null);
    }

    /**
     * Faculty rejects a submitted project. Rejection reason is mandatory.
     */
    public void facultyReject(Project project, User faculty, String comments, String rejectionReason) {
        validateFaculty(faculty);
        validateBranchMatch(faculty, project.getBranch());
        validateAssignedGuide(project, faculty);
        validateStatus(project, ProjectStatus.PENDING_FACULTY_REVIEW);
        validateRejectionReason(rejectionReason);

        ProjectStatus previousStatus = project.getStatus();
        project.setStatus(ProjectStatus.FACULTY_REJECTED);
        project.setRejectedAtStage(ProjectStatus.PENDING_FACULTY_REVIEW);
        projectRepository.save(project);

        recordHistory(project, "Faculty rejected project", faculty, previousStatus,
                ProjectStatus.FACULTY_REJECTED, comments, rejectionReason);
    }

    // ==================== HOD Review ====================

    /**
     * HOD approves a project → COMPLETED.
     */
    public void hodApprove(Project project, User hod, String comments) {
        validateHod(hod);
        validateBranchMatch(hod, project.getBranch());
        validateStatus(project, ProjectStatus.PENDING_HOD_REVIEW);

        ProjectStatus previousStatus = project.getStatus();
        project.setStatus(ProjectStatus.COMPLETED);
        projectRepository.save(project);

        recordHistory(project, "HOD approved project — COMPLETED", hod, previousStatus,
                ProjectStatus.COMPLETED, comments, null);
    }

    /**
     * HOD rejects a project. Rejection reason is mandatory.
     */
    public void hodReject(Project project, User hod, String comments, String rejectionReason) {
        validateHod(hod);
        validateBranchMatch(hod, project.getBranch());
        validateStatus(project, ProjectStatus.PENDING_HOD_REVIEW);
        validateRejectionReason(rejectionReason);

        ProjectStatus previousStatus = project.getStatus();
        project.setStatus(ProjectStatus.HOD_REJECTED);
        project.setRejectedAtStage(ProjectStatus.PENDING_HOD_REVIEW);
        projectRepository.save(project);

        recordHistory(project, "HOD rejected project", hod, previousStatus,
                ProjectStatus.HOD_REJECTED, comments, rejectionReason);
    }

    // ==================== Student Resubmission ====================

    /**
     * Student resubmits after rejection.
     * Goes back to the stage where it was rejected (faculty or HOD).
     */
    public void resubmit(Project project, User student, String reportPath, String projectPath, String comments) {
        validateStudent(student);
        validateOwner(project, student);

        if (project.getStatus() != ProjectStatus.FACULTY_REJECTED &&
            project.getStatus() != ProjectStatus.HOD_REJECTED) {
            throw new IllegalStateException("Can only resubmit from a rejected status");
        }

        ProjectStatus previousStatus = project.getStatus();

        if (reportPath != null) project.setReportFilePath(reportPath);
        if (projectPath != null) project.setProjectFilePath(projectPath);

        // First move to STUDENT_RESUBMISSION
        project.setStatus(ProjectStatus.STUDENT_RESUBMISSION);
        projectRepository.save(project);

        recordHistory(project, "Student resubmitted project", student, previousStatus,
                ProjectStatus.STUDENT_RESUBMISSION, comments, null);

        // Then route back to the correct reviewer
        ProjectStatus targetStatus;
        if (project.getRejectedAtStage() == ProjectStatus.PENDING_HOD_REVIEW) {
            targetStatus = ProjectStatus.PENDING_HOD_REVIEW;
        } else if (project.getRejectedAtStage() == ProjectStatus.PROJECT_IDEA_PENDING_FACULTY) {
            targetStatus = ProjectStatus.PROJECT_IDEA_PENDING_FACULTY;
        } else {
            targetStatus = ProjectStatus.PENDING_FACULTY_REVIEW;
        }

        project.setStatus(targetStatus);
        project.setRejectedAtStage(null);
        projectRepository.save(project);

        recordHistory(project, "Routed back for review", student, ProjectStatus.STUDENT_RESUBMISSION,
                targetStatus, null, null);
    }

    /**
     * Student edits project details (used before resubmission).
     */
    public void editProject(Project project, User student, String title, String description,
                            String category, String techStack) {
        validateStudent(student);
        validateOwner(project, student);

        // Can only edit when in rejected state
        if (project.getStatus() != ProjectStatus.FACULTY_REJECTED &&
            project.getStatus() != ProjectStatus.HOD_REJECTED) {
            throw new IllegalStateException("Can only edit project when it has been rejected");
        }

        project.setTitle(title);
        project.setDescription(description);
        project.setCategory(category);
        project.setTechStack(techStack);
        projectRepository.save(project);
    }

    // ==================== Validation Helpers ====================

    private void validateStudent(User user) {
        if (user.getRole() != Role.STUDENT) {
            throw new SecurityException("Only students can perform this action");
        }
    }

    private void validateFaculty(User user) {
        if (user.getRole() != Role.FACULTY) {
            throw new SecurityException("Only faculty can perform this action");
        }
    }

    private void validateHod(User user) {
        if (user.getRole() != Role.HOD) {
            throw new SecurityException("Only HOD can perform this action");
        }
    }

    private void validateOwner(Project project, User student) {
        if (!project.getStudent().getId().equals(student.getId())) {
            throw new SecurityException("You do not own this project");
        }
    }

    private void validateBranchMatch(User user, Branch projectBranch) {
        if (user.getBranch() == null || !user.getBranch().getId().equals(projectBranch.getId())) {
            throw new SecurityException("Access denied: you cannot access projects from another branch");
        }
    }

    private void validateNoActiveProject(User student) {
        if (hasActiveProject(student)) {
            throw new IllegalStateException("You already have an active project. Complete or close it before starting a new one.");
        }
    }

    private void validateStatus(Project project, ProjectStatus expectedStatus) {
        if (project.getStatus() != expectedStatus) {
            throw new IllegalStateException(
                    "Invalid action: project is in '" + project.getStatus().getDisplayName() +
                    "' status, expected '" + expectedStatus.getDisplayName() + "'");
        }
    }

    /**
     * Validates that the faculty performing the action is the assigned guide for this project.
     */
    private void validateAssignedGuide(Project project, User faculty) {
        if (project.getFacultyGuide() == null) {
            return; // Legacy projects without guide — allow any branch faculty
        }
        if (!project.getFacultyGuide().getId().equals(faculty.getId())) {
            throw new SecurityException("Only the assigned faculty guide can review this project");
        }
    }

    private void validateRejectionReason(String rejectionReason) {
        if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
            throw new IllegalArgumentException("Rejection reason is mandatory");
        }
    }

    // ==================== History Recording ====================

    private void recordHistory(Project project, String action, User performedBy,
                               ProjectStatus previousStatus, ProjectStatus newStatus,
                               String comments, String rejectionReason) {
        ApprovalHistory history = new ApprovalHistory();
        history.setProject(project);
        history.setAction(action);
        history.setPerformedBy(performedBy);
        history.setUserRole(performedBy.getRole());
        history.setPreviousStatus(previousStatus);
        history.setNewStatus(newStatus);
        history.setComments(comments);
        history.setRejectionReason(rejectionReason);
        historyRepository.save(history);
    }
}
