package com.project.pas.service;

import com.project.pas.model.Branch;
import com.project.pas.model.Role;
import com.project.pas.model.User;
import com.project.pas.repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.project.pas.specification.SearchSpecifications;

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

    public Page<User> searchUsers(String keyword, Role roleFilter, Branch branch, Boolean statusFilter,
            Pageable pageable) {
        return userRepository.findAll(
                SearchSpecifications.userSearch(keyword, roleFilter, branch, statusFilter),
                pageable);
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
        return createUser(fullName, email, rawPassword, role, branch, null, null);
    }

    public User createUser(String fullName, String email, String rawPassword, Role role, Branch branch,
            String erpId, String rollNumber) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already registered: " + email);
        }

        // Validate ERP ID uniqueness
        if (erpId != null && !erpId.trim().isEmpty()) {
            erpId = erpId.trim();
            if (userRepository.existsByErpId(erpId)) {
                throw new IllegalArgumentException("ERP ID already in use: " + erpId);
            }
        } else {
            erpId = null;
        }

        // Validate Roll Number uniqueness
        if (rollNumber != null && !rollNumber.trim().isEmpty()) {
            rollNumber = rollNumber.trim();
            if (userRepository.existsByRollNumber(rollNumber)) {
                throw new IllegalArgumentException("Roll Number already in use: " + rollNumber);
            }
        } else {
            rollNumber = null;
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
        user.setErpId(erpId);
        user.setRollNumber(rollNumber);
        return userRepository.save(user);
    }

    public User updateUser(Long id, String fullName, String email, Role role, Branch branch) {
        return updateUser(id, fullName, email, role, branch, null, null);
    }

    public User updateUser(Long id, String fullName, String email, Role role, Branch branch,
            String erpId, String rollNumber) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Enforce HOD uniqueness if changing to HOD
        if (role == Role.HOD && branch != null && user.getRole() != Role.HOD) {
            List<User> existingHods = userRepository.findByBranchAndRole(branch, Role.HOD);
            if (!existingHods.isEmpty()) {
                throw new IllegalArgumentException("Branch " + branch.getName() + " already has an HOD assigned");
            }
        }

        // Validate ERP ID uniqueness (allow same user to keep theirs)
        if (erpId != null && !erpId.trim().isEmpty()) {
            erpId = erpId.trim();
            Optional<User> existing = userRepository.findByErpId(erpId);
            if (existing.isPresent() && !existing.get().getId().equals(id)) {
                throw new IllegalArgumentException("ERP ID already in use: " + erpId);
            }
        } else {
            erpId = null;
        }

        // Validate Roll Number uniqueness (allow same user to keep theirs)
        if (rollNumber != null && !rollNumber.trim().isEmpty()) {
            rollNumber = rollNumber.trim();
            Optional<User> existing = userRepository.findByRollNumber(rollNumber);
            if (existing.isPresent() && !existing.get().getId().equals(id)) {
                throw new IllegalArgumentException("Roll Number already in use: " + rollNumber);
            }
        } else {
            rollNumber = null;
        }

        user.setFullName(fullName);
        user.setEmail(email);
        user.setRole(role);
        user.setBranch(branch);
        user.setErpId(erpId);
        user.setRollNumber(rollNumber);
        return userRepository.save(user);
    }

    /**
     * Update the current user's own profile.
     * Users cannot change their own role or branch.
     */
    public User updateOwnProfile(Long userId, String fullName, String erpId, String rollNumber) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (fullName == null || fullName.trim().isEmpty()) {
            throw new IllegalArgumentException("Full name is required");
        }

        // Validate ERP ID uniqueness
        if (erpId != null && !erpId.trim().isEmpty()) {
            erpId = erpId.trim();
            Optional<User> existing = userRepository.findByErpId(erpId);
            if (existing.isPresent() && !existing.get().getId().equals(userId)) {
                throw new IllegalArgumentException("ERP ID already in use: " + erpId);
            }
        } else {
            erpId = null;
        }

        // Validate Roll Number uniqueness
        if (rollNumber != null && !rollNumber.trim().isEmpty()) {
            rollNumber = rollNumber.trim();
            Optional<User> existing = userRepository.findByRollNumber(rollNumber);
            if (existing.isPresent() && !existing.get().getId().equals(userId)) {
                throw new IllegalArgumentException("Roll Number already in use: " + rollNumber);
            }
        } else {
            rollNumber = null;
        }

        user.setFullName(fullName.trim());
        user.setErpId(erpId);
        user.setRollNumber(rollNumber);
        return userRepository.save(user);
    }

    public void updateProfilePhoto(Long userId, String photoPath) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setProfilePhotoPath(photoPath);
        userRepository.save(user);
    }

    public void updatePassword(Long id, String rawPassword) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setPassword(passwordEncoder.encode(rawPassword));
        userRepository.save(user);
    }

    /**
     * Change own password — validates old password first.
     */
    public void changeOwnPassword(Long userId, String oldPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new IllegalArgumentException("New password must be at least 6 characters");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
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

    public long countByBranch(Branch branch) {
        return userRepository.countByBranch(branch);
    }

    public long countByBranchAndRole(Branch branch, Role role) {
        return userRepository.countByBranchAndRole(branch, role);
    }

    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        boolean hasProjects = projectRepository.existsByStudent(user) || projectRepository.existsByFacultyGuide(user);
        boolean hasAssignments = guideAssignmentRepository.existsByStudent(user)
                || guideAssignmentRepository.existsByFaculty(user);
        boolean hasApprovalHist = approvalHistoryRepository.existsByPerformedBy(user);
        boolean hasTopics = projectTopicRepository.existsByCreatedBy(user);
        boolean hasForms = formRepository.existsByCreatedBy(user);
        boolean hasGuideHist = guideHistoryRepository.existsByStudent(user)
                || guideHistoryRepository.existsByPreviousFaculty(user)
                || guideHistoryRepository.existsByNewFaculty(user) || guideHistoryRepository.existsByPerformedBy(user);

        if (hasProjects || hasAssignments || hasApprovalHist || hasTopics || hasForms || hasGuideHist) {
            user.setEnabled(false);
            userRepository.save(user);
            throw new IllegalStateException("User '" + user.getFullName()
                    + "' has historical records in the system and cannot be physically deleted. The account has been deactivated instead.");
        }

        userRepository.delete(user);
    }

    public static class BulkUserUploadResult {
        private int totalProcessed;
        private int successCount;
        private int failedCount;
        private List<String> errors = new java.util.ArrayList<>();

        public int getTotalProcessed() {
            return totalProcessed;
        }

        public int getSuccessCount() {
            return successCount;
        }

        public int getFailedCount() {
            return failedCount;
        }

        public List<String> getErrors() {
            return errors;
        }

        public void incrementSuccess() {
            successCount++;
            totalProcessed++;
        }

        public void addError(int rowNum, String error) {
            errors.add("Row " + rowNum + ": " + error);
            failedCount++;
            totalProcessed++;
        }
    }

    public BulkUserUploadResult uploadUsersFromCsv(org.springframework.web.multipart.MultipartFile file, User admin) {
        BulkUserUploadResult result = new BulkUserUploadResult();
        List<String> lines = fileStorageService.readCsvLines(file);

        // Skip header line
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty())
                continue;

            String[] parts = parseCsvLine(line);
            if (parts.length < 7) {
                result.addError(i + 1,
                        "Missing required columns. Expected: fullName,email,erpId,rollNumber,role,branch,password");
                continue;
            }

            String fullName = parts[0].trim();
            String email = parts[1].trim();
            String erpId = parts[2].trim();
            String rollNumber = parts[3].trim();
            String roleStr = parts[4].trim().toUpperCase();
            String branchCode = parts[5].trim().toUpperCase();
            String rawPassword = parts[6].trim();

            // Validate required fields
            if (fullName.isEmpty()) {
                result.addError(i + 1, "Full name is required");
                continue;
            }
            if (email.isEmpty()) {
                result.addError(i + 1, "Email is required");
                continue;
            }
            if (rawPassword.isEmpty()) {
                result.addError(i + 1, "Password is required");
                continue;
            }

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

            // Validate ERP ID uniqueness
            if (!erpId.isEmpty() && userRepository.existsByErpId(erpId)) {
                result.addError(i + 1, "ERP ID already in use: " + erpId);
                continue;
            }

            // Validate Roll Number uniqueness
            if (!rollNumber.isEmpty() && userRepository.existsByRollNumber(rollNumber)) {
                result.addError(i + 1, "Roll Number already in use: " + rollNumber);
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

                if (admin != null && admin.getRole() == Role.ADMIN && admin.getBranch() != null) {
                    if (!branch.getId().equals(admin.getBranch().getId())) {
                        result.addError(i + 1, "Access Denied: You can only add users to your assigned branch ("
                                + admin.getBranch().getCode() + ")");
                        continue;
                    }
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
            user.setErpId(erpId.isEmpty() ? null : erpId);
            user.setRollNumber(rollNumber.isEmpty() ? null : rollNumber);
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
