package com.org.devgenie.model.coverage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoverageData {
    @Id
    private String id;
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
    private List<FileCoverageData> files;
    private List<DirectoryCoverageData> directories; // NEW: Directory-level coverage
    private LocalDateTime timestamp;
    private ProjectConfiguration projectConfiguration;
    private CoverageSource coverageSource; // NEW: Track data source

    // NEW: Enum to track coverage data source
    public enum CoverageSource {
        JACOCO_ANALYSIS,    // From Jacoco execution
        SONARQUBE,          // From SonarQube API
        ESTIMATED,          // Estimated based on test generation
        BASIC_ANALYSIS,     // Basic file analysis (0% coverage)
        MONGODB_CACHE       // Retrieved from MongoDB cache
    }
}
