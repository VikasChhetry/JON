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
    private final FileStorageService fileStorageService;
    private final BranchRepository branchRepository;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       ProjectRepository projectRepository, GuideAssignmentRepository guideAssignmentRepository,
                       ApprovalHistoryRepository approvalHistoryRepository, ProjectTopicRepository projectTopicRepository,
                       GuideSelectionFormRepository formRepository, GuideAssignmentHistoryRepository guideHistoryRepository,
                       FileStorageService fileStorageService, BranchRepository branchRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.projectRepository = projectRepository;
        this.guideAssignmentRepository = guideAssignmentRepository;
        this.approvalHistoryRepository = approvalHistoryRepository;
        this.projectTopicRepository = projectTopicRepository;
        this.formRepository = formRepository;
        this.guideHistoryRepository = guideHistoryRepository;
        this.fileStorageService = fileStorageService;
        this.branchRepository = branchRepository;
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

    public static class BulkUserUploadResult {
        private int totalProcessed;
        private int successCount;
        private int failedCount;
        private List<String> errors = new java.util.ArrayList<>();

        public int getTotalProcessed() { return totalProcessed; }
        public int getSuccessCount() { return successCount; }
        public int getFailedCount() { return failedCount; }
        public List<String> getErrors() { return errors; }

        public void incrementSuccess() { successCount++; totalProcessed++; }
        public void addError(int rowNum, String error) { errors.add("Row " + rowNum + ": " + error); failedCount++; totalProcessed++; }
    }

    public BulkUserUploadResult uploadUsersFromCsv(org.springframework.web.multipart.MultipartFile file) {
        BulkUserUploadResult result = new BulkUserUploadResult();
        List<String> lines = fileStorageService.readCsvLines(file);

        // Skip header line
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) continue;

            String[] parts = parseCsvLine(line);
            if (parts.length < 5) {
                result.addError(i + 1, "Missing required columns. Expected: fullName, email, role, branch, password");
                continue;
            }

            String fullName = parts[0].trim();
            String email = parts[1].trim();
            String roleStr = parts[2].trim().toUpperCase();
            String branchCode = parts[3].trim().toUpperCase();
            String rawPassword = parts[4].trim();

            if (userRepository.existsByEmail(email)) {
                result.addError(i + 1, "Email already exists: " + email);
                continue;
            }

            Role role;
            try {
                role = Role.valueOf(roleStr);
            } catch (IllegalArgumentException e) {
                result.addError(i + 1, "Invalid role: " + roleStr);
                continue;
            }

            Branch branch = null;
            if (role != Role.ADMIN) {
                if (branchCode.isEmpty()) {
                    result.addError(i + 1, "Branch code is required for role: " + roleStr);
                    continue;
                }
                branch = branchRepository.findByCode(branchCode).orElse(null);
                if (branch == null) {
                    result.addError(i + 1, "Invalid branch code: " + branchCode);
                    continue;
                }

                if (role == Role.HOD) {
                    List<User> existingHods = userRepository.findByBranchAndRole(branch, Role.HOD);
                    if (!existingHods.isEmpty()) {
                        result.addError(i + 1, "Branch " + branch.getName() + " already has an HOD assigned");
                        continue;
                    }
                }
            }

            User user = new User(fullName, email, passwordEncoder.encode(rawPassword), role, branch);
            userRepository.save(user);
            result.incrementSuccess();
        }

        return result;
    }

    private String[] parseCsvLine(String line) {
        List<String> result = new java.util.ArrayList<>();
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();

        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        result.add(current.toString());
        return result.toArray(new String[0]);
    }
}
