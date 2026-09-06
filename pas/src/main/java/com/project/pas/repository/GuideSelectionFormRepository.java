package com.project.pas.repository;

import com.project.pas.model.Branch;
import com.project.pas.model.GuideSelectionForm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GuideSelectionFormRepository extends JpaRepository<GuideSelectionForm, Long> {
    Optional<GuideSelectionForm> findByBranchAndActiveTrue(Branch branch);
    List<GuideSelectionForm> findByBranchOrderByCreatedAtDesc(Branch branch);
}
