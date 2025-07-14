package com.org.devgenie.model.coverage;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnhancedFileCoverageRequest {
    private String repositoryUrl;
    private String branch = "main";
    private String filePath;
    private Double targetCoverageIncrease;
    private String githubToken;
    private String workspaceId;
}
