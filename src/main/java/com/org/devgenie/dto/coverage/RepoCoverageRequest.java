package com.org.devgenie.dto.coverage;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RepoCoverageRequest {
    private String repoPath;
    private Double targetCoverageIncrease;
    private List<String> excludeFiles;
    private boolean forceRefresh;
}
