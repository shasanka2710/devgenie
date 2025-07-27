package com.org.devgenie.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImprovementRecordDto {
    private String sessionId;
    private String type; // FILE_IMPROVEMENT, REPOSITORY_IMPROVEMENT
    private String status;
    private String repositoryUrl;
    private String branch;
    private String filePath;
    private String fileName;
    private Double originalCoverage;
    private Double improvedCoverage;
    private Double coverageIncrease;
    private Integer totalTestsGenerated;
    private Long processingTimeMs;
    private String startedAt;
    private String completedAt;
    private Boolean testsCompiled;
    private Boolean testsExecuted;
    private ValidationDetails validation;
    private List<String> recommendations;
    private List<String> warnings;
    private List<String> errors;
    private List<GeneratedTestDto> generatedTests;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidationDetails {
        private Boolean success;
        private Integer testsExecuted;
        private Integer testsPassed;
        private Integer testsFailed;
        private Long executionTimeMs;
        private String validationMethod;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeneratedTestDto {
        private String testMethodName;
        private String testClass;
        private String description;
        private List<String> coveredMethods;
        private Integer estimatedCoverageContribution;
    }
}
