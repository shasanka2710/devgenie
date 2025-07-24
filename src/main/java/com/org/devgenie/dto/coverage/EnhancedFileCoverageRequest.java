package com.org.devgenie.dto.coverage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnhancedFileCoverageRequest {
    private String repositoryUrl;
    @Builder.Default
    private String branch = "main";
    private String filePath;
    private Double targetCoverageIncrease; // e.g., 25.0 for +25%
    private String githubToken; // optional for private repos
    private String workspaceId; // for session management
    
    // Batch processing controls
    @Builder.Default
    private Integer maxTestsPerBatch = 5; // Token limitation handling
    @Builder.Default
    private Integer maxRetries = 3;
    @Builder.Default
    private Boolean validateTests = true;
    
    // Advanced options
    private List<String> testFrameworks; // ["junit5", "mockito", "spring-boot-test"]
    private Map<String, Object> additionalOptions;
    @Builder.Default
    private Boolean createPullRequest = false;
    private String prTitle;
    private String prDescription;
}
