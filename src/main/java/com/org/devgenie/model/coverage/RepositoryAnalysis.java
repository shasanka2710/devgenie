package com.org.devgenie.model.coverage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RepositoryAnalysis {
    @Id
    private String id; // Unique identifier for the analysis
    private boolean success;
    private String error;
    private String repositoryUrl;
    private String branch;
    private String workspaceId;
    private ProjectConfiguration projectConfiguration;
    private int totalJavaFiles;
    private SimplifiedRepositoryInsights insights;
    private List<CoverageRecommendation> recommendations;
    private LocalDateTime analysisTimestamp;

    public static RepositoryAnalysis error(String error) {
        return RepositoryAnalysis.builder()
                .success(false)
                .error(error)
                .build();
    }
}
