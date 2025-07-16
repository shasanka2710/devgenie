package com.org.devgenie.mongo;

import com.org.devgenie.model.coverage.RepositoryAnalysisResponse;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RepositoryAnalysisMongoRepository extends MongoRepository<RepositoryAnalysisResponse, String> {
    RepositoryAnalysisResponse findByRepositoryUrlAndBranch(String repositoryUrl, String branch);
}
