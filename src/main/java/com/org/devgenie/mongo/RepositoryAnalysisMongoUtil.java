package com.org.devgenie.mongo;

import com.org.devgenie.exception.coverage.CoverageDataNotFoundException;
import com.org.devgenie.model.SonarBaseComponentMetrics;
import com.org.devgenie.model.coverage.CoverageData;
import com.org.devgenie.model.coverage.RepositoryAnalysis;
import com.org.devgenie.model.coverage.RepositoryAnalysisResponse;
import com.org.devgenie.service.metadata.MetadataAnalyzer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class RepositoryAnalysisMongoUtil {
    @Autowired
    private RepositoryAnalysisMongoRepository analysisMongoRepository;
    @Autowired
    private CoverageDataFlatMongoRepository coverageDataFlatMongoRepository;
    @Autowired
    private FileMetadataMongoRepository fileMetadataMongoRepository;
    @Autowired
    private SonarBaseComponentMetricsRepository sonarBaseComponentMetricsRepository;

    /**
     * Persist repository summary only (no embedded coverage tree)
     */
    @Async
    public void persistRepositoryAnalysisAsync(RepositoryAnalysis response) {
        try {
            if (response != null && response.getRepositoryUrl() != null && response.getBranch() != null) {
                analysisMongoRepository.save(response);
                log.info("Repository summary persisted to Mongo for {}:{}", response.getRepositoryUrl(), response.getBranch());
            }
        } catch (Exception e) {
            log.error("Failed to persist repository summary to Mongo", e);
        }
    }

    /**
     * Persist a single coverage node (directory or file) as CoverageData
     */
    @Async
    public void persistCoverageDataBatchAsync(List<CoverageData> coverageDataList, String repoDir, String branch) {
        if(coverageDataList!= null && !coverageDataList.isEmpty()) {
            coverageDataFlatMongoRepository.saveAll(coverageDataList);
            log.info("Coverage data batch persisted for repo {} branch {}: {} nodes", repoDir, branch, coverageDataList.size());
        }
    }


    /**
     * Persist file metadata in batch
     */
    @Async
    public void persistFileMetadataBatchAsync(java.util.List<MetadataAnalyzer.FileMetadata> fileMetadata, String repoDir, String branch) {
        if (fileMetadata != null && !fileMetadata.isEmpty()) {
            fileMetadataMongoRepository.saveAll((java.util.List<MetadataAnalyzer.FileMetadata>) fileMetadata);
            log.info("File metadata batch persisted for repo {} branch {}: {} files", repoDir, branch, fileMetadata.size());
        }
    }

    public RepositoryAnalysis getAnalysisFromMongo(String repositoryUrl, String branch) {
        try {
            RepositoryAnalysis response = analysisMongoRepository.findByRepositoryUrlAndBranch(repositoryUrl, branch);
            if (response == null) {
                throw new CoverageDataNotFoundException("No analysis found for " + repositoryUrl + " on branch " + branch);
            }
            return response;
        } catch (CoverageDataNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error fetching analysis from Mongo", e);
            return null;
        }
    }

    public List<MetadataAnalyzer.FileMetadata> getFileMetadataFromMongo(String repositoryUrl, String branch) {
        try {
            return fileMetadataMongoRepository.findByRepositoryUrlAndBranch(repositoryUrl, branch);
        } catch (Exception e) {
            log.error("Error fetching file metadata from Mongo", e);
            return List.of(); // Return empty list on error
        }
    }

    public List<CoverageData> getCoverageDataFromMongo(String repositoryUrl, String branch) {
        try {
            return coverageDataFlatMongoRepository.findByRepoPathAndBranch(repositoryUrl, branch);
        } catch (Exception e) {
            log.error("Error fetching coverage data from Mongo", e);
            return List.of(); // Return empty list on error
        }
    }

    public RepositoryAnalysisResponse getRepositoryAnalysisResponse(String repositoryUrl, String branch) {
        try {
            RepositoryAnalysis analysis = getAnalysisFromMongo(repositoryUrl, branch);
            List<MetadataAnalyzer.FileMetadata> fileMetadata = getFileMetadataFromMongo(repositoryUrl, branch);
            List<CoverageData> existingCoverage = getCoverageDataFromMongo(repositoryUrl, branch);
            SonarBaseComponentMetrics sonarMetrics = getSonarBaseComponentMetrics(repositoryUrl, branch);

            return RepositoryAnalysisResponse.builder()
                    .repositoryAnalysis(analysis)
                    .fileMetadata(fileMetadata)
                    .existingCoverage(existingCoverage)
                    .sonarBaseComponentMetrics(sonarMetrics)
                    .success(true)
                    .build();
        } catch (Exception e) {
            log.error("Error fetching repository analysis response from Mongo", e);
            return null; // Return null on error
        }
    }

    @Async
    public void persistSonarBaseComponentMetricsAsync(String repositoryUrl, String branch, SonarBaseComponentMetrics metrics) {
        try {
            if (metrics != null && repositoryUrl != null && branch != null) {
                sonarBaseComponentMetricsRepository.save(metrics);
                log.info("Sonar base component metrics persisted for {}:{}", repositoryUrl, branch);
            }
        } catch (Exception e) {
            log.error("Failed to persist Sonar base component metrics to Mongo", e);
        }
    }

    public SonarBaseComponentMetrics getSonarBaseComponentMetrics(String repositoryUrl, String branch) {
        try {
            return sonarBaseComponentMetricsRepository.findByRepositoryUrlAndBranch(repositoryUrl, branch);
        } catch (Exception e) {
            log.error("Error fetching Sonar base component metrics from Mongo", e);
            return null; // Return null on error
        }
    }
}
