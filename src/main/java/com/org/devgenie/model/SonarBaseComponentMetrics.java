package com.org.devgenie.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SonarBaseComponentMetrics {
    @Id
    private String id;
    private String repositoryUrl;
    private String branch;
    private double overallCoverage;
    private double lineCoverage;
    private double branchCoverage;
    private int totalLines;
    private int coveredLines;
    private int totalBranches;
    private int coveredBranches;
}
