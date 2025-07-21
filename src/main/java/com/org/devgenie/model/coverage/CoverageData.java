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
public class CoverageData {
    private String branch;
    // Type: FILE or DIRECTORY
    private String type;

    // Common fields
    private String repoPath;
    private String path; // Full path (file or directory)
    private String parentPath;
    private String name; // fileName or directoryName
    private LocalDateTime timestamp;
    private ProjectConfiguration projectConfiguration;
    private CoverageSource coverageSource;

    // Coverage metrics
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

    // File-specific fields
    private String fileName;
    private String className;
    private String packageName;
    // Add more file metadata as needed

    // Directory-specific fields
    private String directoryName;
    private List<String> children; // List of child paths (files or directories)

    // For backward compatibility, you may keep these but mark as deprecated
    // private DirectoryCoverageData rootDirectory;

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
