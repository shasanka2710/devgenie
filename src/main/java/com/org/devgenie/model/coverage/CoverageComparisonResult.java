package com.org.devgenie.model.coverage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoverageComparisonResult {
    private CoverageData originalCoverage;
    private CoverageData newCoverage;
    private double coverageImprovement;
    private String validationMethod;
    private LocalDateTime validatedAt;
    private List<String> warnings;
    private boolean significant; // Whether improvement is statistically significant

    public boolean hasImprovement() {
        return coverageImprovement > 0.1; // At least 0.1% improvement
    }

    public boolean isSignificantImprovement() {
        return coverageImprovement > 5.0; // At least 5% improvement
    }
}
