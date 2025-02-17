package com.org.devgenie.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "javadoc")
@Getter
@Setter
public class AppConfig {

    private String tempDir = "/tmp/javadoc-generator";
    private boolean overwriteExisting = false;
    private boolean enableAi = false;

    private List<String> includePaths = new ArrayList<>();
    private List<String> excludePaths = new ArrayList<>();
    private boolean dryRun = false;
    private Map<String, String> aiConfig = new HashMap<>();
    private int cyclomaticComplexityThreshold=5;
    private int callGraphDepth = 3;
}