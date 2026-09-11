package com.project.pas.service;

import com.project.pas.model.*;
import com.project.pas.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Core logic engine for the Faculty Guide Selection workflow.
 * Enforces branch isolation, time-window constraints, and capacity limits.
 */
@Service
@Transactional
public class GuideSelectionService {

    private static final Logger log = LoggerFactory.getLogger(GuideSelectionService.class);

    private final GuideSelectionFormRepository formRepository;
    private final GuideAssignmentRepository assignmentRepository;
    private final GuideAssignmentHistoryRepository historyRepository;
    private final UserRepository userRepository;

    public GuideSelectionService(GuideSelectionFormRepository formRepository,
            GuideAssignmentRepository assignmentRepository,
            GuideAssignmentHistoryRepository historyRepository,
            UserRepository userRepository) {
        this.formRepository = formRepository;
        this.assignmentRepository = assignmentRepository;
        this.historyRepository = historyRepository;
        this.userRepository = userRepository;
    }

    // ==================== Form Management (HOD) ====================

    /**
     * HOD creates a new guide selection form for their branch.
     * Deactivates any existing active form first.
     */
    public GuideSelectionForm createSelectionForm(User hod, LocalDateTime start, LocalDateTime end) {
        validateHod(hod);
        validateDateRange(start, end);

        Branch branch = hod.getBranch();

        // Deactivate existing active form
        formRepository.findByBranchAndActiveTrue(branch).ifPresent(existing -> {
            existing.setActive(false);
            formRepository.save(existing);
        });

        GuideSelectionForm form = new GuideSelectionForm();
        form.setBranch(branch);
        form.setCreatedBy(hod);
        form.setStartDateTime(start);
        form.setEndDateTime(end);
        form.setActive(true);

        return formRepository.save(form);
    }

    /**
     * HOD updates an existing selection form.
     */
    public GuideSelectionForm updateSelectionForm(User hod, Long formId, LocalDateTime start, LocalDateTime end) {
        validateHod(hod);
        validateDateRange(start, end);

        GuideSelectionForm form = formRepository.findById(formId)
                .orElseThrow(() -> new IllegalArgumentException("Form not found"));
        validateBranchMatch(hod, form.getBranch());

        form.setStartDateTime(start);
        form.setEndDateTime(end);
        return formRepository.save(form);
    }

    /**
     * Get the active form for a branch.
     */
    public Optional<GuideSelectionForm> getActiveForm(Branch branch) {
        return formRepository.findByBranchAndActiveTrue(branch);
    }

    /**
     * Is the selection window currently open?
     */
    public boolean isSelectionActive(Branch branch) {
        return getActiveForm(branch).map(GuideSelectionForm::isCurrentlyOpen).orElse(false);
    }

    /**
     * Has the selection period ended?
     */
    public boolean isSelectionEnded(Branch branch) {
        return getActiveForm(branch).map(GuideSelectionForm::hasEnded).orElse(false);
    }

    // ==================== Faculty Capacity (HOD) ====================

    /**
     * HOD sets a faculty's maximum guiding capacity.
     */
    public void updateFacultyCapacity(User hod, Long facultyId, int maxCapacity) {
        validateHod(hod);
        if (maxCapacity < 0) {
            throw new IllegalArgumentException("Capacity cannot be negative");
        }

        User faculty = userRepository.findById(facultyId)
                .orElseThrow(() -> new IllegalArgumentException("Faculty not found"));

        if (faculty.getRole() != Role.FACULTY) {
            throw new IllegalArgumentException("User is not a faculty member");
        }
        validateBranchMatch(hod, faculty.getBranch());

        // Ensure capacity isn't set below current assignments
        long currentAssigned = assignmentRepository.countByFaculty(faculty);
        if (maxCapacity < currentAssigned) {
            throw new IllegalArgumentException(
                    "Cannot set capacity below current assignment count (" + currentAssigned + ")");
        }

        faculty.setMaxGuidingCapacity(maxCapacity);
        userRepository.save(faculty);
    }

    // ==================== Student Guide Selection ====================

