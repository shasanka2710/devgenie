package com.org.devgenie.service.coverage;

import com.org.devgenie.model.coverage.CoverageNode;
import com.org.devgenie.mongo.CoverageNodeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OptimizedCoverageService {

    @Autowired
    private CoverageNodeRepository coverageNodeRepository;

    /**
     * Ultra-fast dashboard loading - uses flat structure with indexes
     */
    @Cacheable(value = "dashboard", key = "#repoPath + ':' + #branch")
    public DashboardResponse getFastDashboard(String repoPath, String branch) {
        long startTime = System.nanoTime();
        String repoPathBranch = repoPath + ":" + branch;
        
        try {
            // 1. Get overall statistics (single aggregation query)
            Optional<CoverageNodeRepository.CoverageStatistics> statsOpt = 
                coverageNodeRepository.calculateOverallStatistics(repoPathBranch);
            
            if (statsOpt.isEmpty()) {
                return DashboardResponse.empty();
            }
            
            CoverageNodeRepository.CoverageStatistics stats = statsOpt.get();
            
            // 2. Get root directories (very fast with index)
            List<CoverageNode> rootDirs = coverageNodeRepository.findDirectoriesByDepth(repoPathBranch, 1);
            
            // 3. Get file summary for details panel (projection query for minimal data)
            List<CoverageNode> fileSummaries = coverageNodeRepository.findFilesSummaryForDashboard(repoPathBranch);
            
            long endTime = System.nanoTime();
            log.info("Dashboard loaded in {} ms", (endTime - startTime) / 1_000_000);
            
            return DashboardResponse.builder()
                    .overallMetrics(buildOverallMetrics(stats))
                    .rootDirectories(rootDirs)
                    .fileSummaries(fileSummaries)
                    .totalFiles(stats.getTotalFiles())
                    .loadTimeMs((endTime - startTime) / 1_000_000)
                    .build();
                    
        } catch (Exception e) {
            log.error("Error loading dashboard for repo: {} branch: {}", repoPath, branch, e);
            throw new RuntimeException("Failed to load dashboard", e);
        }
    }

    /**
     * Get children of a directory (instant with parent index)
     */
    @Cacheable(value = "children", key = "#repoPath + ':' + #branch + ':' + #parentPath")
    public List<CoverageNode> getDirectoryChildren(String repoPath, String branch, String parentPath) {
        String parentPathRepo = parentPath + ":" + repoPath + ":" + branch;
        
        // Get both directories and files in a single query
        List<CoverageNode> directories = coverageNodeRepository.findChildrenByParentPathAndType(parentPathRepo, "DIRECTORY");
        List<CoverageNode> files = coverageNodeRepository.findChildrenByParentPathAndType(parentPathRepo, "FILE");
        
        // Combine and sort
        List<CoverageNode> children = new ArrayList<>();
        children.addAll(directories);
        children.addAll(files);
        
        return children.stream()
                .sorted((a, b) -> {
                    // Directories first, then files, both alphabetically
                    if (a.isDirectory() && b.isFile()) return -1;
                    if (a.isFile() && b.isDirectory()) return 1;
                    return a.getName().compareToIgnoreCase(b.getName());
                })
                .collect(Collectors.toList());
    }

    /**
     * Get file details with improvement opportunities
     */
    @Cacheable(value = "fileDetails", key = "#repoPath + ':' + #branch + ':' + #filePath")
    public CoverageNode getFileDetails(String repoPath, String branch, String filePath) {
        String repoPathBranch = repoPath + ":" + branch;
        
        return coverageNodeRepository.findAllFiles(repoPathBranch)
                .stream()
                .filter(node -> filePath.equals(node.getFullPath()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Search files and directories
     */
    public List<CoverageNode> searchNodes(String repoPath, String branch, String searchTerm) {
        String repoPathBranch = repoPath + ":" + branch;
        
        // Search both by name and path
        List<CoverageNode> nameResults = coverageNodeRepository.searchByFileName(repoPathBranch, searchTerm);
        List<CoverageNode> pathResults = coverageNodeRepository.searchByPath(repoPathBranch, searchTerm);
        
        // Combine and deduplicate
        Set<String> seenIds = new HashSet<>();
        List<CoverageNode> results = new ArrayList<>();
        
        nameResults.forEach(node -> {
            if (seenIds.add(node.getId())) {
                results.add(node);
            }
        });
        
        pathResults.forEach(node -> {
            if (seenIds.add(node.getId())) {
                results.add(node);
            }
        });
        
        return results;
    }

    /**
     * Get files that need attention (low coverage)
     */
    @Cacheable(value = "lowCoverage", key = "#repoPath + ':' + #branch + ':' + #threshold")
    public List<CoverageNode> getFilesNeedingAttention(String repoPath, String branch, double threshold) {
        String repoPathBranch = repoPath + ":" + branch;
        return coverageNodeRepository.findFilesWithLowCoverage(repoPathBranch, threshold);
    }

    /**
     * Get package-level coverage summary
     */
    @Cacheable(value = "packageSummary", key = "#repoPath + ':' + #branch")
    public Map<String, PackageCoverage> getPackageCoverageSummary(String repoPath, String branch) {
        String repoPathBranch = repoPath + ":" + branch;
        List<CoverageNode> files = coverageNodeRepository.findAllFiles(repoPathBranch);
        
        return files.stream()
                .filter(node -> node.getPackageName() != null)
                .collect(Collectors.groupingBy(
                    CoverageNode::getPackageName,
                    Collectors.collectingAndThen(
                        Collectors.toList(),
                        this::calculatePackageCoverage
                    )
                ));
    }

    /**
     * Clear cache for a repository
     */
    @CacheEvict(value = {"dashboard", "children", "fileDetails", "lowCoverage", "packageSummary"}, 
                key = "#repoPath + ':' + #branch")
    public void clearRepositoryCache(String repoPath, String branch) {
        log.info("Cleared cache for repo: {} branch: {}", repoPath, branch);
    }

    /**
     * Bulk update coverage data (used during analysis)
     */
    public void bulkUpdateCoverageNodes(List<CoverageNode> nodes) {
        // Clear cache before update
        if (!nodes.isEmpty()) {
            CoverageNode first = nodes.get(0);
            clearRepositoryCache(first.getRepoPath(), first.getBranch());
        }
        
        // Batch save for performance
        coverageNodeRepository.saveAll(nodes);
        log.info("Bulk updated {} coverage nodes", nodes.size());
    }

    /**
     * Check if data has been migrated to the flat structure
     */
    public boolean isDataMigrated(String repoPath, String branch) {
        String repoPathBranch = repoPath + ":" + branch;
        return coverageNodeRepository.countByRepoPathBranch(repoPathBranch) > 0;
    }

    /**
     * Get fast dashboard data compatible with controller interface
     */
    public Map<String, Object> getFastDashboardData(String repoPath, String branch) {
        DashboardResponse response = getFastDashboard(repoPath, branch);
        
        Map<String, Object> result = new HashMap<>();
        result.put("overallMetrics", response.getOverallMetrics());
        result.put("rootDirectories", response.getRootDirectories());
        result.put("fileSummaries", response.getFileSummaries());
        result.put("totalFiles", response.getTotalFiles());
        result.put("loadTimeMs", response.getLoadTimeMs());
        
        return result;
    }

    /**
     * Get file tree structure for tree navigation
     */
    public List<Map<String, Object>> getFileTreeStructure(String repoPath, String branch, String parentPath, int maxDepth) {
        String actualParentPath = parentPath != null ? parentPath : "";
        List<CoverageNode> children = getDirectoryChildren(repoPath, branch, actualParentPath);
        
        return children.stream()
                .map(node -> {
                    Map<String, Object> nodeMap = new HashMap<>();
                    nodeMap.put("name", node.getName());
                    nodeMap.put("type", node.getType());
                    nodeMap.put("path", node.getFullPath());
                    nodeMap.put("lineCoverage", node.getLineCoverage());
                    nodeMap.put("branchCoverage", node.getBranchCoverage());
                    nodeMap.put("methodCoverage", node.getMethodCoverage());
                    
                    if (node.isDirectory() && maxDepth > 1) {
                        // Recursively get children up to maxDepth
                        nodeMap.put("children", getFileTreeStructure(repoPath, branch, node.getFullPath(), maxDepth - 1));
                    }
                    
                    return nodeMap;
                })
                .collect(Collectors.toList());
    }

    /**
     * Get file details as a map for controller response
     */
    public Map<String, Object> getFileDetailsAsMap(String repoPath, String branch, String filePath) {
        CoverageNode node = getFileDetails(repoPath, branch, filePath);
        
        if (node == null) {
            return null;
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("fileName", node.getName());
        result.put("filePath", node.getFullPath());
        result.put("packageName", node.getPackageName());
        result.put("className", node.getClassName());
        result.put("lineCoverage", node.getLineCoverage());
        result.put("branchCoverage", node.getBranchCoverage());
        result.put("methodCoverage", node.getMethodCoverage());
        result.put("totalLines", node.getTotalLines());
        result.put("coveredLines", node.getCoveredLines());
        result.put("totalBranches", node.getTotalBranches());
        result.put("coveredBranches", node.getCoveredBranches());
        result.put("totalMethods", node.getTotalMethods());
        result.put("coveredMethods", node.getCoveredMethods());
        
        return result;
    }

    /**
     * Search files with limit and return as maps
     */
    public List<Map<String, Object>> searchFiles(String repoPath, String branch, String query, int limit) {
        List<CoverageNode> results = searchNodes(repoPath, branch, query);
        
        return results.stream()
                .limit(limit)
                .map(node -> {
                    Map<String, Object> nodeMap = new HashMap<>();
                    nodeMap.put("fileName", node.getName());
                    nodeMap.put("path", node.getFullPath());
                    nodeMap.put("type", node.getType());
                    nodeMap.put("lineCoverage", node.getLineCoverage());
                    nodeMap.put("packageName", node.getPackageName());
                    return nodeMap;
                })
                .collect(Collectors.toList());
    }

    /**
     * Get directory statistics
     */
    public Map<String, Object> getDirectoryStatistics(String repoPath, String branch, String directoryPath) {
        List<CoverageNode> children = getDirectoryChildren(repoPath, branch, directoryPath);
        
        int fileCount = (int) children.stream().filter(CoverageNode::isFile).count();
        int directoryCount = (int) children.stream().filter(CoverageNode::isDirectory).count();
        
        double avgLineCoverage = children.stream()
                .filter(CoverageNode::isFile)
                .mapToDouble(node -> node.getLineCoverage() != null ? node.getLineCoverage() : 0)
                .average()
                .orElse(0.0);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("directoryPath", directoryPath);
        stats.put("fileCount", fileCount);
        stats.put("directoryCount", directoryCount);
        stats.put("totalItems", fileCount + directoryCount);
        stats.put("averageLineCoverage", avgLineCoverage);
        stats.put("children", children);
        
        return stats;
    }

    /**
     * Get count of coverage nodes for a repository
     */
    public long getCoverageNodeCount(String repoPath, String branch) {
        String repoPathBranch = repoPath + ":" + branch;
        return coverageNodeRepository.countByRepoPathBranch(repoPathBranch);
    }

    /**
     * Get performance metrics
     */
    public Map<String, Object> getPerformanceMetrics(String repoPath, String branch) {
        String repoPathBranch = repoPath + ":" + branch;
        
        long totalNodes = coverageNodeRepository.countByRepoPathBranch(repoPathBranch);
        long fileNodes = coverageNodeRepository.countFilesByRepoPathBranch(repoPathBranch);
        long directoryNodes = totalNodes - fileNodes;
        
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("totalNodes", totalNodes);
        metrics.put("fileNodes", fileNodes);
        metrics.put("directoryNodes", directoryNodes);
        metrics.put("indexOptimized", true);
        metrics.put("flatStructure", true);
        
        return metrics;
    }

    /**
     * Clear cache for the optimized service
     */
    public void clearCache(String repoPath, String branch) {
        clearRepositoryCache(repoPath, branch);
    }

    private OverallMetrics buildOverallMetrics(CoverageNodeRepository.CoverageStatistics stats) {
        double overallCoverage = stats.getTotalLines() > 0 ? 
            (double) stats.getTotalCoveredLines() / stats.getTotalLines() * 100 : 0;
        
        return OverallMetrics.builder()
                .overallCoverage(overallCoverage)
                .lineCoverage(stats.getAvgLineCoverage())
                .branchCoverage(stats.getAvgBranchCoverage())
                .methodCoverage(stats.getAvgMethodCoverage())
                .totalLines(stats.getTotalLines())
                .coveredLines(stats.getTotalCoveredLines())
                .totalFiles(stats.getTotalFiles())
                .build();
    }

    private PackageCoverage calculatePackageCoverage(List<CoverageNode> files) {
        int totalLines = files.stream().mapToInt(f -> f.getTotalLines() != null ? f.getTotalLines() : 0).sum();
        int coveredLines = files.stream().mapToInt(f -> f.getCoveredLines() != null ? f.getCoveredLines() : 0).sum();
        double avgCoverage = files.stream().mapToDouble(f -> f.getLineCoverage() != null ? f.getLineCoverage() : 0).average().orElse(0);
        
        return PackageCoverage.builder()
                .fileCount(files.size())
                .totalLines(totalLines)
                .coveredLines(coveredLines)
                .averageCoverage(avgCoverage)
                .lineCoverage(totalLines > 0 ? (double) coveredLines / totalLines * 100 : 0)
                .build();
    }

    // Response DTOs
    public static class DashboardResponse {
        private OverallMetrics overallMetrics;
        private List<CoverageNode> rootDirectories;
        private List<CoverageNode> fileSummaries;
        private int totalFiles;
        private long loadTimeMs;
        
        // Getters, setters, builder
        public static DashboardResponse empty() {
            return new DashboardResponse();
        }
        
        // Builder and other methods...
        public static DashboardResponseBuilder builder() {
            return new DashboardResponseBuilder();
        }
        
        public static class DashboardResponseBuilder {
            private OverallMetrics overallMetrics;
            private List<CoverageNode> rootDirectories;
            private List<CoverageNode> fileSummaries;
            private int totalFiles;
            private long loadTimeMs;
            
            public DashboardResponseBuilder overallMetrics(OverallMetrics overallMetrics) {
                this.overallMetrics = overallMetrics;
                return this;
            }
            
            public DashboardResponseBuilder rootDirectories(List<CoverageNode> rootDirectories) {
                this.rootDirectories = rootDirectories;
                return this;
            }
            
            public DashboardResponseBuilder fileSummaries(List<CoverageNode> fileSummaries) {
                this.fileSummaries = fileSummaries;
                return this;
            }
            
            public DashboardResponseBuilder totalFiles(int totalFiles) {
                this.totalFiles = totalFiles;
                return this;
            }
            
            public DashboardResponseBuilder loadTimeMs(long loadTimeMs) {
                this.loadTimeMs = loadTimeMs;
                return this;
            }
            
            public DashboardResponse build() {
                DashboardResponse response = new DashboardResponse();
                response.overallMetrics = this.overallMetrics;
                response.rootDirectories = this.rootDirectories;
                response.fileSummaries = this.fileSummaries;
                response.totalFiles = this.totalFiles;
                response.loadTimeMs = this.loadTimeMs;
                return response;
            }
        }
        
        // Getters
        public OverallMetrics getOverallMetrics() { return overallMetrics; }
        public List<CoverageNode> getRootDirectories() { return rootDirectories; }
        public List<CoverageNode> getFileSummaries() { return fileSummaries; }
        public int getTotalFiles() { return totalFiles; }
        public long getLoadTimeMs() { return loadTimeMs; }
    }

    public static class OverallMetrics {
        private double overallCoverage;
        private double lineCoverage;
        private double branchCoverage;
        private double methodCoverage;
        private int totalLines;
        private int coveredLines;
        private int totalFiles;
        
        public static OverallMetricsBuilder builder() {
            return new OverallMetricsBuilder();
        }
        
        public static class OverallMetricsBuilder {
            private double overallCoverage;
            private double lineCoverage;
            private double branchCoverage;
            private double methodCoverage;
            private int totalLines;
            private int coveredLines;
            private int totalFiles;
            
            public OverallMetricsBuilder overallCoverage(double overallCoverage) {
                this.overallCoverage = overallCoverage;
                return this;
            }
            
            public OverallMetricsBuilder lineCoverage(double lineCoverage) {
                this.lineCoverage = lineCoverage;
                return this;
            }
            
            public OverallMetricsBuilder branchCoverage(double branchCoverage) {
                this.branchCoverage = branchCoverage;
                return this;
            }
            
            public OverallMetricsBuilder methodCoverage(double methodCoverage) {
                this.methodCoverage = methodCoverage;
                return this;
            }
            
            public OverallMetricsBuilder totalLines(int totalLines) {
                this.totalLines = totalLines;
                return this;
            }
            
            public OverallMetricsBuilder coveredLines(int coveredLines) {
                this.coveredLines = coveredLines;
                return this;
            }
            
            public OverallMetricsBuilder totalFiles(int totalFiles) {
                this.totalFiles = totalFiles;
                return this;
            }
            
            public OverallMetrics build() {
                OverallMetrics metrics = new OverallMetrics();
                metrics.overallCoverage = this.overallCoverage;
                metrics.lineCoverage = this.lineCoverage;
                metrics.branchCoverage = this.branchCoverage;
                metrics.methodCoverage = this.methodCoverage;
                metrics.totalLines = this.totalLines;
                metrics.coveredLines = this.coveredLines;
                metrics.totalFiles = this.totalFiles;
                return metrics;
            }
        }
        
        // Getters
        public double getOverallCoverage() { return overallCoverage; }
        public double getLineCoverage() { return lineCoverage; }
        public double getBranchCoverage() { return branchCoverage; }
        public double getMethodCoverage() { return methodCoverage; }
        public int getTotalLines() { return totalLines; }
        public int getCoveredLines() { return coveredLines; }
        public int getTotalFiles() { return totalFiles; }
    }

    public static class PackageCoverage {
        private int fileCount;
        private int totalLines;
        private int coveredLines;
        private double averageCoverage;
        private double lineCoverage;
        
        public static PackageCoverageBuilder builder() {
            return new PackageCoverageBuilder();
        }
        
        public static class PackageCoverageBuilder {
            private int fileCount;
            private int totalLines;
            private int coveredLines;
            private double averageCoverage;
            private double lineCoverage;
            
            public PackageCoverageBuilder fileCount(int fileCount) {
                this.fileCount = fileCount;
                return this;
            }
            
            public PackageCoverageBuilder totalLines(int totalLines) {
                this.totalLines = totalLines;
                return this;
            }
            
            public PackageCoverageBuilder coveredLines(int coveredLines) {
                this.coveredLines = coveredLines;
                return this;
            }
            
            public PackageCoverageBuilder averageCoverage(double averageCoverage) {
                this.averageCoverage = averageCoverage;
                return this;
            }
            
            public PackageCoverageBuilder lineCoverage(double lineCoverage) {
                this.lineCoverage = lineCoverage;
                return this;
            }
            
            public PackageCoverage build() {
                PackageCoverage coverage = new PackageCoverage();
                coverage.fileCount = this.fileCount;
                coverage.totalLines = this.totalLines;
                coverage.coveredLines = this.coveredLines;
                coverage.averageCoverage = this.averageCoverage;
                coverage.lineCoverage = this.lineCoverage;
                return coverage;
            }
        }
        
        // Getters
        public int getFileCount() { return fileCount; }
        public int getTotalLines() { return totalLines; }
        public int getCoveredLines() { return coveredLines; }
        public double getAverageCoverage() { return averageCoverage; }
        public double getLineCoverage() { return lineCoverage; }
    }
}
