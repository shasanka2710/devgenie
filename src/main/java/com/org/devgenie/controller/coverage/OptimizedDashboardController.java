package com.org.devgenie.controller.coverage;

import com.org.devgenie.service.coverage.OptimizedCoverageService;
import com.org.devgenie.service.coverage.CoverageDataMigrationService;
import com.org.devgenie.model.coverage.CoverageNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

/**
 * Optimized Dashboard Controller using flat CoverageNode structure
 * for enterprise-scale performance with large repositories
 */
@RestController
@RequestMapping("/api/v2/dashboard")
@Slf4j
public class OptimizedDashboardController {

    @Autowired
    private OptimizedCoverageService optimizedCoverageService;

    @Autowired
    private CoverageDataMigrationService migrationService;

    /**
     * Ultra-fast dashboard data retrieval using optimized flat structure
     */
    @GetMapping("/coverage")
    public ResponseEntity<?> getOptimizedCoverageData(
            @RequestParam String repoPath,
            @RequestParam(defaultValue = "main") String branch) {
        try {
            log.info("Loading optimized dashboard data for repo: {} branch: {}", repoPath, branch);

            // Check if migration is needed
            if (!optimizedCoverageService.isDataMigrated(repoPath, branch)) {
                log.info("Data not migrated yet for repo: {} - triggering migration", repoPath);
                migrationService.migrateRepository(repoPath, branch);
            }

            var dashboardData = optimizedCoverageService.getFastDashboardData(repoPath, branch);
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "data", dashboardData,
                "repoPath", repoPath,
                "branch", branch,
                "optimized", true
            ));
        } catch (Exception e) {
            log.error("Error fetching optimized dashboard data for repo: {}", repoPath, e);
            return ResponseEntity.status(500).body(Map.of(
                "status", "error",
                "message", "Error fetching dashboard data: " + e.getMessage(),
                "repoPath", repoPath,
                "branch", branch
            ));
        }
    }

    /**
     * Get file tree structure with optimized performance
     */
    @GetMapping("/tree")
    public ResponseEntity<?> getFileTree(
            @RequestParam String repoPath,
            @RequestParam(defaultValue = "main") String branch,
            @RequestParam(required = false) String parentPath,
            @RequestParam(defaultValue = "2") int maxDepth) {
        try {
            log.info("Loading file tree for repo: {} branch: {} parentPath: {}", 
                    repoPath, branch, parentPath);

            var fileTree = optimizedCoverageService.getFileTreeStructure(
                repoPath, branch, parentPath, maxDepth);
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "tree", fileTree,
                "parentPath", parentPath != null ? parentPath : "",
                "maxDepth", maxDepth
            ));
        } catch (Exception e) {
            log.error("Error fetching file tree for repo: {}", repoPath, e);
            return ResponseEntity.status(500).body(Map.of(
                "status", "error",
                "message", "Error fetching file tree: " + e.getMessage()
            ));
        }
    }

    /**
     * Get detailed coverage information for a specific file
     */
    @GetMapping("/file")
    public ResponseEntity<?> getFileDetails(
            @RequestParam String repoPath,
            @RequestParam(defaultValue = "main") String branch,
            @RequestParam String filePath) {
        try {
            log.info("Loading file details for repo: {} branch: {} file: {}", 
                    repoPath, branch, filePath);

            var fileDetails = optimizedCoverageService.getFileDetailsAsMap(repoPath, branch, filePath);
            
            if (fileDetails == null) {
                return ResponseEntity.status(404).body(Map.of(
                    "status", "error",
                    "message", "File not found: " + filePath
                ));
            }
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "file", fileDetails,
                "filePath", filePath
            ));
        } catch (Exception e) {
            log.error("Error fetching file details for repo: {} file: {}", repoPath, filePath, e);
            return ResponseEntity.status(500).body(Map.of(
                "status", "error",
                "message", "Error fetching file details: " + e.getMessage()
            ));
        }
    }

    /**
     * Search for files and directories by name or path
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchFiles(
            @RequestParam String repoPath,
            @RequestParam(defaultValue = "main") String branch,
            @RequestParam String query,
            @RequestParam(defaultValue = "20") int limit) {
        try {
            log.info("Searching files for repo: {} branch: {} query: '{}'", 
                    repoPath, branch, query);

            var searchResults = optimizedCoverageService.searchFiles(repoPath, branch, query, limit);
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "results", searchResults,
                "query", query,
                "count", searchResults.size()
            ));
        } catch (Exception e) {
            log.error("Error searching files for repo: {} query: {}", repoPath, query, e);
            return ResponseEntity.status(500).body(Map.of(
                "status", "error",
                "message", "Error searching files: " + e.getMessage()
            ));
        }
    }

    /**
     * Get directory statistics and summary
     */
    @GetMapping("/directory")
    public ResponseEntity<?> getDirectoryStats(
            @RequestParam String repoPath,
            @RequestParam(defaultValue = "main") String branch,
            @RequestParam String directoryPath) {
        try {
            log.info("Loading directory stats for repo: {} branch: {} directory: {}", 
                    repoPath, branch, directoryPath);

            var directoryStats = optimizedCoverageService.getDirectoryStatistics(
                repoPath, branch, directoryPath);
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "stats", directoryStats,
                "directoryPath", directoryPath
            ));
        } catch (Exception e) {
            log.error("Error fetching directory stats for repo: {} directory: {}", 
                    repoPath, directoryPath, e);
            return ResponseEntity.status(500).body(Map.of(
                "status", "error",
                "message", "Error fetching directory stats: " + e.getMessage()
            ));
        }
    }

    /**
     * Force migration from old nested structure to new flat structure
     */
    @PostMapping("/migrate")
    public ResponseEntity<?> migrateCoverageData(
            @RequestParam String repoPath,
            @RequestParam(defaultValue = "main") String branch) {
        try {
            log.info("Starting migration for repo: {} branch: {}", repoPath, branch);

            boolean migrated = migrationService.migrateRepository(repoPath, branch);
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "migrated", migrated,
                "message", migrated ? "Migration completed successfully" : "No data to migrate",
                "repoPath", repoPath,
                "branch", branch
            ));
        } catch (Exception e) {
            log.error("Error migrating coverage data for repo: {}", repoPath, e);
            return ResponseEntity.status(500).body(Map.of(
                "status", "error",
                "message", "Error migrating data: " + e.getMessage()
            ));
        }
    }

    /**
     * Check migration status and data availability
     */
    @GetMapping("/migration/status")
    public ResponseEntity<?> getMigrationStatus(
            @RequestParam String repoPath,
            @RequestParam(defaultValue = "main") String branch) {
        try {
            boolean isMigrated = optimizedCoverageService.isDataMigrated(repoPath, branch);
            long nodeCount = optimizedCoverageService.getCoverageNodeCount(repoPath, branch);
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "migrated", isMigrated,
                "nodeCount", nodeCount,
                "repoPath", repoPath,
                "branch", branch
            ));
        } catch (Exception e) {
            log.error("Error checking migration status for repo: {}", repoPath, e);
            return ResponseEntity.status(500).body(Map.of(
                "status", "error",
                "message", "Error checking migration status: " + e.getMessage()
            ));
        }
    }

    /**
     * Get performance metrics and cache information
     */
    @GetMapping("/performance")
    public ResponseEntity<?> getPerformanceMetrics(
            @RequestParam String repoPath,
            @RequestParam(defaultValue = "main") String branch) {
        try {
            var metrics = optimizedCoverageService.getPerformanceMetrics(repoPath, branch);
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "metrics", metrics,
                "repoPath", repoPath,
                "branch", branch
            ));
        } catch (Exception e) {
            log.error("Error fetching performance metrics for repo: {}", repoPath, e);
            return ResponseEntity.status(500).body(Map.of(
                "status", "error",
                "message", "Error fetching performance metrics: " + e.getMessage()
            ));
        }
    }

    /**
     * Clear optimized coverage cache for a repository
     */
    @DeleteMapping("/cache/clear")
    public ResponseEntity<?> clearOptimizedCache(
            @RequestParam String repoPath,
            @RequestParam(defaultValue = "main") String branch) {
        try {
            log.info("Clearing optimized cache for repo: {} branch: {}", repoPath, branch);
            
            optimizedCoverageService.clearCache(repoPath, branch);
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Optimized cache cleared successfully",
                "repoPath", repoPath,
                "branch", branch
            ));
        } catch (Exception e) {
            log.error("Error clearing optimized cache for repo: {}", repoPath, e);
            return ResponseEntity.status(500).body(Map.of(
                "status", "error",
                "message", "Error clearing cache: " + e.getMessage()
            ));
        }
    }

    /**
     * Health check for optimized dashboard API
     */
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of(
            "status", "healthy",
            "service", "OptimizedDashboardController",
            "version", "v2",
            "features", List.of(
                "flat_coverage_nodes",
                "optimized_indexing", 
                "fast_tree_navigation",
                "enterprise_scalability"
            )
        ));
    }
}
