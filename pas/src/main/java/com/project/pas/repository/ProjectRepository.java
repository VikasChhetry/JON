package com.project.pas.repository;

import com.project.pas.model.Branch;
import com.project.pas.model.Project;
import com.project.pas.model.ProjectStatus;
import com.project.pas.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByStudent(User student);
    List<Project> findByBranch(Branch branch);
    List<Project> findByBranchAndStatus(Branch branch, ProjectStatus status);
    List<Project> findByBranchAndStatusIn(Branch branch, List<ProjectStatus> statuses);
    boolean existsByStudentAndStatusNotIn(User student, List<ProjectStatus> excludedStatuses);
    long countByBranch(Branch branch);
    long countByBranchAndStatus(Branch branch, ProjectStatus status);
    long countByStatus(ProjectStatus status);
}
