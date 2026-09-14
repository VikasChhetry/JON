package com.project.pas.repository;

import com.project.pas.model.Project;
import com.project.pas.model.ProjectTeamMember;
import com.project.pas.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectTeamMemberRepository extends JpaRepository<ProjectTeamMember, Long> {
    List<ProjectTeamMember> findByProject(Project project);

    List<ProjectTeamMember> findByMember(User member);

    Optional<ProjectTeamMember> findByProjectAndMember(Project project, User member);

    boolean existsByProjectAndMember(Project project, User member);

    /**
     * Check if a user is a team member in ANY project.
     */
    boolean existsByMember(User member);

    long countByProject(Project project);

    void deleteByProjectAndMember(Project project, User member);

    void deleteByProject(Project project);
}
