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
    @Column(columnDefinition = "VARCHAR(255)")
    private ProjectStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(255)")
    private ProjectStatus newStatus;

    @Column(columnDefinition = "TEXT")
    private String comments;

    @Column(columnDefinition = "TEXT")
    private String rejectionReason;

    private Integer previousRequestedTeamSize;
    private Integer newRequestedTeamSize;

    private Integer previousMinTeamSize;
    private Integer newMinTeamSize;

    private Integer previousMaxTeamSize;
    private Integer newMaxTeamSize;

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

    public Integer getPreviousRequestedTeamSize() {
        return previousRequestedTeamSize;
    }

    public void setPreviousRequestedTeamSize(Integer previousRequestedTeamSize) {
        this.previousRequestedTeamSize = previousRequestedTeamSize;
    }

    public Integer getNewRequestedTeamSize() {
        return newRequestedTeamSize;
    }

    public void setNewRequestedTeamSize(Integer newRequestedTeamSize) {
        this.newRequestedTeamSize = newRequestedTeamSize;
    }

    public Integer getPreviousMinTeamSize() {
        return previousMinTeamSize;
    }

    public void setPreviousMinTeamSize(Integer previousMinTeamSize) {
        this.previousMinTeamSize = previousMinTeamSize;
    }

    public Integer getNewMinTeamSize() {
        return newMinTeamSize;
    }

    public void setNewMinTeamSize(Integer newMinTeamSize) {
        this.newMinTeamSize = newMinTeamSize;
    }

    public Integer getPreviousMaxTeamSize() {
        return previousMaxTeamSize;
    }

    public void setPreviousMaxTeamSize(Integer previousMaxTeamSize) {
        this.previousMaxTeamSize = previousMaxTeamSize;
    }

    public Integer getNewMaxTeamSize() {
        return newMaxTeamSize;
    }

    public void setNewMaxTeamSize(Integer newMaxTeamSize) {
        this.newMaxTeamSize = newMaxTeamSize;
    }
}
