package com.org.devgenie.service.coverage;

import com.org.devgenie.model.coverage.CoverageData;
import com.org.devgenie.model.coverage.DashboardCache;
import com.org.devgenie.model.coverage.RepositoryAnalysis;
import com.org.devgenie.model.SonarBaseComponentMetrics;
import com.org.devgenie.mongo.CoverageDataFlatMongoRepository;
import com.org.devgenie.mongo.DashboardCacheRepository;
import com.org.devgenie.service.metadata.MetadataAnalyzer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class FastDashboardService {

    @Autowired
    private DashboardCacheRepository dashboardCacheRepository;
    
    @Autowired
    private CoverageDataFlatMongoRepository coverageRepository;
    
    @Autowired
    private RepositoryDashboardService repositoryDashboardService;

    /**
     * Fast dashboard retrieval - loads from cache instantly
     */
    public RepositoryDashboardService.DashboardData getFastDashboardData(String repoPath, String branch) {
        log.info("Fast loading dashboard for repo: {} branch: {}", repoPath, branch);
        
        Optional<DashboardCache> cached = dashboardCacheRepository.findByRepoPathAndBranch(repoPath, branch);
        
        if (cached.isPresent() && "COMPLETED".equals(cached.get().getStatus())) {
            log.info("Loading dashboard from cache - instant response");
            return convertCacheToFastDashboard(cached.get());
        } else {
            log.warn("No cached dashboard found for repo: {} - falling back to slow load", repoPath);
            // Trigger async cache generation
            generateDashboardCacheAsync(repoPath, branch);
            // Return empty dashboard with status
            return createEmptyDashboardWithStatus("PROCESSING");
        }
    }

    /**
     * Pre-generate and cache dashboard data during repository analysis
     */
    @Async
    public void generateDashboardCacheAsync(String repoPath, String branch) {
        long overallStart = System.nanoTime();
        try {
            long stepStart, stepEnd;
            stepStart = System.nanoTime();
            log.info("Starting async dashboard cache generation for repo: {}", repoPath);
            
            // Mark as processing
            DashboardCache processingCache = DashboardCache.builder()
                    .repoPath(repoPath)
                    .branch(branch)
                    .status("PROCESSING")
                    .generatedAt(LocalDateTime.now())
                    .lastUpdated(LocalDateTime.now())
                    .build();
            dashboardCacheRepository.save(processingCache);
            stepEnd=System.nanoTime();
            log.info("Marked dashboard cache as PROCESSING for repo: {} in {} ms", repoPath, (stepEnd - stepStart) / 1_000_000);

            log.info("Generating dashboard cache for repo: {} branch: {}", repoPath, branch);
            stepStart = System.nanoTime();
            // Generate dashboard data
            DashboardCache dashboardCache = generateDashboardCache(repoPath, branch);
            stepEnd= System.nanoTime();
            log.info("Generated dashboard cache for repo: {} in {} ms", repoPath, (stepEnd - stepStart) / 1_000_000);
            
            // Save completed cache
            dashboardCache.setStatus("COMPLETED");
            dashboardCache.setLastUpdated(LocalDateTime.now());
            dashboardCacheRepository.save(dashboardCache);

            long overallEnd = System.nanoTime();
            log.info("Overall dashboard cache generation for repo: {} took {} ms", repoPath, (overallEnd - overallStart) / 1_000_000);
            
        } catch (Exception e) {
            log.error("Failed to generate dashboard cache for repo: {}", repoPath, e);
            
            // Save error status
            DashboardCache errorCache = DashboardCache.builder()
                    .repoPath(repoPath)
                    .branch(branch)
                    .status("ERROR")
                    .errorMessage(e.getMessage())
                    .generatedAt(LocalDateTime.now())
                    .lastUpdated(LocalDateTime.now())
                    .build();
            dashboardCacheRepository.save(errorCache);
        }
    }

    /**
     * Synchronous dashboard cache generation - used during analysis
     */
    public void generateDashboardCacheSync(String repoPath, String branch) {
        try {
            log.info("Generating dashboard cache synchronously for repo: {}", repoPath);
            DashboardCache dashboardCache = generateDashboardCache(repoPath, branch);
            dashboardCache.setStatus("COMPLETED");
            dashboardCacheRepository.save(dashboardCache);
            log.info("Dashboard cache generated and saved for repo: {}", repoPath);
        } catch (Exception e) {
            log.error("Failed to generate dashboard cache synchronously for repo: {}", repoPath, e);
        }
    }

    private DashboardCache generateDashboardCache(String repoPath, String branch) {
        log.info("Generating dashboard cache for repo: {} branch: {}", repoPath, branch);
        List<CoverageData> allCoverageData = coverageRepository.findByRepoPathAndBranch(repoPath, branch);
        
        if (allCoverageData.isEmpty()) {
            throw new RuntimeException("No coverage data found for repository");
        }

        // Use optimized generation for performance
        return generateOptimizedDashboardCache(repoPath, branch, allCoverageData);
    }

    private DashboardCache.OverallMetrics calculateOverallMetrics(List<CoverageData> coverageData) {
        // This method is kept for compatibility with async methods
        List<CoverageData> files = coverageData.stream()
                .filter(data -> "FILE".equals(data.getType()))
                .collect(Collectors.toList());
        return calculateOptimizedMetrics(files);
    }

    private DashboardCache.FileTreeData buildFileTree(List<CoverageData> coverageData) {
        // Use the new package-style file tree from RepositoryDashboardService
        RepositoryDashboardService.FileTreeNode packageStyleTree = 
            repositoryDashboardService.buildPackageStyleFileTree(coverageData);
        
        // Convert to cache format
        return convertPackageTreeToCacheTree(packageStyleTree);
    }

    private List<DashboardCache.FileDetailsData> buildFileDetails(List<CoverageData> coverageData) {
        // This method is kept for compatibility but uses optimized version
        List<CoverageData> files = coverageData.stream()
                .filter(data -> "FILE".equals(data.getType()))
                .collect(Collectors.toList());
        return buildOptimizedFileDetails(files);
    }

    private DashboardCache.FileDetailsData convertToFileDetails(CoverageData data) {
        // PERFORMANCE OPTIMIZATION: Remove AI processing for speed
        // AI opportunities can be generated on-demand in the UI if needed
        return DashboardCache.FileDetailsData.builder()
                .fileName(data.getFileName())
                .filePath(data.getPath())
                .packageName(data.getPackageName())
                .className(data.getClassName())
                .lineCoverage(data.getLineCoverage())
                .branchCoverage(data.getBranchCoverage())
                .methodCoverage(data.getMethodCoverage())
                .totalLines(data.getTotalLines())
                .coveredLines(data.getCoveredLines())
                .totalBranches(data.getTotalBranches())
                .coveredBranches(data.getCoveredBranches())
                .totalMethods(data.getTotalMethods())
                .coveredMethods(data.getCoveredMethods())
                .improvementOpportunities(Collections.emptyList()) // Empty for performance
                .build();
    }

    private RepositoryDashboardService.DashboardData convertCacheToFastDashboard(DashboardCache cache) {
        // Convert cached data back to dashboard format
        RepositoryDashboardService.OverallMetrics overallMetrics = 
            RepositoryDashboardService.OverallMetrics.builder()
                .overallCoverage(cache.getOverallMetrics().getOverallCoverage())
                .lineCoverage(cache.getOverallMetrics().getLineCoverage())
                .branchCoverage(cache.getOverallMetrics().getBranchCoverage())
                .methodCoverage(cache.getOverallMetrics().getMethodCoverage())
                .totalLines(cache.getOverallMetrics().getTotalLines())
                .coveredLines(cache.getOverallMetrics().getCoveredLines())
                .totalBranches(cache.getOverallMetrics().getTotalBranches())
                .coveredBranches(cache.getOverallMetrics().getCoveredBranches())
                .totalMethods(cache.getOverallMetrics().getTotalMethods())
                .coveredMethods(cache.getOverallMetrics().getCoveredMethods())
                .build();

        // Create file details lookup map for enhanced tree node data
        Map<String, DashboardCache.FileDetailsData> fileDetailsMap = new HashMap<>();
        if (cache.getFileDetails() != null) {
            cache.getFileDetails().forEach(file -> {
                if (file != null && file.getFilePath() != null) {
                    fileDetailsMap.put(file.getFilePath(), file);
                }
            });
        }

        // Convert file tree with enhanced coverage data and fallback for malformed trees
        RepositoryDashboardService.FileTreeNode fileTree;
        if (cache.getFileTreeData() != null && isValidFileTree(cache.getFileTreeData())) {
            fileTree = convertCacheTreeToFastTreeWithDetails(cache.getFileTreeData(), fileDetailsMap);
        } else {
            log.warn("File tree data is malformed or missing - building from fileDetails");
            fileTree = buildFileTreeFromFileDetails(cache.getFileDetails());
        }

        // Convert file details with null safety
        List<RepositoryDashboardService.FileDetails> fileDetails = 
            cache.getFileDetails() != null 
                ? cache.getFileDetails().stream()
                    .filter(file -> file != null) // Filter out null files
                    .map(this::convertCacheFileToFastFile)
                    .collect(Collectors.toList())
                : new ArrayList<>();

        return RepositoryDashboardService.DashboardData.builder()
                .overallMetrics(overallMetrics)
                .fileTree(fileTree)
                .fileDetails(fileDetails)
                .build();
    }

    private RepositoryDashboardService.FileTreeNode convertCacheTreeToFastTree(DashboardCache.FileTreeData cacheNode) {
        if (cacheNode == null) {
            return null; // Handle null cache nodes
        }
        
        // Create CoverageData for file tree nodes to enable template data binding
        CoverageData coverageData = createCoverageDataFromCacheNode(cacheNode);
        
        RepositoryDashboardService.FileTreeNode node = new RepositoryDashboardService.FileTreeNode(
            cacheNode.getName(), 
            cacheNode.getType(), 
            coverageData // ✅ Now provides actual coverage data for templates
        );
        
        if (cacheNode.getChildren() != null) {
            for (DashboardCache.FileTreeData child : cacheNode.getChildren()) {
                if (child != null) { // Add null check for children
                    RepositoryDashboardService.FileTreeNode childNode = convertCacheTreeToFastTree(child);
                    if (childNode != null) {
                        node.addChild(childNode);
                    }
                }
            }
        }
        
        return node;
    }

    /**
     * Enhanced tree conversion that includes detailed coverage data from fileDetails
     */
    private RepositoryDashboardService.FileTreeNode convertCacheTreeToFastTreeWithDetails(
            DashboardCache.FileTreeData cacheNode, 
            Map<String, DashboardCache.FileDetailsData> fileDetailsMap) {
        
        if (cacheNode == null) {
            return null;
        }
        
        // Get detailed coverage data for files
        CoverageData coverageData;
        if ("FILE".equals(cacheNode.getType()) && fileDetailsMap.containsKey(cacheNode.getPath())) {
            DashboardCache.FileDetailsData fileDetail = fileDetailsMap.get(cacheNode.getPath());
            coverageData = createCoverageDataFromFileDetail(fileDetail, cacheNode);
        } else {
            coverageData = createCoverageDataFromCacheNode(cacheNode);
        }
        
        RepositoryDashboardService.FileTreeNode node = new RepositoryDashboardService.FileTreeNode(
            cacheNode.getName(), 
            cacheNode.getType(), 
            coverageData
        );
        
        // Recursively process children
        if (cacheNode.getChildren() != null) {
            for (DashboardCache.FileTreeData child : cacheNode.getChildren()) {
                if (child != null) {
                    RepositoryDashboardService.FileTreeNode childNode = 
                        convertCacheTreeToFastTreeWithDetails(child, fileDetailsMap);
                    if (childNode != null) {
                        node.addChild(childNode);
                    }
                }
            }
        }
        
        return node;
    }

    /**
     * Create enhanced CoverageData from FileDetailsData for complete file information
     */
    private CoverageData createCoverageDataFromFileDetail(
            DashboardCache.FileDetailsData fileDetail, 
            DashboardCache.FileTreeData treeNode) {
        
        return CoverageData.builder()
                .fileName(fileDetail.getFileName())
                .path(fileDetail.getFilePath())
                .type(treeNode.getType())
                .packageName(fileDetail.getPackageName())
                .className(fileDetail.getClassName())
                .lineCoverage(fileDetail.getLineCoverage())
                .branchCoverage(fileDetail.getBranchCoverage())
                .methodCoverage(fileDetail.getMethodCoverage())
                .totalLines(fileDetail.getTotalLines())
                .coveredLines(fileDetail.getCoveredLines())
                .totalBranches(fileDetail.getTotalBranches())
                .coveredBranches(fileDetail.getCoveredBranches())
                .totalMethods(fileDetail.getTotalMethods())
                .coveredMethods(fileDetail.getCoveredMethods())
                .build();
    }

    private RepositoryDashboardService.FileDetails convertCacheFileToFastFile(DashboardCache.FileDetailsData cached) {
        List<RepositoryDashboardService.ImprovementOpportunity> opportunities = 
            cached.getImprovementOpportunities().stream()
                .map(opp -> RepositoryDashboardService.ImprovementOpportunity.builder()
                        .type(opp.getType())
                        .description(opp.getDescription())
                        .priority(opp.getPriority())
                        .estimatedImpact(opp.getEstimatedImpact())
                        .build())
                .collect(Collectors.toList());

        return RepositoryDashboardService.FileDetails.builder()
                .fileName(cached.getFileName())
                .filePath(cached.getFilePath())
                .packageName(cached.getPackageName())
                .className(cached.getClassName())
                .lineCoverage(cached.getLineCoverage())
                .branchCoverage(cached.getBranchCoverage())
                .methodCoverage(cached.getMethodCoverage())
                .totalLines(cached.getTotalLines())
                .coveredLines(cached.getCoveredLines())
                .totalBranches(cached.getTotalBranches())
                .coveredBranches(cached.getCoveredBranches())
                .totalMethods(cached.getTotalMethods())
                .coveredMethods(cached.getCoveredMethods())
                .improvementOpportunities(opportunities)
                .build();
    }

    private RepositoryDashboardService.DashboardData createEmptyDashboardWithStatus(String status) {
        return RepositoryDashboardService.DashboardData.builder()
                .overallMetrics(RepositoryDashboardService.OverallMetrics.builder()
                        .overallCoverage(0.0)
                        .lineCoverage(0.0)
                        .branchCoverage(0.0)
                        .methodCoverage(0.0)
                        .build())
                .fileTree(new RepositoryDashboardService.FileTreeNode("src", "DIRECTORY", null))
                .fileDetails(new ArrayList<>())
                .build();
    }

    /**
     * Clear cache for a repository (useful when re-analyzing)
     */
    public void clearDashboardCache(String repoPath, String branch) {
        dashboardCacheRepository.deleteByRepoPathAndBranch(repoPath, branch);
        log.info("Cleared dashboard cache for repo: {} branch: {}", repoPath, branch);
    }

    /**
     * Check if cache is available for fast loading
     */
    public boolean isCacheAvailable(String repoPath, String branch) {
        Optional<DashboardCache> cached = dashboardCacheRepository.findByRepoPathAndBranch(repoPath, branch);
        return cached.isPresent() && "COMPLETED".equals(cached.get().getStatus());
    }

    /**
     * LIGHTNING-FAST dashboard cache generation from in-memory data
     * Optimized for high performance with minimal database operations
     */
    public void generateDashboardCacheFromMemory(
            String repoPath, 
            String branch, 
            List<CoverageData> coverageData,
            RepositoryAnalysis repositoryAnalysis,
            List<MetadataAnalyzer.FileMetadata> fileMetadata,
            SonarBaseComponentMetrics sonarMetrics) {
        
        long startTime = System.nanoTime();
        try {
            log.info("FAST: Generating dashboard cache for repo: {} with {} coverage nodes", repoPath, coverageData.size());
            
            if (coverageData == null || coverageData.isEmpty()) {
                log.warn("No coverage data - skipping cache generation");
                return;
            }

            // PERFORMANCE OPTIMIZATION: Generate cache directly - no intermediate DB writes
            DashboardCache dashboardCache = generateOptimizedDashboardCache(
                repoPath, branch, coverageData);
            
            // Single database write with COMPLETED status
            dashboardCache.setStatus("COMPLETED");
            dashboardCacheRepository.save(dashboardCache);
            
            long endTime = System.nanoTime();
            log.info("FAST: Dashboard cache completed for {} nodes in {} ms", 
                coverageData.size(), (endTime - startTime) / 1_000_000);
            
        } catch (Exception e) {
            log.error("FAST: Dashboard cache generation failed for repo: {}", repoPath, e);
            // Single error status write
            DashboardCache errorCache = DashboardCache.builder()
                    .repoPath(repoPath)
                    .branch(branch)
                    .status("ERROR")
                    .errorMessage(e.getMessage())
                    .generatedAt(LocalDateTime.now())
                    .build();
            dashboardCacheRepository.save(errorCache);
        }
    }

    /**
     * ULTRA-FAST dashboard cache generation optimized for performance
     */
    private DashboardCache generateOptimizedDashboardCache(String repoPath, String branch, List<CoverageData> coverageData) {
        // Pre-filter files once to avoid multiple stream operations
        List<CoverageData> files = coverageData.stream()
                .filter(data -> "FILE".equals(data.getType()))
                .collect(Collectors.toList());
        
        // FAST metrics calculation - single pass
        DashboardCache.OverallMetrics overallMetrics = calculateOptimizedMetrics(files);
        
        // FAST file tree - optimized algorithm
        DashboardCache.FileTreeData fileTree = buildOptimizedFileTree(coverageData);
        
        // FAST file details - NO AI processing for performance
        List<DashboardCache.FileDetailsData> fileDetails = buildOptimizedFileDetails(files);

        return DashboardCache.builder()
                .repoPath(repoPath)
                .branch(branch)
                .generatedAt(LocalDateTime.now())
                .overallMetrics(overallMetrics)
                .fileTreeData(fileTree)
                .fileDetails(fileDetails)
                .totalFiles(files.size())
                .totalDirectories(coverageData.size() - files.size())
                .lastUpdated(LocalDateTime.now())
                .build();
    }

    /**
     * OPTIMIZED metrics calculation - single pass through data
     */
    private DashboardCache.OverallMetrics calculateOptimizedMetrics(List<CoverageData> files) {
        if (files.isEmpty()) {
            return DashboardCache.OverallMetrics.builder()
                    .overallCoverage(0.0).lineCoverage(0.0).branchCoverage(0.0).methodCoverage(0.0)
                    .totalLines(0).coveredLines(0).totalBranches(0).coveredBranches(0).totalMethods(0).coveredMethods(0)
                    .build();
        }

        // Single pass aggregation
        int totalLines = 0, coveredLines = 0, totalBranches = 0, coveredBranches = 0, totalMethods = 0, coveredMethods = 0;
        
        for (CoverageData file : files) {
            totalLines += file.getTotalLines();
            coveredLines += file.getCoveredLines();
            totalBranches += file.getTotalBranches();
            coveredBranches += file.getCoveredBranches();
            totalMethods += file.getTotalMethods();
            coveredMethods += file.getCoveredMethods();
        }

        double lineCoverage = totalLines > 0 ? (double) coveredLines / totalLines * 100 : 0.0;
        double branchCoverage = totalBranches > 0 ? (double) coveredBranches / totalBranches * 100 : 0.0;
        double methodCoverage = totalMethods > 0 ? (double) coveredMethods / totalMethods * 100 : 0.0;
        double overallCoverage = (lineCoverage + branchCoverage + methodCoverage) / 3;

        return DashboardCache.OverallMetrics.builder()
                .overallCoverage(Math.round(overallCoverage * 10.0) / 10.0)
                .lineCoverage(Math.round(lineCoverage * 10.0) / 10.0)
                .branchCoverage(Math.round(branchCoverage * 10.0) / 10.0)
                .methodCoverage(Math.round(methodCoverage * 10.0) / 10.0)
                .totalLines(totalLines).coveredLines(coveredLines)
                .totalBranches(totalBranches).coveredBranches(coveredBranches)
                .totalMethods(totalMethods).coveredMethods(coveredMethods)
                .build();
    }

    /**
     * OPTIMIZED file tree building - FINAL FIX for correct tree structure
     */
    private DashboardCache.FileTreeData buildOptimizedFileTree(List<CoverageData> coverageData) {
        log.info("Building file tree from {} coverage data items", coverageData.size());
        
        Map<String, DashboardCache.FileTreeData> nodeMap = new HashMap<>();
        DashboardCache.FileTreeData root = DashboardCache.FileTreeData.builder()
                .name("src").type("DIRECTORY").path("src").children(new ArrayList<>()).build();
        nodeMap.put("src", root);

        // Process each file/directory path
        for (CoverageData data : coverageData) {
            String[] pathParts = data.getPath().split("/");
            
            // Skip if path doesn't start with "src"
            if (pathParts.length == 0 || !"src".equals(pathParts[0])) continue;
            
            log.debug("Processing path: {} (type: {})", data.getPath(), data.getType());
            
            // Build all directory levels first
            StringBuilder cumulativePath = new StringBuilder("src");
            DashboardCache.FileTreeData currentParent = root;
            
            // Process all path segments except the last one (which might be a file)
            for (int i = 1; i < pathParts.length - 1; i++) {
                String dirName = pathParts[i];
                if (dirName.isEmpty()) continue;
                
                cumulativePath.append("/").append(dirName);
                String dirPath = cumulativePath.toString();
                
                // Get or create directory node
                DashboardCache.FileTreeData dirNode = nodeMap.get(dirPath);
                if (dirNode == null) {
                    dirNode = DashboardCache.FileTreeData.builder()
                            .name(dirName)
                            .type("DIRECTORY")
                            .path(dirPath)
                            .lineCoverage(0.0)
                            .children(new ArrayList<>())
                            .build();
                    
                    currentParent.getChildren().add(dirNode);
                    nodeMap.put(dirPath, dirNode);
                    log.debug("Created directory node: {} at path: {}", dirName, dirPath);
                }
                currentParent = dirNode;
            }
            
            // Now handle the final segment (file or directory)
            if (pathParts.length > 1) {
                String finalName = pathParts[pathParts.length - 1];
                cumulativePath.append("/").append(finalName);
                String finalPath = cumulativePath.toString();
                
                // Check if this final node already exists
                DashboardCache.FileTreeData finalNode = nodeMap.get(finalPath);
                if (finalNode == null) {
                    finalNode = DashboardCache.FileTreeData.builder()
                            .name(finalName)
                            .type(data.getType())
                            .path(finalPath)
                            .lineCoverage("FILE".equals(data.getType()) ? data.getLineCoverage() : 0.0)
                            .children(new ArrayList<>())
                            .build();
                    
                    currentParent.getChildren().add(finalNode);
                    nodeMap.put(finalPath, finalNode);
                    log.debug("Created {} node: {} at path: {}", data.getType(), finalName, finalPath);
                }
            }
        }
        
        // Log final tree structure
        log.info("File tree built successfully:");
        logTreeStructure(root, 0);
        
        return root;
    }

    /**
     * Helper method to log tree structure for debugging
     */
    private void logTreeStructure(DashboardCache.FileTreeData node, int depth) {
        if (node == null) return;
        
        String indent = "  ".repeat(depth);
        log.info("{}|- {} ({}) - {} children", indent, node.getName(), node.getType(), 
                node.getChildren() != null ? node.getChildren().size() : 0);
        
        if (node.getChildren() != null) {
            for (DashboardCache.FileTreeData child : node.getChildren()) {
                logTreeStructure(child, depth + 1);
            }
        }
    }

    /**
     * ULTRA-FAST file details - NO AI processing for performance
     */
    private List<DashboardCache.FileDetailsData> buildOptimizedFileDetails(List<CoverageData> files) {
        List<DashboardCache.FileDetailsData> result = new ArrayList<>(files.size());
        
        for (CoverageData data : files) {
            // NO AI processing - just basic data mapping for speed
            DashboardCache.FileDetailsData fileDetail = DashboardCache.FileDetailsData.builder()
                    .fileName(data.getFileName())
                    .filePath(data.getPath())
                    .packageName(data.getPackageName())
                    .className(data.getClassName())
                    .lineCoverage(data.getLineCoverage())
                    .branchCoverage(data.getBranchCoverage())
                    .methodCoverage(data.getMethodCoverage())
                    .totalLines(data.getTotalLines())
                    .coveredLines(data.getCoveredLines())
                    .totalBranches(data.getTotalBranches())
                    .coveredBranches(data.getCoveredBranches())
                    .totalMethods(data.getTotalMethods())
                    .coveredMethods(data.getCoveredMethods())
                    .improvementOpportunities(Collections.emptyList()) // Empty for performance
                    .build();
            result.add(fileDetail);
        }
        return result;
    }

    /**
     * Convert package-style FileTreeNode to DashboardCache.FileTreeData format
     */
    private DashboardCache.FileTreeData convertPackageTreeToCacheTree(RepositoryDashboardService.FileTreeNode node) {
        if (node == null) {
            return null;
        }
        
        // Build the cache tree data
        DashboardCache.FileTreeData.FileTreeDataBuilder builder = DashboardCache.FileTreeData.builder()
                .name(node.getName())
                .type(node.getType())
                .nodeType(node.getNodeType())
                .packageName(node.getPackageName())
                .flattened(node.isFlattened());
        
        // Add coverage data if available
        if (node.getData() != null) {
            CoverageData data = node.getData();
            builder
                .path(data.getPath())
                .lineCoverage(data.getLineCoverage())
                .branchCoverage(data.getBranchCoverage())
                .methodCoverage(data.getMethodCoverage());
        }
        
        // Convert children recursively
        if (node.getChildren() != null && !node.getChildren().isEmpty()) {
            List<DashboardCache.FileTreeData> children = node.getChildren().stream()
                    .map(this::convertPackageTreeToCacheTree)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            builder.children(children);
        }
        
        return builder.build();
    }

    /**
     * Create CoverageData from DashboardCache.FileTreeData for template binding
     */
    private CoverageData createCoverageDataFromCacheNode(DashboardCache.FileTreeData cacheNode) {
        return CoverageData.builder()
                .fileName(cacheNode.getName())
                .path(cacheNode.getPath())
                .type(cacheNode.getType())
                .lineCoverage(cacheNode.getLineCoverage() != null ? cacheNode.getLineCoverage() : 0.0)
                .branchCoverage(0.0) // Not available in FileTreeData, using default
                .methodCoverage(0.0) // Not available in FileTreeData, using default
                .totalLines(0) // Not available in FileTreeData, will be populated from fileDetails if needed
                .coveredLines(0) // Not available in FileTreeData, will be populated from fileDetails if needed
                .totalBranches(0)
                .coveredBranches(0)
                .totalMethods(0)
                .coveredMethods(0)
                .build();
    }

    /**
     * Create default directory CoverageData
     */
    private CoverageData createDefaultDirectoryData() {
        return CoverageData.builder()
                .fileName("src")
                .path("src")
                .type("DIRECTORY")
                .lineCoverage(0.0)
                .branchCoverage(0.0)
                .methodCoverage(0.0)
                .totalLines(0)
                .coveredLines(0)
                .totalBranches(0)
                .coveredBranches(0)
                .totalMethods(0)
                .coveredMethods(0)
                .build();
    }

    /**
     * Validate cache completeness and structure
     */
    private boolean isValidCache(DashboardCache cache) {
        if (cache == null) {
            log.warn("Cache is null");
            return false;
        }
        
        if (!"COMPLETED".equals(cache.getStatus())) {
            log.warn("Cache status is not COMPLETED: {}", cache.getStatus());
            return false;
        }
        
        if (cache.getOverallMetrics() == null) {
            log.warn("Cache missing overall metrics");
            return false;
        }
        
        if (cache.getFileTreeData() == null) {
            log.warn("Cache missing file tree data");
            return false;
        }
        
        if (cache.getFileDetails() == null || cache.getFileDetails().isEmpty()) {
            log.warn("Cache missing file details");
            return false;
        }
        
        // Check for structural integrity
        boolean hasValidFileData = cache.getFileDetails().stream()
            .anyMatch(file -> file != null && file.getFilePath() != null && file.getFileName() != null);
            
        if (!hasValidFileData) {
            log.warn("Cache has no valid file data");
            return false;
        }
        
        log.debug("Cache validation passed for repo: {} branch: {}", cache.getRepoPath(), cache.getBranch());
        return true;
    }

    /**
     * Enhanced fast dashboard with cache validation and recovery
     */
    public RepositoryDashboardService.DashboardData getFastDashboardDataWithValidation(String repoPath, String branch) {
        log.info("Loading dashboard with validation for repo: {} branch: {}", repoPath, branch);
        
        Optional<DashboardCache> cached = dashboardCacheRepository.findByRepoPathAndBranch(repoPath, branch);
        
        if (cached.isPresent()) {
            DashboardCache cache = cached.get();
            
            if (isValidCache(cache)) {
                log.info("Loading dashboard from valid cache - instant response");
                return convertCacheToFastDashboard(cache);
            } else {
                log.warn("Cache exists but is invalid - attempting recovery for repo: {}", repoPath);
                // Clear invalid cache and regenerate
                dashboardCacheRepository.delete(cache);
                generateDashboardCacheAsync(repoPath, branch);
                return createEmptyDashboardWithStatus("CACHE_RECOVERY");
            }
        } else {
            log.warn("No cache found for repo: {} - generating new cache", repoPath);
            generateDashboardCacheAsync(repoPath, branch);
            return createEmptyDashboardWithStatus("PROCESSING");
        }
    }

    /**
     * Force cache refresh - useful for recovery or manual refresh
     */
    public void forceCacheRefresh(String repoPath, String branch) {
        log.info("FORCE REFRESH: Force refreshing cache for repo: {} branch: {}", repoPath, branch);
        
        try {
            // Clear existing cache
            clearDashboardCache(repoPath, branch);
            
            // Regenerate cache synchronously for immediate availability
            generateDashboardCacheSync(repoPath, branch);
            
            log.info("FORCE REFRESH: Cache refresh completed for repo: {}", repoPath);
        } catch (Exception e) {
            log.error("FORCE REFRESH: Cache refresh failed for repo: {}", repoPath, e);
            throw new RuntimeException("Cache refresh failed", e);
        }
    }

    /**
     * Get cache health status for monitoring
     */
    public Map<String, Object> getCacheHealthStatus(String repoPath, String branch) {
        Map<String, Object> status = new HashMap<>();
        
        Optional<DashboardCache> cached = dashboardCacheRepository.findByRepoPathAndBranch(repoPath, branch);
        
        if (cached.isPresent()) {
            DashboardCache cache = cached.get();
            boolean isValid = isValidCache(cache);
            
            status.put("exists", true);
            status.put("valid", isValid);
            status.put("status", cache.getStatus());
            status.put("generatedAt", cache.getGeneratedAt());
            status.put("lastUpdated", cache.getLastUpdated());
            status.put("totalFiles", cache.getTotalFiles());
            status.put("hasFileTree", cache.getFileTreeData() != null);
            status.put("hasFileDetails", cache.getFileDetails() != null && !cache.getFileDetails().isEmpty());
            status.put("hasOverallMetrics", cache.getOverallMetrics() != null);
            
            if (cache.getErrorMessage() != null) {
                status.put("errorMessage", cache.getErrorMessage());
            }
        } else {
            status.put("exists", false);
            status.put("valid", false);
            status.put("status", "NOT_FOUND");
        }
        
        return status;
    }
    
    // Helper method to validate if file tree data is valid
    private boolean isValidFileTree(DashboardCache.FileTreeData treeData) {
        if (treeData == null) {
            return false;
        }
        
        // Check if the tree node has valid data
        return hasValidTreeData(treeData);
    }
    
    // Recursively check if tree data has valid structure
    private boolean hasValidTreeData(DashboardCache.FileTreeData node) {
        if (node == null) {
            return false;
        }
        
        // A valid node should have at least a name or be a valid directory structure
        if (node.getName() == null && node.getType() == null && node.getPath() == null) {
            return false;
        }
        
        // If it has children, at least some should be valid
        if (node.getChildren() != null && !node.getChildren().isEmpty()) {
            return node.getChildren().stream().anyMatch(this::hasValidTreeData);
        }
        
        // If it's a leaf node, it should have a name
        return node.getName() != null;
    }
    
    // Build file tree from file details when tree data is malformed
    private RepositoryDashboardService.FileTreeNode buildFileTreeFromFileDetails(List<DashboardCache.FileDetailsData> fileDetails) {
        if (fileDetails == null || fileDetails.isEmpty()) {
            return new RepositoryDashboardService.FileTreeNode(
                "root", 
                "DIRECTORY",
                CoverageData.builder()
                    .name("root")
                    .type("DIRECTORY")
                    .lineCoverage(0.0)
                    .branchCoverage(0.0)
                    .build()
            );
        }
        
        // Build tree structure from file paths
        RepositoryDashboardService.FileTreeNode root = new RepositoryDashboardService.FileTreeNode(
            "src", 
            "DIRECTORY",
            CoverageData.builder()
                .name("src")
                .type("DIRECTORY")
                .lineCoverage(0.0)
                .branchCoverage(0.0)
                .build()
        );
        
        // Use a more robust tree building approach
        Map<String, RepositoryDashboardService.FileTreeNode> pathToNodeMap = new HashMap<>();
        pathToNodeMap.put("src", root);
        
        for (DashboardCache.FileDetailsData file : fileDetails) {
            if (file == null || file.getFilePath() == null) continue;
            
            String filePath = file.getFilePath();
            String[] pathParts = filePath.split("/");
            
            // Skip if path doesn't start with "src"
            if (pathParts.length == 0 || !"src".equals(pathParts[0])) continue;
            
            // Build path progressively and create missing directories
            StringBuilder pathBuilder = new StringBuilder();
            RepositoryDashboardService.FileTreeNode currentNode = root;
            
            // Start from index 1 (skip "src" since it's the root)
            for (int i = 1; i < pathParts.length - 1; i++) {
                String dirName = pathParts[i];
                if (dirName.isEmpty()) continue;
                
                // Build cumulative path properly
                if (pathBuilder.length() > 0) {
                    pathBuilder.append("/");
                }
                pathBuilder.append(dirName);
                String cumulativePath = "src/" + pathBuilder.toString();
                
                // Get or create directory node
                RepositoryDashboardService.FileTreeNode dirNode = pathToNodeMap.get(cumulativePath);
                if (dirNode == null) {
                    dirNode = new RepositoryDashboardService.FileTreeNode(
                        dirName,
                        "DIRECTORY",
                        CoverageData.builder()
                            .name(dirName)
                            .type("DIRECTORY")
                            .path(cumulativePath)
                            .lineCoverage(0.0)
                            .branchCoverage(0.0)
                            .build()
                    );
                    currentNode.addChild(dirNode);
                    pathToNodeMap.put(cumulativePath, dirNode);
                }
                
                currentNode = dirNode;
            }
            
            // Create file node
            String fileName = pathParts[pathParts.length - 1];
            CoverageData fileData = CoverageData.builder()
                .name(fileName)
                .type("FILE")
                .path(filePath)
                .fileName(fileName)
                .packageName(file.getPackageName())
                .className(file.getClassName())
                .lineCoverage(file.getLineCoverage())
                .branchCoverage(file.getBranchCoverage())
                .methodCoverage(file.getMethodCoverage())
                .totalLines(file.getTotalLines())
                .coveredLines(file.getCoveredLines())
                .totalBranches(file.getTotalBranches())
                .coveredBranches(file.getCoveredBranches())
                .totalMethods(file.getTotalMethods())
                .coveredMethods(file.getCoveredMethods())
                .build();
            
            RepositoryDashboardService.FileTreeNode fileNode = new RepositoryDashboardService.FileTreeNode(
                fileName,
                "FILE",
                fileData
            );
            
            currentNode.addChild(fileNode);
        }
        
        return root;
    }
}
