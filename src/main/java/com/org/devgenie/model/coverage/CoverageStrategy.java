package com.org.devgenie.model.coverage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoverageStrategy {
    private String recommendedApproach;
    private List<String> priorityAreas;
    private String estimatedTimeToImprove;
}
