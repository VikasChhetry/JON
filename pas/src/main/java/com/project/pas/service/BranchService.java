package com.project.pas.service;

import com.project.pas.model.Branch;
import com.project.pas.repository.BranchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class BranchService {

    private final BranchRepository branchRepository;

    public BranchService(BranchRepository branchRepository) {
        this.branchRepository = branchRepository;
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
        branchRepository.deleteById(id);
    }

    public long count() {
        return branchRepository.count();
    }
}
