package com.project.pas.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Table(name = "guide_assignment_history")
public class GuideAssignmentHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String action;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "performed_by", nullable = false)
    private User performedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role performerRole;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "previous_faculty_id")
    private User previousFaculty;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "new_faculty_id")
    private User newFaculty;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    public GuideAssignmentHistory() {
    }

    @PrePersist
    protected void onCreate() {
        this.timestamp = LocalDateTime.now();
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public User getPerformedBy() {
        return performedBy;
    }

    public void setPerformedBy(User performedBy) {
        this.performedBy = performedBy;
    }

    public Role getPerformerRole() {
        return performerRole;
    }

    public void setPerformerRole(Role performerRole) {
        this.performerRole = performerRole;
    }

    public User getStudent() {
        return student;
    }

    public void setStudent(User student) {
        this.student = student;
    }

    public User getPreviousFaculty() {
        return previousFaculty;
    }

    public void setPreviousFaculty(User previousFaculty) {
        this.previousFaculty = previousFaculty;
    }

    public User getNewFaculty() {
        return newFaculty;
    }

    public void setNewFaculty(User newFaculty) {
        this.newFaculty = newFaculty;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Branch getBranch() {
        return branch;
    }

    public void setBranch(Branch branch) {
        this.branch = branch;
    }
}
