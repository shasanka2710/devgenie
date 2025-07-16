package com.org.devgenie.mongo;

import com.org.devgenie.exception.coverage.CoverageDataNotFoundException;
import com.org.devgenie.model.coverage.RepositoryAnalysisResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RepositoryAnalysisMongoUtil {
    @Autowired
    private RepositoryAnalysisMongoRepository analysisMongoRepository;

    @Async
    public void persistAnalysisAsync(RepositoryAnalysisResponse response) {
        try {
            if (response != null && response.getRepositoryUrl() != null && response.getBranch() != null) {
                analysisMongoRepository.save(response);
                log.info("Repository analysis persisted to Mongo for {}:{}", response.getRepositoryUrl(), response.getBranch());
            }
        } catch (Exception e) {
            log.error("Failed to persist analysis to Mongo", e);
        }
    }

    public RepositoryAnalysisResponse getAnalysisFromMongo(String repositoryUrl, String branch) {
        try {
            RepositoryAnalysisResponse response = analysisMongoRepository.findByRepositoryUrlAndBranch(repositoryUrl, branch);
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
}
