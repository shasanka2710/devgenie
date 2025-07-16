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
public class DirectoryCoverageData {
    private String directoryPath; // Relative to repo root, e.g., src/main/java/com/org/devgenie/service
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
    private List<FileCoverageData> files; // List of file coverage data directly under this directory
    private List<DirectoryCoverageData> subdirectories; // List of subdirectory DirectoryCoverageData objects
    private LocalDateTime lastUpdated;
}
