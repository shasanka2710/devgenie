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
public class FileImprovementOpportunity {
    private String filePath;
    private String fileName;
    private OpportunityType type;
    private String title;
    private String description;
    private String priority; // HIGH, MEDIUM, LOW
    private double estimatedCoverageIncrease;
    private String estimatedEffort;
    private List<String> recommendedActions;
    private List<UncoveredMethod> uncoveredMethods;
    private List<UncoveredBranch> uncoveredBranches;
    private ComplexityMetrics complexityMetrics;

    public enum OpportunityType {
        UNCOVERED_METHODS,
        EDGE_CASES,
        EXCEPTION_HANDLING,
        COMPLEX_LOGIC,
        BUSINESS_CRITICAL,
        LOW_BRANCH_COVERAGE,
        HIGH_RISK_CODE
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UncoveredMethod {
        private String methodName;
        private int lineNumber;
        private String signature;
        private double riskScore;
        private String recommendation;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UncoveredBranch {
        private int lineNumber;
        private String condition;
        private String branchType; // IF, SWITCH, LOOP, etc.
        private String recommendation;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComplexityMetrics {
        private int cyclomaticComplexity;
        private int cognitiveComplexity;
        private double businessCriticality;
        private double riskScore;
    }
}
