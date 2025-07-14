package com.org.devgenie.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "coverage")
@Data
public class CoverageConfiguration {

    private double defaultCoverageIncrease = 20.0;
    private int maxFilesToProcess = 50;
    private List<String> excludedFilePatterns = Arrays.asList(
            ".*Test.java",
            ".*IT.java",
            ".*Application.java"
    );
    private boolean enableParallelProcessing = true;
    private int maxRetries = 3;
    private String testFramework = "junit5";
    private boolean enableMocking = true;
    private List<String> mockingFrameworks = Arrays.asList("mockito");

    // NEW: Workspace configuration
    private String workspaceRootDir = "/tmp/coverage-workspaces";
    private long workspaceTimeoutHours = 24;
    private boolean autoCleanupWorkspaces = true;

    @Data
    public static class QualityThresholds {
        private double minimumMethodCoverage = 80.0;
        private double minimumLineCoverage = 75.0;
        private double minimumBranchCoverage = 70.0;
    }

    private QualityThresholds qualityThresholds = new QualityThresholds();
}
