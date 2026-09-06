package com.project.pas.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Table(name = "guide_assignments", uniqueConstraints = {
        @UniqueConstraint(columnNames = "student_id")
})
public class GuideAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "student_id", nullable = false, unique = true)
    private User student;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "faculty_id", nullable = false)
    private User faculty;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "selection_form_id")
    private GuideSelectionForm selectionForm;

    @Column(nullable = false)
    private LocalDateTime assignedAt;

    /**
     * Who initiated this assignment: STUDENT, HOD, or AUTO
     */
    @Column(nullable = false, length = 20)
    private String assignedBy;

    public GuideAssignment() {
    }

    @PrePersist
    protected void onCreate() {
        this.assignedAt = LocalDateTime.now();
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getStudent() {
        return student;
    }

    public void setStudent(User student) {
        this.student = student;
    }

    public User getFaculty() {
        return faculty;
    }

    public void setFaculty(User faculty) {
        this.faculty = faculty;
    }

    public Branch getBranch() {
        return branch;
    }

    public void setBranch(Branch branch) {
        this.branch = branch;
    }

    public GuideSelectionForm getSelectionForm() {
        return selectionForm;
    }

    public void setSelectionForm(GuideSelectionForm selectionForm) {
        this.selectionForm = selectionForm;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }

    public String getAssignedBy() {
        return assignedBy;
    }

    public void setAssignedBy(String assignedBy) {
        this.assignedBy = assignedBy;
    }
}
