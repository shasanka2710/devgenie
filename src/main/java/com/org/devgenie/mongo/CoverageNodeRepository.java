package com.org.devgenie.mongo;

import com.org.devgenie.model.coverage.CoverageNode;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CoverageNodeRepository extends MongoRepository<CoverageNode, String> {
    
    // Basic queries optimized with indexes
    List<CoverageNode> findByRepoPathAndBranch(String repoPath, String branch);
    
    List<CoverageNode> findByRepoPathBranchAndType(String repoPathBranch, String type);
    
    List<CoverageNode> findByParentPathRepoAndType(String parentPathRepo, String type);
    
    // Tree navigation queries - highly optimized
    @Query("{ 'parentPathRepo': ?0, 'type': ?1 }")
    List<CoverageNode> findChildrenByParentPathAndType(String parentPathRepo, String type);
    
    @Query("{ 'repoPathBranch': ?0, 'depth': ?1, 'type': ?2 }")
    List<CoverageNode> findByDepthAndType(String repoPathBranch, int depth, String type);
    
    // File-specific queries
    @Query("{ 'repoPathBranch': ?0, 'type': 'FILE' }")
    List<CoverageNode> findAllFiles(String repoPathBranch);
    
    @Query("{ 'repoPathBranch': ?0, 'type': 'FILE', 'lineCoverage': { $lt: ?1 } }")
    List<CoverageNode> findFilesWithLowCoverage(String repoPathBranch, double threshold);
    
    @Query("{ 'repoPathBranch': ?0, 'type': 'FILE', 'lineCoverage': { $gte: ?1 } }")
    List<CoverageNode> findFilesWithHighCoverage(String repoPathBranch, double threshold);
    
    // Directory-specific queries
    @Query("{ 'repoPathBranch': ?0, 'type': 'DIRECTORY' }")
    List<CoverageNode> findAllDirectories(String repoPathBranch);
    
    @Query("{ 'repoPathBranch': ?0, 'type': 'DIRECTORY', 'depth': ?1 }")
    List<CoverageNode> findDirectoriesByDepth(String repoPathBranch, int depth);
    
    // Root directory query
    @Query("{ 'repoPathBranch': ?0, 'type': 'DIRECTORY', $or: [{'parentPath': null}, {'parentPath': ''}] }")
    Optional<CoverageNode> findRootDirectory(String repoPathBranch);
    
    // Performance queries for dashboard
    @Query(value = "{ 'repoPathBranch': ?0, 'type': 'FILE' }", 
           fields = "{ 'fullPath': 1, 'lineCoverage': 1, 'branchCoverage': 1, 'methodCoverage': 1, 'totalLines': 1, 'coveredLines': 1 }")
    List<CoverageNode> findFilesSummaryForDashboard(String repoPathBranch);
    
    // Aggregation queries for statistics
    @Aggregation(pipeline = {
        "{ $match: { 'repoPathBranch': ?0, 'type': 'FILE' } }",
        "{ $group: { " +
        "    '_id': null, " +
        "    'totalFiles': { $sum: 1 }, " +
        "    'avgLineCoverage': { $avg: '$lineCoverage' }, " +
        "    'avgBranchCoverage': { $avg: '$branchCoverage' }, " +
        "    'avgMethodCoverage': { $avg: '$methodCoverage' }, " +
        "    'totalLines': { $sum: '$totalLines' }, " +
        "    'totalCoveredLines': { $sum: '$coveredLines' } " +
        "} }"
    })
    Optional<CoverageStatistics> calculateOverallStatistics(String repoPathBranch);
    
    // Search queries
    @Query("{ 'repoPathBranch': ?0, 'name': { $regex: ?1, $options: 'i' } }")
    List<CoverageNode> searchByFileName(String repoPathBranch, String namePattern);
    
    @Query("{ 'repoPathBranch': ?0, 'fullPath': { $regex: ?1, $options: 'i' } }")
    List<CoverageNode> searchByPath(String repoPathBranch, String pathPattern);
    
    // Package-based queries
    @Query("{ 'repoPathBranch': ?0, 'type': 'FILE', 'packageName': ?1 }")
    List<CoverageNode> findFilesByPackage(String repoPathBranch, String packageName);
    
    @Query("{ 'repoPathBranch': ?0, 'type': 'FILE', 'packageName': { $regex: ?1 } }")
    List<CoverageNode> findFilesByPackagePattern(String repoPathBranch, String packagePattern);
    
    // Test file queries
    @Query("{ 'repoPathBranch': ?0, 'type': 'FILE', 'fullPath': { $regex: '/test/' } }")
    List<CoverageNode> findTestFiles(String repoPathBranch);
    
    @Query("{ 'repoPathBranch': ?0, 'type': 'FILE', 'fullPath': { $not: { $regex: '/test/' } } }")
    List<CoverageNode> findSourceFiles(String repoPathBranch);
    
    // Cleanup queries
    void deleteByRepoPathAndBranch(String repoPath, String branch);
    
    void deleteByRepoPathBranch(String repoPathBranch);
    
    // Count queries for pagination
    long countByRepoPathBranchAndType(String repoPathBranch, String type);
    
    long countByParentPathRepoAndType(String parentPathRepo, String type);
    
    /**
     * Count total nodes for a repository and branch
     */
    @Query("SELECT COUNT(*) FROM #{#entityName} c WHERE c.repoPathBranch = ?1")
    long countByRepoPathBranch(String repoPathBranch);

    /**
     * Count file nodes for a repository and branch
     */
    @Query("SELECT COUNT(*) FROM #{#entityName} c WHERE c.repoPathBranch = ?1 AND c.type = 'FILE'")
    long countFilesByRepoPathBranch(String repoPathBranch);
    
    // Interface for aggregation results
    interface CoverageStatistics {
        int getTotalFiles();
        double getAvgLineCoverage();
        double getAvgBranchCoverage();
        double getAvgMethodCoverage();
        int getTotalLines();
        int getTotalCoveredLines();
    }
}
