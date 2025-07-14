package com.org.devgenie.model.coverage;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnhancedRepoCoverageRequest {
    private String repositoryUrl;
    private String branch = "main";
    private Double targetCoverageIncrease;
    private String githubToken;
    private String workspaceId;
    private List<String> excludePatterns;
    private Integer maxFilesToProcess;
    private boolean forceRefresh = false;
}
