package com.org.devgenie.model.coverage;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
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
    private String buildTool; // NEW: Track which build tool generated this data
    private String testFramework; // NEW: Track test framework used
}
