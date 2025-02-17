package com.org.devgenie.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SonarIssue {

    private String key;
    private String type;
    private String severity;
    private String description;
    private String category;
    private String className;
    private List<String> softwareQuality;
}
