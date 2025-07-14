package com.org.devgenie.dto.coverage;

import com.org.devgenie.model.coverage.CoverageSummary;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoverageResponse {
    private boolean success;
    private String message;
    private CoverageSummary summary;
    private String error;

    public static CoverageResponse success(CoverageSummary summary) {
        return CoverageResponse.builder()
                .success(true)
                .summary(summary)
                .build();
    }

    public static CoverageResponse error(String error) {
        return CoverageResponse.builder()
                .success(false)
                .error(error)
                .build();
    }
}
