package com.project.pas.specification;

import com.project.pas.model.Branch;
import com.project.pas.model.Project;
import com.project.pas.model.ProjectStatus;
import com.project.pas.model.ProjectTopic;
import com.project.pas.model.Role;
import com.project.pas.model.TopicStatus;
import com.project.pas.model.User;
import com.project.pas.model.GuideAssignment;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class SearchSpecifications {

    public static Specification<User> userSearch(String keyword, Role roleFilter, Branch branch, Boolean statusFilter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(keyword)) {
                String searchPattern = "%" + keyword.toLowerCase().trim() + "%";
                Predicate fullName = cb.like(cb.lower(root.get("fullName")), searchPattern);
                Predicate email = cb.like(cb.lower(root.get("email")), searchPattern);
                Predicate erpId = cb.like(cb.lower(root.get("erpId")), searchPattern);
                Predicate rollNumber = cb.like(cb.lower(root.get("rollNumber")), searchPattern);
                predicates.add(cb.or(fullName, email, erpId, rollNumber));
            }

            if (roleFilter != null) {
                predicates.add(cb.equal(root.get("role"), roleFilter));
            }

            if (branch != null) {
                predicates.add(cb.equal(root.get("branch"), branch));
            }

            if (statusFilter != null) {
                predicates.add(cb.equal(root.get("enabled"), statusFilter));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Branch> branchSearch(String keyword) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(keyword))
                return cb.conjunction();
            String searchPattern = "%" + keyword.toLowerCase().trim() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("name")), searchPattern),
                    cb.like(cb.lower(root.get("code")), searchPattern));
        };
    }

    public static Specification<Project> projectSearch(String keyword, Branch branch, User facultyGuide,
            ProjectStatus statusFilter, List<ProjectStatus> statusInFilter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(keyword)) {
                String searchPattern = "%" + keyword.toLowerCase().trim() + "%";
                Predicate title = cb.like(cb.lower(root.get("title")), searchPattern);
                Predicate category = cb.like(cb.lower(root.get("category")), searchPattern);
                Predicate techStack = cb.like(cb.lower(root.get("techStack")), searchPattern);
                Predicate studentName = cb.like(cb.lower(root.join("student", JoinType.LEFT).get("fullName")),
                        searchPattern);
                predicates.add(cb.or(title, category, techStack, studentName));
            }

            if (branch != null) {
                predicates.add(cb.equal(root.get("branch"), branch));
            }

            if (facultyGuide != null) {
                predicates.add(cb.equal(root.get("facultyGuide"), facultyGuide));
            }

            if (statusFilter != null) {
                predicates.add(cb.equal(root.get("status"), statusFilter));
            }

            if (statusInFilter != null && !statusInFilter.isEmpty()) {
                predicates.add(root.get("status").in(statusInFilter));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<ProjectTopic> topicSearch(String keyword, Branch branch, TopicStatus statusFilter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(keyword)) {
                String searchPattern = "%" + keyword.toLowerCase().trim() + "%";
                Predicate title = cb.like(cb.lower(root.get("title")), searchPattern);
                Predicate category = cb.like(cb.lower(root.get("category")), searchPattern);
                Predicate techStack = cb.like(cb.lower(root.get("techStack")), searchPattern);
                predicates.add(cb.or(title, category, techStack));
            }

            if (branch != null) {
                predicates.add(cb.equal(root.get("branch"), branch));
            }

            if (statusFilter != null) {
                predicates.add(cb.equal(root.get("status"), statusFilter));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<GuideAssignment> guideAssignmentSearch(String keyword, Branch branch, User faculty) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(keyword)) {
                String searchPattern = "%" + keyword.toLowerCase().trim() + "%";
                Predicate studentName = cb.like(cb.lower(root.join("student", JoinType.LEFT).get("fullName")),
                        searchPattern);
                Predicate studentEmail = cb.like(cb.lower(root.join("student", JoinType.LEFT).get("email")),
                        searchPattern);
                Predicate studentErpId = cb.like(cb.lower(root.join("student", JoinType.LEFT).get("erpId")),
                        searchPattern);
                Predicate studentRollNumber = cb.like(cb.lower(root.join("student", JoinType.LEFT).get("rollNumber")),
                        searchPattern);

                Predicate facultyName = cb.like(cb.lower(root.join("faculty", JoinType.LEFT).get("fullName")),
                        searchPattern);
                Predicate facultyEmail = cb.like(cb.lower(root.join("faculty", JoinType.LEFT).get("email")),
                        searchPattern);

                predicates.add(
                        cb.or(studentName, studentEmail, studentErpId, studentRollNumber, facultyName, facultyEmail));
            }

            if (branch != null) {
                predicates.add(cb.equal(root.get("branch"), branch));
            }

            if (faculty != null) {
                predicates.add(cb.equal(root.get("faculty"), faculty));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
