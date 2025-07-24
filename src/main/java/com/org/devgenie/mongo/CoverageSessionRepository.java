package com.org.devgenie.mongo;

import com.org.devgenie.model.coverage.CoverageImprovementSession;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CoverageSessionRepository extends MongoRepository<CoverageImprovementSession, String> {
    
    List<CoverageImprovementSession> findByRepositoryUrlAndBranch(String repositoryUrl, String branch);
    
    List<CoverageImprovementSession> findByRepositoryUrlAndBranchAndFilePath(String repositoryUrl, String branch, String filePath);
    
    List<CoverageImprovementSession> findByStatus(CoverageImprovementSession.SessionStatus status);
    
    List<CoverageImprovementSession> findByType(CoverageImprovementSession.SessionType type);
    
    Optional<CoverageImprovementSession> findByWorkspaceId(String workspaceId);
    
    void deleteByRepositoryUrlAndBranch(String repositoryUrl, String branch);
}
