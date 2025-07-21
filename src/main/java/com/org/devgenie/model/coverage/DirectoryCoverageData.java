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
public class DirectoryCoverageData {
    private String directoryPath; // Relative to repo root, e.g., src/main/java/com/org/devgenie/service
    private String directoryName; // Just the directory name without path
    private String parentPath; // Parent directory path for breadcrumb navigation
    
    // Coverage metrics
    private double overallCoverage;
    private double lineCoverage;
    private double branchCoverage;
    private double methodCoverage;
    private int totalLines;
    private int coveredLines;
    private int totalBranches;
    private int coveredBranches;
    private int totalMethods;
    private int coveredMethods;
    
    // Hierarchy structure
    private List<FileCoverageData> files; // List of file coverage data directly under this directory
    private List<DirectoryCoverageData> subdirectories; // List of subdirectory DirectoryCoverageData objects
    
    // UI support fields
    private int totalFileCount; // Total files in this directory and all subdirectories
    private int highRiskFileCount; // Files with risk score > 70
    private int lowCoverageFileCount; // Files with coverage < 50%
    
    // Directory-level improvement opportunities
    private List<DirectoryImprovementSummary> improvementSummary;
    
    private LocalDateTime lastUpdated;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DirectoryImprovementSummary {
        private String category; // e.g., "High Risk Files", "Low Coverage Files"
        private int count;
        private String description;
        private String priority;
        private double estimatedImpact;
    }
}
