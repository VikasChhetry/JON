package com.project.pas.service;

import com.project.pas.model.Branch;
import com.project.pas.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class BranchService {

    private final BranchRepository branchRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final ProjectTopicRepository topicRepository;
    private final GuideSelectionFormRepository formRepository;
    private final GuideAssignmentRepository assignmentRepository;

    public BranchService(BranchRepository branchRepository, UserRepository userRepository,
                         ProjectRepository projectRepository, ProjectTopicRepository topicRepository,
                         GuideSelectionFormRepository formRepository, GuideAssignmentRepository assignmentRepository) {
        this.branchRepository = branchRepository;
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.topicRepository = topicRepository;
        this.formRepository = formRepository;
        this.assignmentRepository = assignmentRepository;
    }

    public List<Branch> getAllBranches() {
        return branchRepository.findAll();
    }

    public Optional<Branch> getBranchById(Long id) {
        return branchRepository.findById(id);
    }

    public Optional<Branch> getBranchByCode(String code) {
        return branchRepository.findByCode(code);
    }

    public Branch createBranch(String name, String code) {
        if (branchRepository.existsByCode(code)) {
            throw new IllegalArgumentException("Branch code already exists: " + code);
        }
        if (branchRepository.existsByName(name)) {
            throw new IllegalArgumentException("Branch name already exists: " + name);
        }
        Branch branch = new Branch(name, code.toUpperCase());
        return branchRepository.save(branch);
    }

    public Branch updateBranch(Long id, String name, String code) {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Branch not found"));
        branch.setName(name);
        branch.setCode(code.toUpperCase());
        return branchRepository.save(branch);
    }

    public void deleteBranch(Long id) {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Branch not found"));

        boolean hasUsers = userRepository.existsByBranch(branch);
        boolean hasProjects = projectRepository.existsByBranch(branch);
        boolean hasTopics = topicRepository.existsByBranch(branch);
        boolean hasForms = formRepository.existsByBranch(branch);
        boolean hasAssignments = assignmentRepository.existsByBranch(branch);

        if (hasUsers || hasProjects || hasTopics || hasForms || hasAssignments) {
            throw new IllegalStateException("Cannot delete branch '" + branch.getName() + "': it has associated users, projects, topics, or guide assignments. Please remove or reassign those records first.");
        }

        branchRepository.delete(branch);
    }

    public long count() {
        return branchRepository.count();
    }
}
