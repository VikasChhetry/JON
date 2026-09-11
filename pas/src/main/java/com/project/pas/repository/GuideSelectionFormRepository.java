package com.project.pas.repository;

import com.project.pas.model.Branch;
import com.project.pas.model.GuideSelectionForm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface GuideSelectionFormRepository extends JpaRepository<GuideSelectionForm, Long> {
    Optional<GuideSelectionForm> findByBranchAndActiveTrue(Branch branch);

    List<GuideSelectionForm> findByBranchOrderByCreatedAtDesc(Branch branch);

    boolean existsByBranch(Branch branch);

    boolean existsByCreatedBy(com.project.pas.model.User createdBy);

    /**
     * Finds active forms whose selection period has ended but auto-assignment
     * has not yet been processed. Used by the scheduler.
     */
    @Query("SELECT f FROM GuideSelectionForm f WHERE f.active = true AND f.endDateTime <= :now AND f.autoProcessed = false")
    List<GuideSelectionForm> findExpiredUnprocessedForms(@Param("now") LocalDateTime now);
}
