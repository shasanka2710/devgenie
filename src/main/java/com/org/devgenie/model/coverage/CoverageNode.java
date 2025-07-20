package com.org.devgenie.model.coverage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document (collection = "coverage_nodes")
public class CoverageNode {
    @Id
    private String id;
    private String repositoryUrl;
    private String branch;
    private String path;
    private String fileName;
    private boolean isDirectory;
    private String fileType; // e.g., "file", "dir", etc.
    private String lineCoverage;
    private String branchCoverage;
    private double coverage;
    private String coverageStatus; // e.g., "covered", "uncovered", "partially covered"
    private int linesTotal;
    private int linesCovered;
    private String parentPath;

}
