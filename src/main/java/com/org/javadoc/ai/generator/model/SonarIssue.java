package com.org.javadoc.ai.generator.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SonarIssue {

    private String key;
    private String type;
    private String severity;
    private String description;
    private String category;
    private String totalIssues;
    private String totalEffort;

}
