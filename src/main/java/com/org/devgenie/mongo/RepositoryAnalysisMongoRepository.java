package com.org.devgenie.mongo;

import com.org.devgenie.model.coverage.RepositoryAnalysis;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RepositoryAnalysisMongoRepository extends MongoRepository<RepositoryAnalysis, String> {
    RepositoryAnalysis findByRepositoryUrlAndBranch(String repositoryUrl, String branch);
}
