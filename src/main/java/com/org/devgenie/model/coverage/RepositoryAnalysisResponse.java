package com.org.devgenie.model.coverage;

import com.org.devgenie.model.SonarBaseComponentMetrics;
import com.org.devgenie.service.metadata.MetadataAnalyzer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RepositoryAnalysisResponse {
    private boolean success;
    private String error;
    private RepositoryAnalysis repositoryAnalysis;
    private SonarBaseComponentMetrics sonarBaseComponentMetrics;
    private List<MetadataAnalyzer.FileMetadata> fileMetadata;
    private List<CoverageData> existingCoverage;

    public static RepositoryAnalysisResponse error(String errorMessage) {
        return RepositoryAnalysisResponse.builder()
                .success(false)
                .error(errorMessage)
                .build();
    }
}
