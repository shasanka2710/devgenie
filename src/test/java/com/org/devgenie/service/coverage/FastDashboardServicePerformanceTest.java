package com.org.devgenie.service.coverage;

import com.org.devgenie.model.coverage.CoverageData;
import com.org.devgenie.model.coverage.DashboardCache;
import com.org.devgenie.model.coverage.RepositoryAnalysis;
import com.org.devgenie.model.SonarBaseComponentMetrics;
import com.org.devgenie.mongo.DashboardCacheRepository;
import com.org.devgenie.service.metadata.MetadataAnalyzer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Performance test for FastDashboardService to ensure lightning-fast cache generation
 * even for large repositories with hundreds of files.
 */
public class FastDashboardServicePerformanceTest {

    @Mock
    private DashboardCacheRepository dashboardCacheRepository;

    @Mock
    private AiImprovementService aiImprovementService;

    @InjectMocks
    private FastDashboardService fastDashboardService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Mock repository save to return successfully
        when(dashboardCacheRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    /**
     * Test cache generation performance with a large repository (500 files).
     * Should complete in under 1 second for lightning-fast performance.
     */
    @Test
    public void testLargeRepositoryDashboardCacheGeneration() {
        String repoPath = "test-large-repo";
        String branch = "main";
        int numberOfFiles = 500; // Simulate large repository

        // Generate test data for large repository
        List<CoverageData> largeCoverageData = generateLargeCoverageDataset(numberOfFiles);
        RepositoryAnalysis repositoryAnalysis = RepositoryAnalysis.builder().build();
        List<MetadataAnalyzer.FileMetadata> fileMetadata = new ArrayList<>();
        SonarBaseComponentMetrics sonarMetrics = SonarBaseComponentMetrics.builder().build();

        // Measure performance
        long startTime = System.nanoTime();
        
        fastDashboardService.generateDashboardCacheFromMemory(
            repoPath, branch, largeCoverageData, repositoryAnalysis, fileMetadata, sonarMetrics);
        
        long endTime = System.nanoTime();
        long durationMs = (endTime - startTime) / 1_000_000;

        System.out.println("=== PERFORMANCE TEST RESULTS ===");
        System.out.println("Repository size: " + numberOfFiles + " files");
        System.out.println("Cache generation time: " + durationMs + " ms");
        System.out.println("Performance target: < 1000 ms");
        
        // Assert lightning-fast performance: should complete in under 1 second
        assertTrue(durationMs < 1000, 
            "Cache generation took " + durationMs + "ms, but should be under 1000ms for " + numberOfFiles + " files");
        
        System.out.println("✅ PERFORMANCE TEST PASSED - Lightning-fast cache generation!");
    }

    /**
     * Test cache generation performance with a massive repository (1000 files).
     * This tests the extreme edge case for very large codebases.
     */
    @Test
    public void testMassiveRepositoryDashboardCacheGeneration() {
        String repoPath = "test-massive-repo";
        String branch = "main";
        int numberOfFiles = 1000; // Simulate massive repository

        // Generate test data for massive repository
        List<CoverageData> massiveCoverageData = generateLargeCoverageDataset(numberOfFiles);
        RepositoryAnalysis repositoryAnalysis = RepositoryAnalysis.builder().build();
        List<MetadataAnalyzer.FileMetadata> fileMetadata = new ArrayList<>();
        SonarBaseComponentMetrics sonarMetrics = SonarBaseComponentMetrics.builder().build();

        // Measure performance
        long startTime = System.nanoTime();
        
        fastDashboardService.generateDashboardCacheFromMemory(
            repoPath, branch, massiveCoverageData, repositoryAnalysis, fileMetadata, sonarMetrics);
        
        long endTime = System.nanoTime();
        long durationMs = (endTime - startTime) / 1_000_000;

        System.out.println("=== MASSIVE REPOSITORY PERFORMANCE TEST ===");
        System.out.println("Repository size: " + numberOfFiles + " files");
        System.out.println("Cache generation time: " + durationMs + " ms");
        System.out.println("Performance target: < 2000 ms");
        
        // Assert performance: should complete in under 2 seconds even for massive repos
        assertTrue(durationMs < 2000, 
            "Cache generation took " + durationMs + "ms, but should be under 2000ms for " + numberOfFiles + " files");
        
        System.out.println("✅ MASSIVE REPOSITORY TEST PASSED - Ultra-fast cache generation!");
    }

    /**
     * Generate realistic test coverage data for performance testing
     */
    private List<CoverageData> generateLargeCoverageDataset(int numberOfFiles) {
        List<CoverageData> coverageData = new ArrayList<>();
        
        // Generate directories first
        String[] modules = {"core", "service", "controller", "repository", "util", "model", "config"};
        for (String module : modules) {
            coverageData.add(createDirectoryCoverageData("src/main/java/com/org/devgenie/" + module));
        }
        
        // Generate files
        for (int i = 0; i < numberOfFiles; i++) {
            String module = modules[i % modules.length];
            String fileName = "TestClass" + i + ".java";
            String filePath = "src/main/java/com/org/devgenie/" + module + "/" + fileName;
            
            coverageData.add(createFileCoverageData(filePath, fileName, "com.org.devgenie." + module, "TestClass" + i));
        }
        
        return coverageData;
    }

    private CoverageData createDirectoryCoverageData(String path) {
        return CoverageData.builder()
                .path(path)
                .type("DIRECTORY")
                .build();
    }

    private CoverageData createFileCoverageData(String path, String fileName, String packageName, String className) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        
        int totalLines = random.nextInt(50, 500);
        int coveredLines = random.nextInt(0, totalLines);
        int totalBranches = random.nextInt(5, 50);
        int coveredBranches = random.nextInt(0, totalBranches);
        int totalMethods = random.nextInt(3, 30);
        int coveredMethods = random.nextInt(0, totalMethods);
        
        return CoverageData.builder()
                .path(path)
                .fileName(fileName)
                .packageName(packageName)
                .className(className)
                .type("FILE")
                .totalLines(totalLines)
                .coveredLines(coveredLines)
                .lineCoverage(totalLines > 0 ? (double) coveredLines / totalLines * 100 : 0.0)
                .totalBranches(totalBranches)
                .coveredBranches(coveredBranches)
                .branchCoverage(totalBranches > 0 ? (double) coveredBranches / totalBranches * 100 : 0.0)
                .totalMethods(totalMethods)
                .coveredMethods(coveredMethods)
                .methodCoverage(totalMethods > 0 ? (double) coveredMethods / totalMethods * 100 : 0.0)
                .build();
    }
}
