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
public class FileAnalysisResult {
    private String filePath;
    private String complexity;
    private String businessLogicPriority;
    private List<TestableComponent> testableComponents;
    private List<UncoveredCodePath> uncoveredCodePaths;
    private List<String> dependencies;
    private String estimatedEffort;
    private String coverageImpactPotential;
    private double currentCoverage;
}

