package com.org.devgenie.model.coverage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimplifiedRepositoryInsights {
    
    private RepositorySummary repositorySummary;
    private CriticalFindings criticalFindings;
    private List<Recommendation> recommendations;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RepositorySummary {
        private String overallRiskLevel; // LOW, MEDIUM, HIGH, CRITICAL
        private Integer complexityScore; // 1-10
        private String coverageGrade; // A, B, C, D, F
        private List<String> primaryConcerns; // Top 3 concerns
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CriticalFindings {
        private List<HighRiskFile> highestRiskFiles;
        private List<String> coverageGaps;
        private List<String> architecturalIssues;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HighRiskFile {
        private String fileName;
        private Double riskScore;
        private String reason;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Recommendation {
        private String priority; // HIGH, MEDIUM, LOW
        private String title;
        private String description;
        private String impact;
        private String effort; // Estimated effort in hours/days
    }
}
