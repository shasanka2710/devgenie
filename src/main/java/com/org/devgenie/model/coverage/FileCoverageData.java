package com.org.devgenie.model.coverage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "file_coverage")
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
}
