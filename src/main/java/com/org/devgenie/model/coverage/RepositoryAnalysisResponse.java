package com.org.devgenie.model.coverage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RepositoryAnalysisResponse {
    private boolean success;
    private String error;
    private String repositoryUrl;
    private String branch;
    private String workspaceId;
    private ProjectConfiguration projectConfiguration;
    private int totalJavaFiles;
    private List<String> javaFiles;
    private CoverageData existingCoverage;
    private RepositoryInsights insights;
    private List<CoverageRecommendation> recommendations;
    private LocalDateTime analysisTimestamp;

    public static RepositoryAnalysisResponse error(String error) {
        return RepositoryAnalysisResponse.builder()
                .success(false)
                .error(error)
                .build();
    }
}
