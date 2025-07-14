package com.org.devgenie.model.coverage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileSample {
    private String filePath;
    private double lineCoverage;
    private double branchCoverage;
    private int fileSize;
    private String contentPreview;
}
