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
    private AiImprovementService aiImprovementService;

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
        // This method is kept for compatibility with async methods
        return buildOptimizedFileTree(coverageData);
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

        // Convert file tree with null safety
        RepositoryDashboardService.FileTreeNode fileTree = cache.getFileTreeData() != null 
            ? convertCacheTreeToFastTree(cache.getFileTreeData())
            : new RepositoryDashboardService.FileTreeNode("src", "DIRECTORY", null);

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
        
        RepositoryDashboardService.FileTreeNode node = new RepositoryDashboardService.FileTreeNode(
            cacheNode.getName(), 
            cacheNode.getType(), 
            null // We don't need the full CoverageData for display
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
     * OPTIMIZED file tree building - efficient path processing
     */
    private DashboardCache.FileTreeData buildOptimizedFileTree(List<CoverageData> coverageData) {
        Map<String, DashboardCache.FileTreeData> nodeMap = new HashMap<>();
        DashboardCache.FileTreeData root = DashboardCache.FileTreeData.builder()
                .name("src").type("DIRECTORY").path("src").children(new ArrayList<>()).build();
        nodeMap.put("src", root);

        // Process paths efficiently
        for (CoverageData data : coverageData) {
            String[] pathParts = data.getPath().split("/");
            DashboardCache.FileTreeData currentNode = root;
            StringBuilder currentPath = new StringBuilder();
            
            for (int i = 0; i < pathParts.length; i++) {
                if (i > 0) currentPath.append("/");
                currentPath.append(pathParts[i]);
                String fullPath = currentPath.toString();
                
                if (!nodeMap.containsKey(fullPath)) {
                    DashboardCache.FileTreeData newNode = DashboardCache.FileTreeData.builder()
                            .name(pathParts[i])
                            .type(i == pathParts.length - 1 ? data.getType() : "DIRECTORY")
                            .path(fullPath)
                            .lineCoverage(data.getLineCoverage())
                            .children(new ArrayList<>())
                            .build();
                    nodeMap.put(fullPath, newNode);
                    currentNode.getChildren().add(newNode);
                    currentNode = newNode;
                } else {
                    currentNode = nodeMap.get(fullPath);
                }
            }
        }
        return root;
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
}
