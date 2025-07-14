package com.org.devgenie.dto.coverage;


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
