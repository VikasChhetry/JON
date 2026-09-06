package com.project.pas.model;

public enum TopicStatus {
    AVAILABLE("Available"),
    ASSIGNED("Assigned"),
    ARCHIVED("Archived");

    private final String displayName;

    TopicStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
