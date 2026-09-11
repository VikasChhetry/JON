package com.project.pas.config;

import com.project.pas.service.GuideSelectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler that automatically assigns faculty guides to unassigned students
 * once a guide selection period ends.
 *
 * Fires every 60 seconds. For each active GuideSelectionForm whose endDateTime
 * has passed and whose autoProcessed flag is false, it delegates to
 * {@link GuideSelectionService#processExpiredForms()} which:
 * 1. Finds all unassigned students in that branch.
 * 2. Distributes them among faculty with available capacity (lowest-load
 * first).
 * 3. Creates GuideAssignment records (assignedBy = "AUTO").
 * 4. Creates GuideAssignmentHistory records (action = "AUTO ASSIGNED GUIDE").
 * 5. Marks the form autoProcessed = true to prevent duplicate runs.
 *
 * This is idempotent — students who already have a guide are always skipped.
 */
@Component
public class GuideSelectionScheduler {

    private static final Logger log = LoggerFactory.getLogger(GuideSelectionScheduler.class);

    private final GuideSelectionService guideSelectionService;

    public GuideSelectionScheduler(GuideSelectionService guideSelectionService) {
        this.guideSelectionService = guideSelectionService;
    }

    /**
     * Runs every 60 seconds.
     * Checks for expired, unprocessed guide selection forms and triggers
     * automatic faculty assignment for the remaining unassigned students.
     */
    @Scheduled(fixedDelay = 60_000)
    public void autoAssignAfterSelectionEnds() {
        log.debug("[AutoAssign Scheduler] Checking for expired guide selection forms...");
        try {
            guideSelectionService.processExpiredForms();
        } catch (Exception ex) {
            log.error("[AutoAssign Scheduler] Unexpected error during processing: {}", ex.getMessage(), ex);
        }
    }
}
