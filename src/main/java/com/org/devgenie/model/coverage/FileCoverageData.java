package com.org.devgenie.model.coverage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileCoverageData {
    private String filePath;
    private String fileName; // Just the file name without path
    private String className; // Java class name
    private String packageName; // Java package name
    
    // Coverage metrics
    private double lineCoverage;
    private double branchCoverage;
    private double methodCoverage; // ADDED
    private int totalLines;
    private int coveredLines;
    private int totalMethods;
    private int coveredMethods;
    private int totalBranches; // ADDED
    private int coveredBranches; // ADDED
    
    // Detailed coverage information
    private List<String> uncoveredLines;
    private List<String> uncoveredBranches;
    private List<UncoveredMethodInfo> uncoveredMethods;
    
    // File metadata and analysis
    private FileComplexityMetrics complexityMetrics;
    private List<FileImprovementOpportunity> improvementOpportunities;
    
    // System information
    private LocalDateTime lastUpdated;
    private String buildTool;
    private String testFramework;
    private String coverageSource; // SONARQUBE, JACOCO, etc.
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UncoveredMethodInfo {
        private String methodName;
        private String methodSignature;
        private int startLine;
        private int endLine;
        private int cyclomaticComplexity;
        private double riskScore;
        private boolean isBusinessCritical;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileComplexityMetrics {
        private int cyclomaticComplexity;
        private int cognitiveComplexity;
        private double businessCriticality;
        private double riskScore;
        private int methodCount;
        private double averageMethodLength;
        private int maxNestingDepth;
    }
}
