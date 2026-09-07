package com.project.pas.repository;

import com.project.pas.model.Branch;
import com.project.pas.model.Role;
import com.project.pas.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findByBranchAndRole(Branch branch, Role role);
    List<User> findByRole(Role role);
    List<User> findByBranch(Branch branch);
    boolean existsByBranch(Branch branch);
    long countByRole(Role role);
}
