package com.org.devgenie.mongo;

import com.org.devgenie.model.coverage.CoverageData;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CoverageDataFlatMongoRepository extends MongoRepository<CoverageData, String> {
    // Custom query methods if needed
    public List<CoverageData> findByRepoPathAndBranch(String repoPath,String branch );
    CoverageData findByPath(String path);
    // Add more queries as needed
}
