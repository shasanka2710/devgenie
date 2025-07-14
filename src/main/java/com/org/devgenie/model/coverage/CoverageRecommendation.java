package com.org.devgenie.model.coverage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoverageRecommendation {
    private String priority; // LOW, MEDIUM, HIGH
    private String category; // STRATEGY, FRAMEWORK, COVERAGE, SETUP
    private String title;
    private String description;
    private String estimatedImpact;
    private String estimatedEffort;
}
