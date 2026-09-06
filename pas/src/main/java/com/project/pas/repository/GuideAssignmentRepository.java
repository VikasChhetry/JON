package com.project.pas.repository;

import com.project.pas.model.Branch;
import com.project.pas.model.GuideAssignment;
import com.project.pas.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GuideAssignmentRepository extends JpaRepository<GuideAssignment, Long> {
    Optional<GuideAssignment> findByStudent(User student);
    List<GuideAssignment> findByFaculty(User faculty);
    List<GuideAssignment> findByBranch(Branch branch);
    long countByFaculty(User faculty);
    boolean existsByStudent(User student);
    void deleteByStudent(User student);
}
