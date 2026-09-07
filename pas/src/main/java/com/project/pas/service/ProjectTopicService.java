package com.project.pas.service;

import com.project.pas.model.*;
import com.project.pas.repository.ProjectTopicRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ProjectTopicService {

    private final ProjectTopicRepository topicRepository;
    private final FileStorageService fileStorageService;

    public ProjectTopicService(ProjectTopicRepository topicRepository, FileStorageService fileStorageService) {
        this.topicRepository = topicRepository;
        this.fileStorageService = fileStorageService;
    }

    /**
     * Get all available topics for a branch (for students to select).
     */
    public List<ProjectTopic> getAvailableTopics(Branch branch) {
        return topicRepository.findByBranchAndStatus(branch, TopicStatus.AVAILABLE);
    }

    /**
     * Get all topics for a branch (for Faculty/HOD management).
     */
    public List<ProjectTopic> getTopicsByBranch(Branch branch) {
        return topicRepository.findByBranch(branch);
    }

    public Optional<ProjectTopic> getTopicById(Long id) {
        return topicRepository.findById(id);
    }

    /**
     * Create a single topic manually.
     * Branch enforcement: creator's branch must match the topic's branch.
     */
    public ProjectTopic createTopic(String title, String description, String category,
                                     String techStack, User creator) {
        validateBranchAccess(creator);

        ProjectTopic topic = new ProjectTopic();
        topic.setTitle(title);
        topic.setDescription(description);
        topic.setCategory(category);
        topic.setTechStack(techStack);
        topic.setBranch(creator.getBranch());
        topic.setCreatedBy(creator);
        topic.setStatus(TopicStatus.AVAILABLE);

        return topicRepository.save(topic);
    }

    /**
     * Upload topics from a CSV file.
     * CSV format: title,description,category,techStack
     * First line is treated as header and skipped.
     */
    public List<ProjectTopic> uploadTopicsFromCsv(MultipartFile file, User creator) {
        validateBranchAccess(creator);

        List<String> lines = fileStorageService.readCsvLines(file);
        List<ProjectTopic> topics = new ArrayList<>();

        // Skip header line
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) continue;

            String[] parts = parseCsvLine(line);
            if (parts.length < 2) continue; // Need at least title and description

            ProjectTopic topic = new ProjectTopic();
            topic.setTitle(parts[0].trim());
            topic.setDescription(parts[1].trim());
            topic.setCategory(parts.length > 2 ? parts[2].trim() : "");
            topic.setTechStack(parts.length > 3 ? parts[3].trim() : "");
            topic.setBranch(creator.getBranch());
            topic.setCreatedBy(creator);
            topic.setStatus(TopicStatus.AVAILABLE);

            topics.add(topicRepository.save(topic));
        }

        return topics;
    }

    /**
     * Mark topic as assigned when selected by a student.
     */
    public void markAsAssigned(ProjectTopic topic) {
        topic.setStatus(TopicStatus.ASSIGNED);
        topicRepository.save(topic);
    }

    /**
     * Release an assigned topic back to available (e.g., when a project is deleted).
     */
    public void releaseAssigned(ProjectTopic topic) {
        topic.setStatus(TopicStatus.AVAILABLE);
        topicRepository.save(topic);
    }

    /**
     * Archive a topic.
     */
    public void archiveTopic(Long id, User user) {
        ProjectTopic topic = topicRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Topic not found"));
        validateBranchMatch(user, topic.getBranch());
        topic.setStatus(TopicStatus.ARCHIVED);
        topicRepository.save(topic);
    }

    public long countByBranch(Branch branch) {
        return topicRepository.countByBranch(branch);
    }

    // --- Branch enforcement helpers ---

    private void validateBranchAccess(User user) {
        if (user.getBranch() == null) {
            throw new SecurityException("User has no branch assigned");
        }
        if (user.getRole() != Role.FACULTY && user.getRole() != Role.HOD) {
            throw new SecurityException("Only Faculty and HOD can manage topics");
        }
    }

    private void validateBranchMatch(User user, Branch branch) {
        if (user.getBranch() == null || !user.getBranch().getId().equals(branch.getId())) {
            throw new SecurityException("Access denied: branch mismatch");
        }
    }

    /**
     * Simple CSV line parser that handles quoted fields.
     */
    private String[] parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();

        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        result.add(current.toString());
        return result.toArray(new String[0]);
    }
}
