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
}
