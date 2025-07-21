package com.org.devgenie.mongo;

import com.org.devgenie.model.coverage.DashboardCache;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DashboardCacheRepository extends MongoRepository<DashboardCache, String> {
    
    Optional<DashboardCache> findByRepoPathAndBranch(String repoPath, String branch);
    
    void deleteByRepoPathAndBranch(String repoPath, String branch);
    
    boolean existsByRepoPathAndBranch(String repoPath, String branch);
}
