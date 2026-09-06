package com.project.pas.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@Entity
@Table(name = "branches")
public class Branch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Branch name is required")
    @Size(max = 100)
    @Column(nullable = false)
    private String name;

    @NotBlank(message = "Branch code is required")
    @Size(max = 20)
    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Branch() {
    }

    public Branch(String name, String code) {
        this.name = name;
        this.code = code;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return name + " (" + code + ")";
    }
}
