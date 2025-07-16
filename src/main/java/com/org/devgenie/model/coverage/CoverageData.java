package com.org.devgenie.model.coverage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoverageData {
    // Project-level metrics
    private String repoPath;
    private double overallCoverage;
    private double lineCoverage;
    private double branchCoverage;
    private double methodCoverage;
    private int totalLines;
    private int coveredLines;
    private int totalBranches;
    private int coveredBranches;
    private int totalMethods;
    private int coveredMethods;
    private LocalDateTime timestamp;
    private ProjectConfiguration projectConfiguration;
    private CoverageSource coverageSource;
    // Only one root directory, which contains all nested directories/files
    private DirectoryCoverageData rootDirectory;

    // NEW: Enum to track coverage data source
    public enum CoverageSource {
        JACOCO_ANALYSIS,    // From Jacoco execution
        SONARQUBE,          // From SonarQube API
        ESTIMATED,          // Estimated based on test generation
        BASIC_ANALYSIS,     // Basic file analysis (0% coverage)
        HTML_REPORT,
        MONGODB_CACHE       // Retrieved from MongoDB cache
    }
}
