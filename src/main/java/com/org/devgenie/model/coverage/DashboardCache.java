package com.org.devgenie.model.coverage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "dashboard_cache")
public class DashboardCache {
    
    @Id
    private String id;
    
    private String repoPath;
    private String branch;
    private LocalDateTime generatedAt;
    private LocalDateTime lastUpdated;
    
    // Pre-computed dashboard data
    private OverallMetrics overallMetrics;
    private FileTreeData fileTreeData;
    private List<FileDetailsData> fileDetails;
    
    // Metadata
    private int totalFiles;
    private int totalDirectories;
    private String status; // PROCESSING, COMPLETED, ERROR
    private String errorMessage;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OverallMetrics {
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
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileTreeData {
        private String name;
        private String type;
        private String path;
        private Double lineCoverage;
        private Double branchCoverage;
        private Double methodCoverage;
        private List<FileTreeData> children;
        
        // Package-style tree support
        private String nodeType;        // PACKAGE, DIRECTORY, FILE
        private String packageName;     // For package nodes (e.g., com.example.service)
        private Boolean flattened;      // Whether this node was flattened
        private Boolean autoExpanded;   // Whether this node should be auto-expanded in UI
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileDetailsData {
        private String fileName;
        private String filePath;
        private String packageName;
        private String className;
        private double lineCoverage;
        private double branchCoverage;
        private double methodCoverage;
        private int totalLines;
        private int coveredLines;
        private int totalBranches;
        private int coveredBranches;
        private int totalMethods;
        private int coveredMethods;
        private List<ImprovementOpportunityData> improvementOpportunities;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImprovementOpportunityData {
        private String type;
        private String description;
        private String priority;
        private String estimatedImpact;
    }
}
