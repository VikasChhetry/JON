package com.project.pas.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@Entity
@Table(name = "project_topics")
public class ProjectTopic {

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

    @NotNull(message = "Branch is required")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @NotNull(message = "Creator is required")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TopicStatus status = TopicStatus.AVAILABLE;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public ProjectTopic() {
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
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

    public Branch getBranch() {
        return branch;
    }

    public void setBranch(Branch branch) {
        this.branch = branch;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public TopicStatus getStatus() {
        return status;
    }

    public void setStatus(TopicStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
