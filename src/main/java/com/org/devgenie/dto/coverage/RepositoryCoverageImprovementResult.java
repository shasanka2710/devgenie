package com.org.devgenie.dto.coverage;

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
public class RepositoryCoverageImprovementResult {
    private String sessionId;
    private String repositoryUrl;
    private String branch;
    
    // Processing summary
    private Integer totalFilesProcessed;
    private Integer successfulFiles;
    private Integer failedFiles;
    private Long processingTimeMs;
    
    // Individual file results
    private List<FileCoverageImprovementResult> fileResults;
    
    // Overall coverage improvements
    private Double totalCoverageIncrease;
    private Double averageCoverageIncrease;
    
    // Processing details
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private List<String> warnings;
    private List<String> errors;
    
    // Additional metadata
    private Map<String, Object> metadata;
    
    public enum RepositoryProcessingStatus {
        PENDING, IN_PROGRESS, COMPLETED, FAILED, PARTIAL_SUCCESS
    }
    
    private RepositoryProcessingStatus status;
}
