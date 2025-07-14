package com.org.devgenie.model;

public class SonarIssue {
    private String severity;
    private String category;
    private String className;

    public SonarIssue(String severity, String category) {
        this.severity = severity;
        this.category = category;
    }

    public String getSeverity() {
        return severity;
    }

    public String getCategory() {
        return category;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getClassName() {
        return className;
    }
}