    /**
     * Student selects a faculty guide during the active selection period.
     */
    public GuideAssignment selectGuide(User student, Long facultyId) {
        validateStudent(student);
        Branch branch = student.getBranch();

        // Check selection is active
        GuideSelectionForm form = getActiveForm(branch)
                .orElseThrow(() -> new IllegalStateException("No active guide selection form for your branch"));

        if (!form.isCurrentlyOpen()) {
            if (form.hasNotStarted()) {
                throw new IllegalStateException(
                        "Guide selection has not started yet. It opens on " + form.getStartDateTime());
            } else {
                throw new IllegalStateException("Guide selection period has ended");
            }
        }

        // Check student doesn't already have a guide
        if (assignmentRepository.existsByStudent(student)) {
            throw new IllegalStateException("You already have an assigned faculty guide. Contact HOD for changes.");
        }

        // Validate faculty
        User faculty = userRepository.findById(facultyId)
                .orElseThrow(() -> new IllegalArgumentException("Faculty not found"));
        if (faculty.getRole() != Role.FACULTY) {
            throw new IllegalArgumentException("Selected user is not a faculty member");
        }
        validateBranchMatch(student, faculty.getBranch());

        // Check capacity
        long currentAssigned = assignmentRepository.countByFaculty(faculty);
        if (faculty.getMaxGuidingCapacity() <= 0) {
            throw new IllegalStateException("This faculty is not available for guiding");
        }
        if (currentAssigned >= faculty.getMaxGuidingCapacity()) {
            throw new IllegalStateException(
                    "This faculty has reached maximum capacity. Please select another faculty.");
        }

        // Create assignment
        GuideAssignment assignment = new GuideAssignment();
        assignment.setStudent(student);
        assignment.setFaculty(faculty);
        assignment.setBranch(branch);
        assignment.setSelectionForm(form);
        assignment.setAssignedBy("STUDENT");

        assignment = assignmentRepository.save(assignment);

        // Record history
        recordHistory("Student selected faculty guide", student, student, null, faculty, "Self-selected", branch);

        return assignment;
    }

    /**
     * Student removes their own guide selection.
     * Only allowed if the student selected the guide themselves (assignedBy ==
     * "STUDENT").
     * If HOD assigned the guide, the student cannot remove it.
     */
    public void studentRemoveGuide(User student) {
        validateStudent(student);
        Branch branch = student.getBranch();

        GuideAssignment assignment = assignmentRepository.findByStudent(student)
                .orElseThrow(() -> new IllegalArgumentException("You don't have a guide assigned"));

        // Only allow removal if student selected it themselves
        if (!"STUDENT".equals(assignment.getAssignedBy())) {
            throw new IllegalStateException(
                    "Cannot remove guide: your guide was assigned by " + assignment.getAssignedBy()
                            + ". Contact HOD for changes.");
        }

        // Check selection period is still active
        if (!isSelectionActive(branch)) {
            throw new IllegalStateException("Cannot remove guide outside the active selection period. Contact HOD.");
        }

        User previousFaculty = assignment.getFaculty();
        assignmentRepository.deleteByStudent(student);

        recordHistory("Student removed self-selected guide", student, student,
                previousFaculty, null, "Student removed own selection", branch);
    }

    // ==================== Faculty Operations ====================

    /**
     * Faculty views students assigned to them.
     */
    public List<GuideAssignment> getAssignedStudents(User faculty) {
        if (faculty.getRole() != Role.FACULTY) {
            throw new SecurityException("Not a faculty member");
        }
        return assignmentRepository.findByFaculty(faculty);
    }

