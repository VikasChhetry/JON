package com.project.pas.model;

public enum ProjectType {
    CUSTOM("Custom Project"),
    TOPIC_BASED("Topic Based Project");

    private final String displayName;

    ProjectType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
