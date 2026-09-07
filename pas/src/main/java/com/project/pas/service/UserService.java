package com.project.pas.service;

import com.project.pas.model.Branch;
import com.project.pas.model.Role;
import com.project.pas.model.User;
import com.project.pas.repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ProjectRepository projectRepository;
    private final GuideAssignmentRepository guideAssignmentRepository;
    private final ApprovalHistoryRepository approvalHistoryRepository;
    private final ProjectTopicRepository projectTopicRepository;
    private final GuideSelectionFormRepository formRepository;
    private final GuideAssignmentHistoryRepository guideHistoryRepository;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       ProjectRepository projectRepository, GuideAssignmentRepository guideAssignmentRepository,
                       ApprovalHistoryRepository approvalHistoryRepository, ProjectTopicRepository projectTopicRepository,
                       GuideSelectionFormRepository formRepository, GuideAssignmentHistoryRepository guideHistoryRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.projectRepository = projectRepository;
        this.guideAssignmentRepository = guideAssignmentRepository;
        this.approvalHistoryRepository = approvalHistoryRepository;
        this.projectTopicRepository = projectTopicRepository;
        this.formRepository = formRepository;
        this.guideHistoryRepository = guideHistoryRepository;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public List<User> getUsersByBranch(Branch branch) {
        return userRepository.findByBranch(branch);
    }

    public List<User> getUsersByBranchAndRole(Branch branch, Role role) {
        return userRepository.findByBranchAndRole(branch, role);
    }

    public User createUser(String fullName, String email, String rawPassword, Role role, Branch branch) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already registered: " + email);
        }

        // Enforce: only one HOD per branch
        if (role == Role.HOD && branch != null) {
            List<User> existingHods = userRepository.findByBranchAndRole(branch, Role.HOD);
            if (!existingHods.isEmpty()) {
                throw new IllegalArgumentException("Branch " + branch.getName() + " already has an HOD assigned");
            }
        }

        // ADMIN doesn't require a branch, all others do
        if (role != Role.ADMIN && branch == null) {
            throw new IllegalArgumentException("Branch is required for " + role.name() + " role");
        }

        User user = new User(fullName, email, passwordEncoder.encode(rawPassword), role, branch);
        return userRepository.save(user);
    }

    public User updateUser(Long id, String fullName, String email, Role role, Branch branch) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Enforce HOD uniqueness if changing to HOD
        if (role == Role.HOD && branch != null && user.getRole() != Role.HOD) {
            List<User> existingHods = userRepository.findByBranchAndRole(branch, Role.HOD);
            if (!existingHods.isEmpty()) {
                throw new IllegalArgumentException("Branch " + branch.getName() + " already has an HOD assigned");
            }
        }

        user.setFullName(fullName);
        user.setEmail(email);
        user.setRole(role);
        user.setBranch(branch);
        return userRepository.save(user);
    }

    public void updatePassword(Long id, String rawPassword) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setPassword(passwordEncoder.encode(rawPassword));
        userRepository.save(user);
    }

    public void toggleEnabled(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setEnabled(!user.isEnabled());
        userRepository.save(user);
    }

    public long count() {
        return userRepository.count();
    }

    public long countByRole(Role role) {
        return userRepository.countByRole(role);
    }

    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        boolean hasProjects = projectRepository.existsByStudent(user) || projectRepository.existsByFacultyGuide(user);
        boolean hasAssignments = guideAssignmentRepository.existsByStudent(user) || guideAssignmentRepository.existsByFaculty(user);
        boolean hasApprovalHist = approvalHistoryRepository.existsByPerformedBy(user);
        boolean hasTopics = projectTopicRepository.existsByCreatedBy(user);
        boolean hasForms = formRepository.existsByCreatedBy(user);
        boolean hasGuideHist = guideHistoryRepository.existsByStudent(user) || guideHistoryRepository.existsByPreviousFaculty(user)
                || guideHistoryRepository.existsByNewFaculty(user) || guideHistoryRepository.existsByPerformedBy(user);

        if (hasProjects || hasAssignments || hasApprovalHist || hasTopics || hasForms || hasGuideHist) {
            user.setEnabled(false);
            userRepository.save(user);
            throw new IllegalStateException("User '" + user.getFullName() + "' has historical records in the system and cannot be physically deleted. The account has been deactivated instead.");
        }

        userRepository.delete(user);
    }
}