    /**
     * Faculty removes a student during the active selection period.
     */
    public void facultyRemoveStudent(User faculty, Long studentId, String reason) {
        if (faculty.getRole() != Role.FACULTY) {
            throw new SecurityException("Not a faculty member");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Reason is required for removing a student");
        }

        Branch branch = faculty.getBranch();

        // Check selection is still active
        if (!isSelectionActive(branch)) {
            throw new IllegalStateException("Cannot remove students outside the active selection period. Contact HOD.");
        }

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));

        GuideAssignment assignment = assignmentRepository.findByStudent(student)
                .orElseThrow(() -> new IllegalArgumentException("Student is not assigned to any faculty"));

        // Faculty can only remove their own assigned students
        if (!assignment.getFaculty().getId().equals(faculty.getId())) {
            throw new SecurityException("This student is not assigned to you");
        }

        assignmentRepository.deleteByStudent(student);

        recordHistory("Faculty removed student", faculty, student, faculty, null, reason, branch);
    }

    // ==================== HOD Override Operations ====================

    /**
     * HOD assigns a student to a faculty. Works at any time.
     */
    public GuideAssignment hodAssignStudent(User hod, Long studentId, Long facultyId, String reason) {
        validateHod(hod);
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Reason/comment is required");
        }

        Branch branch = hod.getBranch();

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));
        validateBranchMatch(hod, student.getBranch());
        if (student.getRole() != Role.STUDENT) {
            throw new IllegalArgumentException("User is not a student");
        }

        User faculty = userRepository.findById(facultyId)
                .orElseThrow(() -> new IllegalArgumentException("Faculty not found"));
        validateBranchMatch(hod, faculty.getBranch());
        if (faculty.getRole() != Role.FACULTY) {
            throw new IllegalArgumentException("User is not a faculty member");
        }

        Optional<GuideAssignment> existingOpt = assignmentRepository.findByStudent(student);
        User previousFaculty = existingOpt.map(GuideAssignment::getFaculty).orElse(null);

        // Check capacity if assigning to a different faculty
        if (previousFaculty == null || !previousFaculty.getId().equals(faculty.getId())) {
            long currentAssigned = assignmentRepository.countByFaculty(faculty);
            if (faculty.getMaxGuidingCapacity() > 0 && currentAssigned >= faculty.getMaxGuidingCapacity()) {
                throw new IllegalStateException(
                        "Faculty has reached maximum capacity (" + faculty.getMaxGuidingCapacity() + ")");
            }
        }

        GuideAssignment assignment;
        boolean isReassignment = existingOpt.isPresent();

        if (isReassignment) {
            assignment = existingOpt.get();
            assignment.setFaculty(faculty);
            assignment.setAssignedBy("HOD");
            assignment.setAssignedAt(LocalDateTime.now());
            getActiveForm(branch).ifPresent(assignment::setSelectionForm);
        } else {
            assignment = new GuideAssignment();
            assignment.setStudent(student);
            assignment.setFaculty(faculty);
            assignment.setBranch(branch);
            assignment.setAssignedBy("HOD");
            assignment.setAssignedAt(LocalDateTime.now());
            getActiveForm(branch).ifPresent(assignment::setSelectionForm);
        }

        assignment = assignmentRepository.save(assignment);

        String action = isReassignment ? "HOD REASSIGNED GUIDE" : "HOD ASSIGNED GUIDE";
        recordHistory(action, hod, student, previousFaculty, faculty, reason, branch);

        return assignment;
    }

    /**
     * HOD removes a student's guide assignment. Works at any time.
     */
    public void hodRemoveStudent(User hod, Long studentId, String reason) {
        validateHod(hod);
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Reason/comment is required");
        }

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));
        validateBranchMatch(hod, student.getBranch());

        GuideAssignment assignment = assignmentRepository.findByStudent(student)
                .orElseThrow(() -> new IllegalArgumentException("Student has no guide assignment"));

        User previousFaculty = assignment.getFaculty();
        assignmentRepository.delete(assignment);

        recordHistory("HOD REMOVED GUIDE", hod, student, previousFaculty, null, reason, hod.getBranch());
    }

    /**
     * HOD reassigns a student from one faculty to another.
     */
    public GuideAssignment hodReassignStudent(User hod, Long studentId, Long newFacultyId, String reason) {
        return hodAssignStudent(hod, studentId, newFacultyId, reason);
    }

    /**
     * HOD triggers manual auto-assignment for unassigned students in their branch.
     * Delegates to the shared internal doAutoAssign() method.
     * This is idempotent — students who already have a guide are skipped.
     */
    public int autoAssignUnassignedStudents(User hod) {
        validateHod(hod);
        return doAutoAssign(hod.getBranch(), hod);
    }

    /**
     * Called by the scheduler to process all active selection forms that have
     * expired (endDateTime passed) but have not yet been auto-processed.
     *
     * For each such form:
     * 1. Finds all unassigned students in that branch.
     * 2. Assigns them to faculty with available capacity (lowest-load first).
     * 3. Records GuideAssignmentHistory with action "AUTO ASSIGNED GUIDE".
     * 4. Marks the form autoProcessed = true to prevent re-processing.
     *
     * This method is safe to call multiple times — it will not create duplicate
     * assignments because already-assigned students are filtered out, and the
     * autoProcessed flag prevents the form from being processed again.
     */
    public void processExpiredForms() {
        List<GuideSelectionForm> expiredForms = formRepository.findExpiredUnprocessedForms(LocalDateTime.now());
        if (expiredForms.isEmpty()) {
            return;
        }

        for (GuideSelectionForm form : expiredForms) {
            Branch branch = form.getBranch();
            // Use the HOD who created the form as the recorded performer for history
            User formCreator = form.getCreatedBy();
            log.info("[AutoAssign] Processing expired form id={} for branch={}", form.getId(), branch.getCode());
            try {
                int assigned = doAutoAssign(branch, formCreator);
                log.info("[AutoAssign] Auto-assigned {} student(s) in branch {}", assigned, branch.getCode());
            } catch (Exception ex) {
                log.error("[AutoAssign] Error during auto-assignment for branch {}: {}", branch.getCode(),
                        ex.getMessage(), ex);
            }
            // Mark processed regardless — even if 0 were assigned (all already assigned)
            form.setAutoProcessed(true);
            formRepository.save(form);
        }
    }

    /**
     * Core auto-assignment logic shared by both the HOD manual button and the
     * scheduler. Assigns unassigned students in `branch` to faculty with the
     * lowest current load, respecting capacity limits. Never creates a second
     * GuideAssignment for a student who already has one.
     *
     * @param branch      the branch to process
     * @param performedBy the User to record as the history performer (null for
     *                    scheduler/AUTO)
     * @return number of students actually assigned in this invocation
     */
    private int doAutoAssign(Branch branch, User performedBy) {
        // All students in this branch
        List<User> allStudents = userRepository.findByBranchAndRole(branch, Role.STUDENT);

        // Only those without a guide
        List<User> unassigned = allStudents.stream()
                .filter(s -> !assignmentRepository.existsByStudent(s))
                .collect(Collectors.toList());

        if (unassigned.isEmpty()) {
            return 0;
        }

        List<User> facultyList = userRepository.findByBranchAndRole(branch, Role.FACULTY);
        Optional<GuideSelectionForm> activeForm = getActiveForm(branch);

        int assignedCount = 0;
        for (User student : unassigned) {
            // Pick the faculty with the lowest current load that still has capacity
            User bestFaculty = null;
            long lowestCount = Long.MAX_VALUE;

            for (User f : facultyList) {
                if (f.getMaxGuidingCapacity() <= 0)
                    continue;
                long count = assignmentRepository.countByFaculty(f);
                if (count < f.getMaxGuidingCapacity() && count < lowestCount) {
                    lowestCount = count;
                    bestFaculty = f;
                }
            }

            if (bestFaculty == null) {
                break; // No remaining capacity across any faculty
            }

            // Guard: do not create a second assignment for this student in case of
            // concurrency
            if (assignmentRepository.existsByStudent(student)) {
                continue;
            }

            GuideAssignment assignment = new GuideAssignment();
            assignment.setStudent(student);
            assignment.setFaculty(bestFaculty);
            assignment.setBranch(branch);
            assignment.setAssignedBy("AUTO");
            activeForm.ifPresent(assignment::setSelectionForm);
            assignmentRepository.save(assignment);

            // Record history
            String reason = "Automatically assigned after guide selection period ended";
            recordHistory("AUTO ASSIGNED GUIDE", performedBy, student, null, bestFaculty, reason, branch);
            assignedCount++;
        }

        return assignedCount;
    }

    // ==================== Query Methods ====================

    /**
     * Get a student's guide assignment.
     */
    public Optional<GuideAssignment> getStudentAssignment(User student) {
        return assignmentRepository.findByStudent(student);
    }

    /**
     * Get all assignments for a branch.
     */
    public List<GuideAssignment> getAssignmentsByBranch(Branch branch) {
        return assignmentRepository.findByBranch(branch);
    }

    /**
     * Get faculty capacity info for a branch (for display).
     * Returns a list of maps with faculty info, maxCapacity, assigned count,
     * vacancies.
     */
    public List<Map<String, Object>> getFacultyCapacityInfo(Branch branch) {
        List<User> facultyList = userRepository.findByBranchAndRole(branch, Role.FACULTY);
        List<Map<String, Object>> result = new ArrayList<>();

        for (User faculty : facultyList) {
            Map<String, Object> info = new LinkedHashMap<>();
            long assigned = assignmentRepository.countByFaculty(faculty);
            info.put("faculty", faculty);
            info.put("maxCapacity", faculty.getMaxGuidingCapacity());
            info.put("assignedCount", assigned);
            info.put("vacancies", Math.max(0, faculty.getMaxGuidingCapacity() - assigned));
            info.put("available", faculty.getMaxGuidingCapacity() > 0 && assigned < faculty.getMaxGuidingCapacity());
            result.add(info);
        }

        return result;
    }

    /**
     * Get audit history for a branch.
     */
    public List<GuideAssignmentHistory> getHistory(Branch branch) {
        return historyRepository.findByBranchOrderByTimestampDesc(branch);
    }

    // ==================== Validation Helpers ====================

    private void validateHod(User user) {
        if (user.getRole() != Role.HOD) {
            throw new SecurityException("Only HOD can perform this action");
        }
    }

    private void validateStudent(User user) {
        if (user.getRole() != Role.STUDENT) {
            throw new SecurityException("Only students can perform this action");
        }
    }

    private void validateBranchMatch(User user, Branch targetBranch) {
        if (user.getBranch() == null || !user.getBranch().getId().equals(targetBranch.getId())) {
            throw new SecurityException("Access denied: branch mismatch");
        }
    }

    private void validateDateRange(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("Start and end dates are required");
        }
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("End date must be after start date");
        }
    }

    // ==================== History Recording ====================

    private void recordHistory(String action, User performedBy, User student,
            User previousFaculty, User newFaculty,
            String reason, Branch branch) {
        GuideAssignmentHistory history = new GuideAssignmentHistory();
        history.setAction(action);
        history.setPerformedBy(performedBy);
        history.setPerformerRole(performedBy.getRole());
        history.setStudent(student);
        history.setPreviousFaculty(previousFaculty);
        history.setNewFaculty(newFaculty);
        history.setReason(reason);
        history.setBranch(branch);
        historyRepository.save(history);
    }
}
