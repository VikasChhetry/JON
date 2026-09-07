package com.project.pas.repository;

import com.project.pas.model.ApprovalHistory;
import com.project.pas.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApprovalHistoryRepository extends JpaRepository<ApprovalHistory, Long> {
    List<ApprovalHistory> findByProjectOrderByTimestampDesc(Project project);
    List<ApprovalHistory> findByProjectIdOrderByTimestampDesc(Long projectId);
    boolean existsByPerformedBy(com.project.pas.model.User performedBy);
    void deleteByProject(Project project);
}
