package com.org.devgenie.model.coverage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Document(collection = "coverage_improvement_sessions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoverageImprovementSession {
    @Id
    private String sessionId;
    private String repositoryUrl;
    private String branch;
    private String filePath; // null for repository-level improvements
    private SessionType type; 
    private SessionStatus status;
    private Double progress; // 0-100
    private String currentStep;
    private LocalDateTime startedAt;
    private LocalDateTime estimatedCompletion;
    private Integer processedFiles;
    private Integer totalFiles;
    private Integer currentBatch;
    private Integer totalBatches;
    @Builder.Default
    private List<String> errors = new ArrayList<>(); // Initialize to prevent NPE
    private Object results; // JSON field for results
    private Map<String, Object> metadata;
    private String workspaceId;

    public enum SessionType { 
        FILE_IMPROVEMENT, 
        REPOSITORY_IMPROVEMENT,
        BATCH_FILE_IMPROVEMENT
    }

    public enum SessionStatus {
        CREATED,
        INITIALIZING,
        ANALYZING_REPOSITORY,
        ANALYZING_FILE,
        PRIORITIZING_FILES,
        GENERATING_TESTS,
        VALIDATING_TESTS,
        COMPILING_TESTS,
        CALCULATING_COVERAGE,
        READY_FOR_REVIEW,
        APPLYING_CHANGES,
        CREATING_PR,
        COMPLETED,
        FAILED,
        CANCELLED,
        PARTIALLY_COMPLETED
    }
}
