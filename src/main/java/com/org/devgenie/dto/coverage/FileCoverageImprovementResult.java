package com.org.devgenie.dto.coverage;

import com.org.devgenie.model.coverage.GeneratedTestInfo;
import com.org.devgenie.model.coverage.TestValidationResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileCoverageImprovementResult {
    private String sessionId;
    private String filePath;
    private String fileName;
    private String packageName;
    
    // Coverage metrics
    private Double originalCoverage;
    private Double improvedCoverage;
    private Double coverageIncrease;
    private CoverageBreakdown beforeBreakdown;
    private CoverageBreakdown afterBreakdown;
    
    // Generated tests information
    private List<GeneratedTestInfo> generatedTests;
    private Integer totalTestsGenerated;
    private List<String> testFilePaths;
    
    // Processing details
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private Long processingTimeMs;
    private Integer batchesProcessed;
    private Integer retryCount;
    
    // Validation results
    private TestValidationResult validationResult;
    private Boolean testsCompiled;
    private Boolean testsExecuted;
    
    // Status and recommendations
    private ProcessingStatus status;
    private List<String> recommendations;
    private List<String> warnings;
    private List<String> errors;
    
    // Additional metadata
    private Map<String, Object> metadata;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CoverageBreakdown {
        private Double lineCoverage;
        private Double branchCoverage;
        private Double methodCoverage;
        private Integer totalLines;
        private Integer coveredLines;
        private Integer totalBranches;
        private Integer coveredBranches;
        private Integer totalMethods;
        private Integer coveredMethods;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeneratedTestInfo {
        private String testMethodName;
        private String testClass;
        private String description;
        private List<String> coveredMethods;
        private Double estimatedCoverageContribution;
    }
    
    public enum ProcessingStatus {
        PENDING, IN_PROGRESS, COMPLETED, FAILED, PARTIAL_SUCCESS
    }
}
