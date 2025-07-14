package com.org.devgenie.model.coverage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoverageTrend {
    private TrendDirection trend;
    private double overallCoverageChange;
    private double lineCoverageChange;
    private double branchCoverageChange;
    private double methodCoverageChange;
    private String message;
    private LocalDateTime analyzedAt;

    public enum TrendDirection {
        IMPROVING, DECLINING, STABLE, BASELINE
    }
}
