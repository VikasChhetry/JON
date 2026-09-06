package com.project.pas.repository;

import com.project.pas.model.Branch;
import com.project.pas.model.GuideAssignmentHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GuideAssignmentHistoryRepository extends JpaRepository<GuideAssignmentHistory, Long> {
    List<GuideAssignmentHistory> findByBranchOrderByTimestampDesc(Branch branch);
}
