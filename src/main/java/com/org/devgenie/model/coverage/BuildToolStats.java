package com.org.devgenie.model.coverage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuildToolStats {
    private int repositoryCount;
    private double averageCoverage;
    private double maxCoverage;
    private double minCoverage;
}
