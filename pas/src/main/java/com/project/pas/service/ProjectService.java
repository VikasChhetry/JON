package com.project.pas.service;

import com.project.pas.model.*;
import com.project.pas.repository.ApprovalHistoryRepository;
import com.project.pas.repository.GuideAssignmentRepository;
import com.project.pas.repository.ProjectRepository;
import com.project.pas.repository.ProjectTeamMemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Core workflow engine implementing the entire project approval state machine.
 * All branch enforcement and business rules are validated here at the service
 * layer.
 *
 * TWO-STAGE WORKFLOW:
 * Stage 1: Project Proposal (synopsis, PPT, problem statement, etc.)
 * -> Faculty reviews proposal -> Approve/Reject
 * Stage 2: Final Project Submission (code, report, GitHub, video)
 * -> Faculty reviews final -> HOD review -> Completed
 */
@Service
@Transactional
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ApprovalHistoryRepository historyRepository;
    private final ProjectTopicService topicService;
    private final GuideAssignmentRepository guideAssignmentRepository;
    private final ProjectTeamMemberRepository teamMemberRepository;

    // Statuses that indicate a project is "finished" — student can start another
    // one
    private static final List<ProjectStatus> TERMINAL_STATUSES = Arrays.asList(
            ProjectStatus.COMPLETED);

    // Default team size limits for OWN PROJECT IDEAS (no topic configured)
    private static final int DEFAULT_MIN_TEAM_SIZE = 1;
    private static final int DEFAULT_MAX_TEAM_SIZE = 4;

    public ProjectService(ProjectRepository projectRepository,
            ApprovalHistoryRepository historyRepository,
            ProjectTopicService topicService,
            GuideAssignmentRepository guideAssignmentRepository,
            ProjectTeamMemberRepository teamMemberRepository) {
        this.projectRepository = projectRepository;
        this.historyRepository = historyRepository;
        this.topicService = topicService;
        this.guideAssignmentRepository = guideAssignmentRepository;
        this.teamMemberRepository = teamMemberRepository;
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
     * Project Details become locked permanently once the initial proposal has been
     * approved.
     */
    public boolean isProjectDetailsLocked(Long projectId) {
        List<ApprovalHistory> history = getApprovalHistory(projectId);
        for (ApprovalHistory h : history) {
            if (h.getNewStatus() == ProjectStatus.PROPOSAL_APPROVED) {
                return true;
            }
        }
        return false;
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

    public List<Project> getAllProjects() {
        return projectRepository.findAll();
    }

    /**
     * Returns only projects where the given faculty is the assigned guide.
     * Used to restrict faculty-facing project lists to their own guided students.
     */
    public List<Project> getProjectsByFacultyGuide(User faculty) {
        return projectRepository.findByFacultyGuide(faculty);
    }

    /**
     * Returns projects for the given faculty guide filtered by a set of statuses.
     * Used for faculty dashboard pending-review queues.
     */
    public List<Project> getProjectsByFacultyGuideAndStatuses(User faculty, List<ProjectStatus> statuses) {
        return projectRepository.findByFacultyGuideAndStatusIn(faculty, statuses);
    }

    public long countByFacultyGuide(User faculty) {
        return projectRepository.countByFacultyGuide(faculty);
    }

    public long countByFacultyGuideAndStatus(User faculty, ProjectStatus status) {
        return projectRepository.countByFacultyGuideAndStatus(faculty, status);
    }

    // ==================== Delete Operations ====================

    /**
     * Student can delete their project ONLY if faculty has not taken any action
     * yet.
     * Rule: Deletion is allowed only when there is NO Faculty action/review
     * recorded for that project.
     */
    public boolean canStudentDelete(Project project, User student) {
        if (project == null || student == null)
            return false;
        if (project.getStudent() == null || !project.getStudent().getId().equals(student.getId()))
            return false;

        // Disallow if status is beyond initial waiting states
        ProjectStatus status = project.getStatus();
        if (status != ProjectStatus.PROJECT_IDEA_PENDING_FACULTY &&
                status != ProjectStatus.TOPIC_SELECTED &&
                status != ProjectStatus.PROPOSAL_PENDING_FACULTY &&
                status != ProjectStatus.PENDING_FACULTY_REVIEW) {
            return false;
        }

        // Check if faculty has recorded any history / action
        List<ApprovalHistory> histories = historyRepository.findByProjectIdOrderByTimestampDesc(project.getId());
        for (ApprovalHistory history : histories) {
            if (history.getUserRole() == Role.FACULTY ||
                    (history.getPerformedBy() != null && history.getPerformedBy().getRole() == Role.FACULTY)) {
                return false;
            }
        }
        return true;
    }

    public void studentDeleteProject(Project project, User student) {
        validateStudent(student);

        if (!canStudentDelete(project, student)) {
            throw new IllegalStateException(
                    "Cannot delete project: Faculty guide has already taken action on this project or project state does not allow deletion.");
        }

        // If topic-based, release the topic back to available
        if (project.getTopic() != null) {
            topicService.releaseAssigned(project.getTopic());
        }

        historyRepository.deleteByProject(project);
        projectRepository.delete(project);
    }

    /**
     * Admin can delete any project at any time, regardless of status.
     */
    public void adminDeleteProject(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));

        // Release topic if applicable
        if (project.getTopic() != null) {
            topicService.releaseAssigned(project.getTopic());
        }

        historyRepository.deleteByProject(project);
        projectRepository.delete(project);
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
                .orElseThrow(() -> new IllegalStateException(
                        "You must have an assigned faculty guide before submitting a project. Please select a guide first."));

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
        recordHistory(project, "Submitted project idea", student, null, ProjectStatus.PROJECT_IDEA_PENDING_FACULTY,
                null, null);

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
                .orElseThrow(() -> new IllegalStateException(
                        "You must have an assigned faculty guide before selecting a topic. Please select a guide first."));

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

    // ==================== Stage 1: Proposal Submission ====================

    /**
     * Student submits proposal documents for faculty review.
     * Allowed from: PROJECT_IDEA_APPROVED, TOPIC_SELECTED, or resubmission after
     * proposal rejection.
     */
    public void submitProposal(Project project, User student,
            String synopsis, String pptFilePath,
            String problemStatement, String objectives,
            String literatureReview, String methodology,
            String systemDesign, String futureWork) {
        validateStudent(student);
        validateOwner(project, student);

        // Allowed states for proposal submission
        if (project.getStatus() != ProjectStatus.PROJECT_IDEA_APPROVED &&
                project.getStatus() != ProjectStatus.TOPIC_SELECTED) {
            throw new IllegalStateException(
                    "Cannot submit proposal in current status: " + project.getStatus().getDisplayName());
        }

        // ===== Validate team size =====
        int[] teamLimits = getTeamSizeLimits(project);
        int minTeam = teamLimits[0];
        int maxTeam = teamLimits[1];
        long currentTeamSize = teamMemberRepository.countByProject(project);

        if (currentTeamSize < minTeam) {
            throw new IllegalStateException(
                    "Team size is too small. You have " + currentTeamSize +
                            " member(s) but the minimum required is " + minTeam +
                            " (including you as team leader). Please add more team members.");
        }
        if (currentTeamSize > maxTeam) {
            throw new IllegalStateException(
                    "Team size exceeds maximum allowed. You have " + currentTeamSize +
                            " member(s) but the maximum allowed is " + maxTeam + ".");
        }

        ProjectStatus previousStatus = project.getStatus();

        // Save proposal fields
        project.setSynopsis(synopsis);
        if (pptFilePath != null)
            project.setPptFilePath(pptFilePath);
        project.setProblemStatement(problemStatement);
        project.setObjectives(objectives);
        project.setLiteratureReview(literatureReview);
        project.setMethodology(methodology);
        project.setSystemDesign(systemDesign);
        project.setFutureWork(futureWork);

        project.setStatus(ProjectStatus.PROPOSAL_PENDING_FACULTY);
        projectRepository.save(project);

        recordHistory(project, "Submitted project proposal for faculty review", student, previousStatus,
                ProjectStatus.PROPOSAL_PENDING_FACULTY, null, null);
    }

    // ==================== Team Member Management ====================

    /**
     * Returns the [min, max] team size for a project.
     * For TOPIC_BASED projects, uses the topic's configured limits.
     * For CUSTOM (own idea) projects, uses system defaults.
     */
    public int[] getTeamSizeLimits(Project project) {
        if (project.getProjectType() == ProjectType.TOPIC_BASED && project.getTopic() != null) {
            return new int[] { project.getTopic().getMinTeamSize(), project.getTopic().getMaxTeamSize() };
        }
        return new int[] { DEFAULT_MIN_TEAM_SIZE, DEFAULT_MAX_TEAM_SIZE };
    }

    /**
     * Get all team members for a project.
     */
    public List<ProjectTeamMember> getTeamMembers(Project project) {
        return teamMemberRepository.findByProject(project);
    }

    /**
     * Get all memberships for a student.
     */
    public List<ProjectTeamMember> getTeamMemberships(User student) {
        return teamMemberRepository.findByMember(student);
    }

    /**
     * Add a team member to a project.
     * Validates: same branch, student role, not duplicate, not in another active
     * project,
     * capacity not exceeded.
     */
    public ProjectTeamMember addTeamMember(Project project, User owner, User member, String contributionRole) {
        validateStudent(owner);
        validateOwner(project, owner);

        // Validate member is a student
        if (member.getRole() != Role.STUDENT) {
            throw new IllegalArgumentException("Only students can be added as team members");
        }

        // Same branch
        if (member.getBranch() == null || !member.getBranch().getId().equals(project.getBranch().getId())) {
            throw new IllegalArgumentException(
                    "Team member must belong to the same branch (" + project.getBranch().getCode() + ")");
        }

        // Not already in this project's team
        if (teamMemberRepository.existsByProjectAndMember(project, member)) {
            throw new IllegalArgumentException("This student is already a team member of this project");
        }

        // Not in another active project (as owner or team member)
        if (!member.getId().equals(owner.getId())) {
            // Check if member owns another active project
            Optional<Project> memberActiveProject = getActiveProject(member);
            if (memberActiveProject.isPresent() && !memberActiveProject.get().getId().equals(project.getId())) {
                throw new IllegalArgumentException(
                        member.getFullName() + " already has an active project and cannot join another team");
            }
            // Check if member is on another project's team
            List<ProjectTeamMember> existingMemberships = teamMemberRepository.findByMember(member);
            for (ProjectTeamMember existing : existingMemberships) {
                Project otherProject = existing.getProject();
                if (!otherProject.getId().equals(project.getId()) &&
                        !TERMINAL_STATUSES.contains(otherProject.getStatus())) {
                    throw new IllegalArgumentException(
                            member.getFullName() + " is already a team member of another active project");
                }
            }
        }

        // Check capacity
        int[] limits = getTeamSizeLimits(project);
        long currentSize = teamMemberRepository.countByProject(project);
        if (currentSize >= limits[1]) {
            throw new IllegalStateException(
                    "Maximum team size (" + limits[1] + ") reached. Cannot add more members.");
        }

        // Contribution role is required
        if (contributionRole == null || contributionRole.trim().isEmpty()) {
            throw new IllegalArgumentException("Contribution/Role is required for each team member");
        }

        ProjectTeamMember teamMember = new ProjectTeamMember();
        teamMember.setProject(project);
        teamMember.setMember(member);
        teamMember.setContributionRole(contributionRole.trim());
        teamMember.setOwner(member.getId().equals(owner.getId()));

        return teamMemberRepository.save(teamMember);
    }

    /**
     * Remove a team member from a project.
     * Cannot remove the project owner.
     */
    public void removeTeamMember(Project project, User owner, Long memberId) {
        validateStudent(owner);
        validateOwner(project, owner);

        ProjectTeamMember teamMember = teamMemberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Team member not found"));

        if (!teamMember.getProject().getId().equals(project.getId())) {
            throw new IllegalArgumentException("Team member does not belong to this project");
        }

        if (teamMember.isOwner()) {
            throw new IllegalStateException("Cannot remove the project owner from the team");
        }

        teamMemberRepository.delete(teamMember);
    }

    /**
     * Update a team member's contribution role.
     */
    public void updateTeamMemberRole(Project project, User owner, Long memberId, String newRole) {
        validateStudent(owner);
        validateOwner(project, owner);

        if (newRole == null || newRole.trim().isEmpty()) {
            throw new IllegalArgumentException("Contribution/Role is required");
        }

        ProjectTeamMember teamMember = teamMemberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Team member not found"));

        if (!teamMember.getProject().getId().equals(project.getId())) {
            throw new IllegalArgumentException("Team member does not belong to this project");
        }

        teamMember.setContributionRole(newRole.trim());
        teamMemberRepository.save(teamMember);
    }

    /**
     * Ensures the project owner is included as a team member.
     * Called when the proposal form is first loaded.
     */
    public void ensureOwnerInTeam(Project project) {
        User owner = project.getStudent();
        if (!teamMemberRepository.existsByProjectAndMember(project, owner)) {
            ProjectTeamMember ownerMember = new ProjectTeamMember();
            ownerMember.setProject(project);
            ownerMember.setMember(owner);
            ownerMember.setContributionRole("Team Leader / Project Owner");
            ownerMember.setOwner(true);
            teamMemberRepository.save(ownerMember);
        }
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

    // ==================== Faculty Proposal Review ====================

    /**
     * Faculty approves a project proposal (Stage 1).
     * After approval, student can start working and eventually submit final project
     * (Stage 2).
     */
    public void approveProposal(Project project, User faculty, String comments) {
        validateFaculty(faculty);
        validateBranchMatch(faculty, project.getBranch());
        validateAssignedGuide(project, faculty);
        validateStatus(project, ProjectStatus.PROPOSAL_PENDING_FACULTY);

        ProjectStatus previousStatus = project.getStatus();
        project.setStatus(ProjectStatus.PROPOSAL_APPROVED);
        projectRepository.save(project);

        recordHistory(project, "Approved project proposal", faculty, previousStatus,
                ProjectStatus.PROPOSAL_APPROVED, comments, null);
    }

    /**
     * Faculty rejects a project proposal. Rejection reason is mandatory.
     */
    public void rejectProposal(Project project, User faculty, String comments, String rejectionReason) {
        validateFaculty(faculty);
        validateBranchMatch(faculty, project.getBranch());
        validateAssignedGuide(project, faculty);
        validateStatus(project, ProjectStatus.PROPOSAL_PENDING_FACULTY);
        validateRejectionReason(rejectionReason);

        ProjectStatus previousStatus = project.getStatus();
        project.setStatus(ProjectStatus.FACULTY_REJECTED);
        project.setRejectedAtStage(ProjectStatus.PROPOSAL_PENDING_FACULTY);
        projectRepository.save(project);

        recordHistory(project, "Rejected project proposal", faculty, previousStatus,
                ProjectStatus.FACULTY_REJECTED, comments, rejectionReason);
    }

    // ==================== Student Working ====================

    /**
     * Student starts working on an approved proposal.
     */
    public void startWorking(Project project, User student) {
        validateStudent(student);
        validateOwner(project, student);

        // Can start working after proposal approval
        if (project.getStatus() != ProjectStatus.PROPOSAL_APPROVED) {
            throw new IllegalStateException(
                    "Cannot start working in current status: " + project.getStatus().getDisplayName() +
                            ". Your proposal must be approved by faculty first.");
        }

        ProjectStatus previousStatus = project.getStatus();
        project.setStatus(ProjectStatus.STUDENT_WORKING);
        projectRepository.save(project);

        recordHistory(project, "Started working on project", student, previousStatus,
                ProjectStatus.STUDENT_WORKING, null, null);
    }

    /**
     * Student submits completed project for faculty review (with files) — Stage 2.
     */
    public void submitForReview(Project project, User student, String reportPath, String projectPath,
            String githubUrl, String videoUrl) {
        validateStudent(student);
        validateOwner(project, student);

        if (project.getStatus() != ProjectStatus.STUDENT_WORKING) {
            throw new IllegalStateException("Can only submit final project from STUDENT_WORKING status. " +
                    "Your proposal must be approved and you must click 'Start Working' first.");
        }

        ProjectStatus previousStatus = project.getStatus();
        if (reportPath != null)
            project.setReportFilePath(reportPath);
        if (projectPath != null)
            project.setProjectFilePath(projectPath);
        if (githubUrl != null && !githubUrl.isBlank())
            project.setGithubUrl(githubUrl);
        if (videoUrl != null && !videoUrl.isBlank())
            project.setVideoUrl(videoUrl);
        project.setStatus(ProjectStatus.PENDING_FACULTY_REVIEW);
        projectRepository.save(project);

        recordHistory(project, "Submitted final project for faculty review", student, previousStatus,
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
     * Goes back to the stage where it was rejected (idea, proposal, faculty review,
     * or HOD).
     *
     * IMPORTANT: Core project details (title, description, category, techStack)
     * are NEVER modified during resubmission. Only proposal documents and/or
     * project files may be updated as per the correction workflow.
     */
    public void resubmit(Project project, User student, String reportPath, String projectPath,
            String githubUrl, String videoUrl, String comments) {
        validateStudent(student);
        validateOwner(project, student);

        if (project.getStatus() != ProjectStatus.FACULTY_REJECTED &&
                project.getStatus() != ProjectStatus.HOD_REJECTED) {
            throw new IllegalStateException("Can only resubmit from a rejected status");
        }

        // SAFETY GUARD: The following fields are intentionally NOT updated here.
        // title, description, category, techStack remain as originally submitted.
        // They are read-only during resubmission and must not be changed.

        ProjectStatus previousStatus = project.getStatus();

        if (reportPath != null)
            project.setReportFilePath(reportPath);
        if (projectPath != null)
            project.setProjectFilePath(projectPath);
        if (githubUrl != null)
            project.setGithubUrl(githubUrl);
        if (videoUrl != null)
            project.setVideoUrl(videoUrl);

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
        } else if (project.getRejectedAtStage() == ProjectStatus.PROPOSAL_PENDING_FACULTY) {
            targetStatus = ProjectStatus.PROPOSAL_PENDING_FACULTY;
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

        boolean isLocked = isProjectDetailsLocked(project.getId());

        if (isLocked) {
            String strTitle = (title == null) ? "" : title.trim();
            String pTitle = (project.getTitle() == null) ? "" : project.getTitle().trim();
            String strDesc = (description == null) ? "" : description.trim();
            String pDesc = (project.getDescription() == null) ? "" : project.getDescription().trim();
            String strCat = (category == null) ? "" : category.trim();
            String pCat = (project.getCategory() == null) ? "" : project.getCategory().trim();
            String strTech = (techStack == null) ? "" : techStack.trim();
            String pTech = (project.getTechStack() == null) ? "" : project.getTechStack().trim();

            if (!strTitle.equals(pTitle) || !strDesc.equals(pDesc) || !strCat.equals(pCat) || !strTech.equals(pTech)) {
                throw new IllegalStateException(
                        "Project Details are locked and cannot be modified after proposal approval.");
            }
        } else {
            project.setTitle(title);
            project.setDescription(description);
            project.setCategory(category);
            project.setTechStack(techStack);
        }
        projectRepository.save(project);
    }

    /**
     * Student edits proposal documents (used when proposal is rejected).
     */
    public void editProposal(Project project, User student,
            String synopsis, String pptFilePath,
            String problemStatement, String objectives,
            String literatureReview, String methodology,
            String systemDesign, String futureWork) {
        validateStudent(student);
        validateOwner(project, student);

        if (project.getStatus() != ProjectStatus.FACULTY_REJECTED) {
            throw new IllegalStateException("Can only edit proposal when it has been rejected");
        }

        if (project.getRejectedAtStage() != ProjectStatus.PROPOSAL_PENDING_FACULTY) {
            throw new IllegalStateException("Proposal can only be edited when rejected at the proposal stage");
        }

        // ===== Validate team size =====
        int[] teamLimits = getTeamSizeLimits(project);
        int minTeam = teamLimits[0];
        int maxTeam = teamLimits[1];
        long currentTeamSize = teamMemberRepository.countByProject(project);

        if (currentTeamSize < minTeam) {
            throw new IllegalStateException(
                    "Team size is too small. You have " + currentTeamSize +
                            " member(s) but the minimum required is " + minTeam +
                            " (including you as team leader). Please add more team members.");
        }
        if (currentTeamSize > maxTeam) {
            throw new IllegalStateException(
                    "Team size exceeds maximum allowed. You have " + currentTeamSize +
                            " member(s) but the maximum allowed is " + maxTeam + ".");
        }

        project.setSynopsis(synopsis);
        if (pptFilePath != null)
            project.setPptFilePath(pptFilePath);
        project.setProblemStatement(problemStatement);
        project.setObjectives(objectives);
        project.setLiteratureReview(literatureReview);
        project.setMethodology(methodology);
        project.setSystemDesign(systemDesign);
        project.setFutureWork(futureWork);
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
            throw new IllegalStateException(
                    "You already have an active project. Complete or close it before starting a new one.");
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
     * Validates that the faculty performing the action is the assigned guide for
     * this project.
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
