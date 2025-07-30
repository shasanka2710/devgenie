package com.org.devgenie.mongo;

import com.org.devgenie.model.coverage.RepositoryAnalysis;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RepositoryAnalysisMongoRepository extends MongoRepository<RepositoryAnalysis, String> {
    RepositoryAnalysis findByRepositoryUrlAndBranch(String repositoryUrl, String branch);
    List<RepositoryAnalysis> findTopByOrderByAnalysisTimestampDesc(Pageable pageable);
}
