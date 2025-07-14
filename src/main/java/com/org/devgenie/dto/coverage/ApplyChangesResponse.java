package com.org.devgenie.dto.coverage;


import com.org.devgenie.model.coverage.CoverageComparisonResult;
import com.org.devgenie.model.coverage.CoverageData;
import com.org.devgenie.model.coverage.PullRequestResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplyChangesResponse {
    private boolean success;
    private String message;
    private PullRequestResult pullRequest;
    private CoverageData finalCoverage;
    private String error;
    private CoverageComparisonResult coverageComparison; // NEW: Detailed comparison
    private String validationMethod; // NEW: How coverage was validated
    private double coverageImprovement; // NEW: Actual improvement achieved

    public static ApplyChangesResponse success(PullRequestResult pr, CoverageData coverage) {
        return ApplyChangesResponse.builder()
                .success(true)
                .pullRequest(pr)
                .finalCoverage(coverage)
                .build();
    }

    public static ApplyChangesResponse error(String error) {
        return ApplyChangesResponse.builder()
                .success(false)
                .error(error)
                .build();
    }
}
