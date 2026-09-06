package com.project.pas.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "projects")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Project title is required")
    @Size(max = 200)
    @Column(nullable = false)
    private String title;

    @NotBlank(message = "Project description is required")
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Size(max = 100)
    private String category;

    @Size(max = 300)
    private String techStack;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectStatus status = ProjectStatus.DRAFT;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectType projectType;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "topic_id")
    private ProjectTopic topic; // nullable — only set for TOPIC_BASED projects

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "faculty_guide_id")
    private User facultyGuide; // The assigned guide who reviews this project

    private String reportFilePath;

    private String projectFilePath;

    // Track which review stage we were at before rejection, so resubmission goes back to the right reviewer
    @Enumerated(EnumType.STRING)
    private ProjectStatus rejectedAtStage;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("timestamp DESC")
    private List<ApprovalHistory> approvalHistory = new ArrayList<>();

    public Project() {
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getTechStack() {
        return techStack;
    }

    public void setTechStack(String techStack) {
        this.techStack = techStack;
    }

    public User getStudent() {
        return student;
    }

    public void setStudent(User student) {
        this.student = student;
    }

    public Branch getBranch() {
        return branch;
    }

    public void setBranch(Branch branch) {
        this.branch = branch;
    }

    public ProjectStatus getStatus() {
        return status;
    }

    public void setStatus(ProjectStatus status) {
        this.status = status;
    }

    public ProjectType getProjectType() {
        return projectType;
    }

    public void setProjectType(ProjectType projectType) {
        this.projectType = projectType;
    }

    public ProjectTopic getTopic() {
        return topic;
    }

    public void setTopic(ProjectTopic topic) {
        this.topic = topic;
    }

    public String getReportFilePath() {
        return reportFilePath;
    }

    public void setReportFilePath(String reportFilePath) {
        this.reportFilePath = reportFilePath;
    }

    public String getProjectFilePath() {
        return projectFilePath;
    }

    public void setProjectFilePath(String projectFilePath) {
        this.projectFilePath = projectFilePath;
    }

    public ProjectStatus getRejectedAtStage() {
        return rejectedAtStage;
    }

    public void setRejectedAtStage(ProjectStatus rejectedAtStage) {
        this.rejectedAtStage = rejectedAtStage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public User getFacultyGuide() {
        return facultyGuide;
    }

    public void setFacultyGuide(User facultyGuide) {
        this.facultyGuide = facultyGuide;
    }

    public List<ApprovalHistory> getApprovalHistory() {
        return approvalHistory;
    }

    public void setApprovalHistory(List<ApprovalHistory> approvalHistory) {
        this.approvalHistory = approvalHistory;
    }
}
