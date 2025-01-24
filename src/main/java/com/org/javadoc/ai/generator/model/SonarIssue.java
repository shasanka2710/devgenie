package com.org.javadoc.ai.generator.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SonarIssue {

    private String id;
    private String type;
    private String severity;
    private String message;
    private String component;
}
