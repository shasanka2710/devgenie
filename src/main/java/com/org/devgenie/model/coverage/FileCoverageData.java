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
public class FileCoverageData {
    @Id
    private String id;
    private String filePath;
    private double lineCoverage;
    private double branchCoverage;
    private double methodCoverage;
    private int totalLines;
    private int coveredLines;
    private int totalBranches;
    private int coveredBranches;
    private int totalMethods;
    private int coveredMethods;
    private List<String> uncoveredLines;
    private List<String> uncoveredBranches;
    private LocalDateTime lastUpdated;
    private String buildTool; // NEW: Track which build tool generated this data
    private String testFramework; // NEW: Track test framework used
}
