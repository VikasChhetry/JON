package com.project.pas.repository;

import com.project.pas.model.Branch;
import com.project.pas.model.ProjectTopic;
import com.project.pas.model.TopicStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectTopicRepository extends JpaRepository<ProjectTopic, Long> {
    List<ProjectTopic> findByBranchAndStatus(Branch branch, TopicStatus status);
    List<ProjectTopic> findByBranch(Branch branch);
    long countByBranch(Branch branch);
}
