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
public class CoverageRecommendation {
    private String priority; // LOW, MEDIUM, HIGH, CRITICAL
    private String category; // STRATEGY, FRAMEWORK, COVERAGE, SETUP, TECHNICAL_DEBT, BUSINESS_RISK
    private String title;
    private String description;
    private String estimatedImpact;
    private String estimatedEffort;
    
    // Enhanced AI-driven recommendations
    private String rationale; // Why this recommendation is important
    private List<String> actionSteps; // Specific steps to implement
    private List<String> expectedOutcomes; // What will be achieved
    private String timeframe; // When this should be completed
    private List<String> prerequisites; // What needs to be done first
    private String successMetrics; // How to measure success
    private String riskIfIgnored; // Consequences of not following this recommendation
    private List<String> relatedFiles; // Specific files this recommendation affects
}
