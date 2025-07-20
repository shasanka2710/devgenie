package com.org.devgenie.model;

import com.org.devgenie.model.coverage.CoverageData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SonarQubeMetricsResponse {
    private SonarBaseComponentMetrics sonarBaseComponentMetrics;
    private List<CoverageData> coverageDataList;
}
