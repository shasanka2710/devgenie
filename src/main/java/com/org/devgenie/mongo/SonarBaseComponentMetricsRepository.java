package com.org.devgenie.mongo;

import com.org.devgenie.model.SonarBaseComponentMetrics;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SonarBaseComponentMetricsRepository extends MongoRepository<SonarBaseComponentMetrics,String> {
    public SonarBaseComponentMetrics findByRepositoryUrlAndBranch(String repositoryUrl, String branch);
}
