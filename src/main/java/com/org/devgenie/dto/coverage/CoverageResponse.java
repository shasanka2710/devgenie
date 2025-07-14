package com.org.devgenie.dto.coverage;

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
