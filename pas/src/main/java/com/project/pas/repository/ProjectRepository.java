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

    boolean existsByStudent(User student);

    boolean existsByFacultyGuide(User facultyGuide);

    boolean existsByBranch(Branch branch);

    boolean existsByStudentAndStatusNotIn(User student, List<ProjectStatus> excludedStatuses);

    long countByBranch(Branch branch);

    long countByBranchAndStatus(Branch branch, ProjectStatus status);

    long countByStatus(ProjectStatus status);

    // Faculty guide-scoped queries — used to restrict faculty visibility to their
    // own guided projects
    List<Project> findByFacultyGuide(User facultyGuide);

    List<Project> findByFacultyGuideAndStatusIn(User facultyGuide, List<ProjectStatus> statuses);

    long countByFacultyGuide(User facultyGuide);

    long countByFacultyGuideAndStatus(User facultyGuide, ProjectStatus status);
}
