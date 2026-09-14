package com.project.pas.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * Represents a team member in a project.
 * Links a User (STUDENT) to a Project with their contribution/role.
 * The project owner is automatically included as Team Leader.
 */
@Entity
@Table(name = "project_team_members", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "project_id", "member_id" })
})
public class ProjectTeamMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "member_id", nullable = false)
    private User member;

    /**
     * The contribution or role of this team member in the project.
     * Examples: Backend Developer, Frontend Developer, Database Designer, etc.
     */
    @NotBlank(message = "Contribution/Role is required")
    @Size(max = 200)
    @Column(nullable = false)
    private String contributionRole;

    /**
     * Whether this member is the project owner / team leader.
     */
    @Column(nullable = false)
    private boolean isOwner = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime addedAt;

    public ProjectTeamMember() {
    }

    @PrePersist
    protected void onCreate() {
        this.addedAt = LocalDateTime.now();
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

    public User getMember() {
        return member;
    }

    public void setMember(User member) {
        this.member = member;
    }

    public String getContributionRole() {
        return contributionRole;
    }

    public void setContributionRole(String contributionRole) {
        this.contributionRole = contributionRole;
    }

    public boolean isOwner() {
        return isOwner;
    }

    public void setOwner(boolean owner) {
        isOwner = owner;
    }

    public LocalDateTime getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(LocalDateTime addedAt) {
        this.addedAt = addedAt;
    }
}
