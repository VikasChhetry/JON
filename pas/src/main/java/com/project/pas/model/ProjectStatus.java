package com.project.pas.model;

public enum ProjectStatus {
    DRAFT("Draft"),
    PROJECT_IDEA_PENDING_FACULTY("Project Idea Pending Faculty Review"),
    PROJECT_IDEA_APPROVED("Project Idea Approved"),
    TOPIC_SELECTED("Topic Selected"),
    PROPOSAL_PENDING_FACULTY("Proposal Pending Faculty Review"),
    PROPOSAL_APPROVED("Proposal Approved"),
    STUDENT_WORKING("Student Working"),
    PENDING_FACULTY_REVIEW("Pending Faculty Review"),
    FACULTY_APPROVED("Faculty Approved"),
    FACULTY_REJECTED("Faculty Rejected"),
    PENDING_HOD_REVIEW("Pending HOD Review"),
    HOD_REJECTED("HOD Rejected"),
    STUDENT_RESUBMISSION("Student Resubmission"),
    COMPLETED("Completed");

    private final String displayName;

    ProjectStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
