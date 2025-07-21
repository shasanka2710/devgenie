package com.org.devgenie.service.coverage;

import com.org.devgenie.model.coverage.CoverageNode;
import com.org.devgenie.model.coverage.DashboardCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CoverageDataMigrationService {

    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * Migrate existing nested tree structure to flat optimized structure
     */
    public void migrateToFlatStructure(String repoPath, String branch) {
        log.info("Starting migration to flat structure for repo: {} branch: {}", repoPath, branch);
        
        try {
            // 1. Load existing dashboard cache
            Query query = new Query(Criteria.where("repoPath").is(repoPath).and("branch").is(branch));
            DashboardCache existingCache = mongoTemplate.findOne(query, DashboardCache.class, "dashboard_cache");
            
            if (existingCache == null) {
                log.warn("No existing cache found for repo: {} branch: {}", repoPath, branch);
                return;
            }

            // 2. Convert to flat structure
            List<CoverageNode> flatNodes = convertToFlatNodes(existingCache);
            
            // 3. Save flat nodes (batch insert for performance)
            if (!flatNodes.isEmpty()) {
                mongoTemplate.insert(flatNodes, CoverageNode.class);
                log.info("Migrated {} nodes to flat structure", flatNodes.size());
            }
            
            // 4. Update migration status
            updateMigrationStatus(repoPath, branch, "COMPLETED");
            
        } catch (Exception e) {
            log.error("Migration failed for repo: {} branch: {}", repoPath, branch, e);
            updateMigrationStatus(repoPath, branch, "FAILED");
            throw new RuntimeException("Migration failed", e);
        }
    }

    /**
     * Migrate a specific repository and branch
     */
    public boolean migrateRepository(String repoPath, String branch) {
        try {
            log.info("Starting repository migration for repo: {} branch: {}", repoPath, branch);
            
            // Check if already migrated
            String repoPathBranch = repoPath + ":" + branch;
            long existingCount = mongoTemplate.count(
                new Query(Criteria.where("repoPathBranch").is(repoPathBranch)), 
                CoverageNode.class
            );
            
            if (existingCount > 0) {
                log.info("Repository already migrated: {} (found {} nodes)", repoPath, existingCount);
                return false; // Already migrated
            }
            
            // Perform migration
            migrateToFlatStructure(repoPath, branch);
            
            log.info("Successfully migrated repository: {} branch: {}", repoPath, branch);
            return true; // Migration completed
            
        } catch (Exception e) {
            log.error("Failed to migrate repository: {} branch: {}", repoPath, branch, e);
            return false; // Migration failed
        }
    }

    private List<CoverageNode> convertToFlatNodes(DashboardCache cache) {
        List<CoverageNode> nodes = new ArrayList<>();
        String repoPathBranch = cache.getRepoPath() + ":" + cache.getBranch();
        
        // Convert file details to flat nodes
        if (cache.getFileDetails() != null) {
            cache.getFileDetails().forEach(fileDetail -> {
                CoverageNode node = CoverageNode.builder()
                        .repoPath(cache.getRepoPath())
                        .branch(cache.getBranch())
                        .repoPathBranch(repoPathBranch)
                        .fullPath(fileDetail.getFilePath())
                        .parentPath(extractParentPath(fileDetail.getFilePath()))
                        .name(extractFileName(fileDetail.getFilePath()))
                        .type("FILE")
                        .depth(calculateDepth(fileDetail.getFilePath()))
                        .lineCoverage(fileDetail.getLineCoverage())
                        .branchCoverage(fileDetail.getBranchCoverage())
                        .methodCoverage(fileDetail.getMethodCoverage())
                        .totalLines(fileDetail.getTotalLines())
                        .coveredLines(fileDetail.getCoveredLines())
                        .totalBranches(fileDetail.getTotalBranches())
                        .coveredBranches(fileDetail.getCoveredBranches())
                        .totalMethods(fileDetail.getTotalMethods())
                        .coveredMethods(fileDetail.getCoveredMethods())
                        .packageName(fileDetail.getPackageName())
                        .className(fileDetail.getClassName())
                        .lastUpdated(LocalDateTime.now())
                        .build();
                
                node.setParentPathRepo(node.getParentPath() + ":" + repoPathBranch);
                nodes.add(node);
            });
        }
        
        // Convert tree structure to directory nodes
        if (cache.getFileTreeData() != null) {
            convertTreeToDirectoryNodes(cache.getFileTreeData(), cache, nodes, 0);
        }
        
        return nodes;
    }

    private void convertTreeToDirectoryNodes(DashboardCache.FileTreeData treeNode, 
                                           DashboardCache cache, 
                                           List<CoverageNode> nodes, 
                                           int depth) {
        if ("DIRECTORY".equals(treeNode.getType())) {
            String repoPathBranch = cache.getRepoPath() + ":" + cache.getBranch();
            
            CoverageNode dirNode = CoverageNode.builder()
                    .repoPath(cache.getRepoPath())
                    .branch(cache.getBranch())
                    .repoPathBranch(repoPathBranch)
                    .fullPath(treeNode.getPath())
                    .parentPath(extractParentPath(treeNode.getPath()))
                    .name(treeNode.getName())
                    .type("DIRECTORY")
                    .depth(depth)
                    .childFileCount(countChildFiles(treeNode))
                    .childDirCount(countChildDirectories(treeNode))
                    .aggregatedLineCoverage(treeNode.getLineCoverage())
                    .lastUpdated(LocalDateTime.now())
                    .build();
            
            dirNode.setParentPathRepo(dirNode.getParentPath() + ":" + repoPathBranch);
            nodes.add(dirNode);
            
            // Recursively process children
            if (treeNode.getChildren() != null) {
                treeNode.getChildren().forEach(child -> 
                    convertTreeToDirectoryNodes(child, cache, nodes, depth + 1));
            }
        }
    }

    private String extractParentPath(String fullPath) {
        if (fullPath == null || !fullPath.contains("/")) {
            return "";
        }
        return fullPath.substring(0, fullPath.lastIndexOf("/"));
    }

    private String extractFileName(String fullPath) {
        if (fullPath == null || !fullPath.contains("/")) {
            return fullPath;
        }
        return fullPath.substring(fullPath.lastIndexOf("/") + 1);
    }

    private int calculateDepth(String path) {
        if (path == null || path.isEmpty()) {
            return 0;
        }
        return (int) path.chars().filter(ch -> ch == '/').count();
    }

    private int countChildFiles(DashboardCache.FileTreeData treeNode) {
        if (treeNode.getChildren() == null) {
            return 0;
        }
        return (int) treeNode.getChildren().stream()
                .filter(child -> "FILE".equals(child.getType()))
                .count();
    }

    private int countChildDirectories(DashboardCache.FileTreeData treeNode) {
        if (treeNode.getChildren() == null) {
            return 0;
        }
        return (int) treeNode.getChildren().stream()
                .filter(child -> "DIRECTORY".equals(child.getType()))
                .count();
    }

    private void updateMigrationStatus(String repoPath, String branch, String status) {
        // Update migration tracking collection
        // Implementation details...
    }
}
