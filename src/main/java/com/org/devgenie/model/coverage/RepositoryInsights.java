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
public class RepositoryInsights {
    private String repositoryComplexity; // LOW, MEDIUM, HIGH
    private List<String> dominantPatterns;
    private List<TestingGap> testingGaps;
    private List<String> architecturalInsights;
    private CoverageStrategy coverageStrategy;
    private RiskAssessment riskAssessment;
    
    // Enhanced contextual insights
    private String executiveSummary; // High-level assessment
    private List<String> keyFindings; // Most important discoveries
    private List<String> criticalActions; // Immediate priority actions
    private CodeQualityAssessment codeQualityAssessment;
    private BusinessImpactAnalysis businessImpactAnalysis;
    private TechnicalDebtAssessment technicalDebtAssessment;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CodeQualityAssessment {
        private String overallGrade; // A, B, C, D, F
        private String complexityLevel; // LOW, MEDIUM, HIGH
        private String maintainabilityScore; // Percentage
        private List<String> qualityIssues;
        private List<String> strengthAreas;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BusinessImpactAnalysis {
        private String riskLevel; // LOW, MEDIUM, HIGH, CRITICAL
        private String businessCriticalityLevel;
        private List<String> highRiskComponents;
        private String estimatedBusinessImpact;
        private List<String> complianceConsiderations;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TechnicalDebtAssessment {
        private String debtLevel; // LOW, MEDIUM, HIGH
        private String estimatedRefactoringEffort;
        private List<String> debtHotspots;
        private List<String> refactoringPriorities;
        private String maintainabilityTrend; // IMPROVING, STABLE, DECLINING
    }
}
