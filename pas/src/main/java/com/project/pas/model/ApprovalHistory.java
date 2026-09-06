package com.project.pas.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "approval_history")
public class ApprovalHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false)
    private String action;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "performed_by", nullable = false)
    private User performedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role userRole;

    @Enumerated(EnumType.STRING)
    private ProjectStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectStatus newStatus;

    @Column(columnDefinition = "TEXT")
    private String comments;

    @Column(columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;

    public ApprovalHistory() {
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

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
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

    public Role getUserRole() {
        return userRole;
    }

    public void setUserRole(Role userRole) {
        this.userRole = userRole;
    }

    public ProjectStatus getPreviousStatus() {
        return previousStatus;
    }

    public void setPreviousStatus(ProjectStatus previousStatus) {
        this.previousStatus = previousStatus;
    }

    public ProjectStatus getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(ProjectStatus newStatus) {
        this.newStatus = newStatus;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
