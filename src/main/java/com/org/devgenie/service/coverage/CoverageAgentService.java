package com.org.devgenie.service.coverage;

import com.org.devgenie.config.CoverageConfiguration;
import com.org.devgenie.dto.coverage.*;
import com.org.devgenie.exception.coverage.CoverageException;
import com.org.devgenie.model.SonarQubeMetricsResponse;
import com.org.devgenie.model.coverage.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.nio.file.Paths;

@Service
@Slf4j
public class CoverageAgentService {

    @Autowired
    private CoverageDataService coverageDataService;

    @Autowired
    private FileAnalysisService fileAnalysisService;

    @Autowired
    private TestGenerationService testGenerationService;

    @Autowired
    private JacocoService jacocoService;

    @Autowired
    private GitService gitService;

    @Autowired
    private CoverageConfiguration config;

    @Autowired
    private ProjectConfigDetectionService projectConfigService;

    @Autowired
    private SessionManagementService sessionManagementService;

    @Autowired
    private RepositoryService repositoryService;

    public CoverageResponse increaseFileCoverage(FileCoverageRequest request) {
        log.info("Starting file coverage increase for: {}", request.getFilePath());

        try {
            // Analyze single file
            FileAnalysisResult analysis = fileAnalysisService.analyzeFile(request.getFilePath());

            // Generate tests for the file
            TestGenerationResult testResult = testGenerationService.generateTestsForFile(analysis);

            // Create summary
            CoverageSummary summary = createFileSummary(analysis, testResult);

            return CoverageResponse.success(summary);

        } catch (Exception e) {
            log.error("Failed to increase file coverage", e);
            throw new CoverageException("Failed to process file: " + e.getMessage(), e);
        }
    }

    public CoverageResponse increaseRepoCoverage(RepoCoverageRequest request) {/*
        log.info("Starting repository coverage increase for target: {}%", request.getTargetCoverageIncrease());

        try {
            // Initialize coverage analysis
            List<CoverageData> currentCoverage = coverageDataService.getCurrentCoverage(request.getRepoPath(),request.getBranch());

            // Calculate target coverage
            double targetCoverage = currentCoverage.getOverallCoverage() +
                    (request.getTargetCoverageIncrease() != null ? request.getTargetCoverageIncrease() : config.getDefaultCoverageIncrease());

            // Prioritize files for maximum impact
            List<FilePriority> prioritizedFiles = fileAnalysisService.prioritizeFiles(currentCoverage, targetCoverage);

            // Process files until target is reached
            ProcessingResult result = processFilesForCoverage(prioritizedFiles, targetCoverage, currentCoverage);

            // Generate final summary
            CoverageSummary summary = createRepoSummary(result, currentCoverage);

            return CoverageResponse.success(summary);

        } catch (Exception e) {
            log.error("Failed to increase repo coverage", e);
            throw new CoverageException("Failed to process repository: " + e.getMessage(), e);
        }*/
        return null;
    }

    public ApplyChangesResponse applyChanges(ApplyChangesRequest request) {
        log.info("Applying changes for session: {}", request.getSessionId());

        try {
            // Get workspace directory
            String workspaceDir = config.getWorkspaceRootDir();

            String repoUrlHash = generateRepoUrlHash(request.getRepositoryUrl());
            String branchName = request.getBranch() != null ? request.getBranch() : "main";
            String persistentDir = workspaceDir + "/" + repoUrlHash + "/" + branchName;
            String repoDir = persistentDir + "/" + extractRepoName(request.getRepositoryUrl());


            // Apply all generated test files
            gitService.applyChanges(request.getChanges(), workspaceDir);

            // Get original coverage for comparison
            CoverageData originalCoverage = coverageDataService.getCurrentCoverage(repoDir, request.getRepositoryUrl(), request.getBranch()).getCoverageDataList().get(0);

            // Detect project configuration
            ProjectConfiguration projectConfig = projectConfigService.detectProjectConfiguration(repoDir);

            // ENHANCED: Use new validation method with multiple strategies
            CoverageComparisonResult comparisonResult = jacocoService.validateCoverageImprovement(
                    repoDir, request.getBranch(),projectConfig, originalCoverage);

            CoverageData finalCoverage = comparisonResult.getNewCoverage();

            // Create PR if requested
            PullRequestResult prResult = null;
            if (request.isCreatePullRequest()) {
                prResult = gitService.createPullRequest(request.getSessionId(), finalCoverage, repoDir);
            }

            // Update coverage data in MongoDB
            //coverageDataService.saveCoverageData(finalCoverage);

            return ApplyChangesResponse.builder()
                    .success(true)
                    .pullRequest(prResult)
                    .finalCoverage(finalCoverage)
                    .coverageComparison(comparisonResult) // NEW: Include comparison details
                    .validationMethod(comparisonResult.getValidationMethod()) // NEW: Show how coverage was validated
                    .coverageImprovement(comparisonResult.getCoverageImprovement()) // NEW: Show actual improvement
                    .build();

        } catch (Exception e) {
            log.error("Failed to apply changes", e);
            // Rollback changes
            gitService.rollbackChanges(request.getSessionId());
            throw new CoverageException("Failed to apply changes: " + e.getMessage(), e);
        }
    }

    /**
     * Enhanced file coverage improvement with session management and batch processing
     */
    public FileCoverageImprovementResult improveFileCoverageEnhanced(com.org.devgenie.dto.coverage.EnhancedFileCoverageRequest request) {
        log.info("Starting enhanced file coverage improvement for: {}", request.getFilePath());

        // Create session for tracking progress
        CoverageImprovementSession session = sessionManagementService.createSession(
                request.getRepositoryUrl(),
                request.getBranch(),
                request.getFilePath(),
                CoverageImprovementSession.SessionType.FILE_IMPROVEMENT
        );

        String sessionId = session.getSessionId();
        LocalDateTime startTime = LocalDateTime.now();

        try {
            // Step 1: Initialize workspace and get repository (5% progress)
            sessionManagementService.updateProgress(sessionId, 5.0, "Setting up workspace");
            String repoDir = repositoryService.setupWorkspace(request.getRepositoryUrl(),
                    request.getBranch(),
                    request.getGithubToken());

            // Step 2: Get current coverage data (15% progress)
            sessionManagementService.updateProgress(sessionId, 15.0, "Analyzing current coverage");
            SonarQubeMetricsResponse currentCoverageResponse = coverageDataService.getCurrentCoverage(
                    repoDir, request.getRepositoryUrl(), request.getBranch());

            CoverageData fileCoverageData = findFileCoverageData(currentCoverageResponse.getCoverageDataList(),
                    request.getFilePath());

            // Step 3: Analyze file for test generation (25% progress)
            sessionManagementService.updateProgress(sessionId, 25.0, "Analyzing file structure");
            FileAnalysisResult analysis = fileAnalysisService.analyzeFile(request.getFilePath());

            // Step 4: Generate tests in batches (25% - 80% progress)
            sessionManagementService.updateProgress(sessionId, 30.0, "Generating tests in batches");
            List<FileCoverageImprovementResult.GeneratedTestInfo> allGeneratedTests = new ArrayList<>();
            List<GeneratedTestInfo> allGeneratedTestsWithCode = new ArrayList<>(); // Keep original objects with test code
            List<String> testFilePaths = new ArrayList<>();
            int totalBatches = calculateBatchCount(analysis, request.getMaxTestsPerBatch());

            for (int batchIndex = 0; batchIndex < totalBatches; batchIndex++) {
                double progressStart = 30.0 + (batchIndex * 50.0 / totalBatches);
                double progressEnd = 30.0 + ((batchIndex + 1) * 50.0 / totalBatches);

                sessionManagementService.updateProgress(sessionId, progressStart,
                        String.format("Processing batch %d of %d", batchIndex + 1, totalBatches));

                // Generate tests for this batch
                BatchTestGenerationResult batchResult = testGenerationService.generateTestsBatch(
                        analysis, batchIndex, request.getMaxTestsPerBatch());

                if (batchResult.getSuccess()) {
                    allGeneratedTests.addAll(convertToResultTestInfo(batchResult.getGeneratedTests()));
                    allGeneratedTestsWithCode.addAll(batchResult.getGeneratedTests()); // Keep originals with test code
                    testFilePaths.addAll(batchResult.getTestFilePaths());
                } else {
                    log.warn("Batch {} failed: {}", batchIndex + 1, batchResult.getError());
                }

                sessionManagementService.updateProgress(sessionId, progressEnd,
                        String.format("Completed batch %d", batchIndex + 1));
            }

            // Step 5: Write generated test files to disk (80% progress)
            sessionManagementService.updateProgress(sessionId, 80.0, "Writing test files to disk");
            writeGeneratedTestFiles(repoDir, allGeneratedTestsWithCode, testFilePaths);

            // Step 6: Validate generated tests (85% progress)
            TestValidationResult validationResult = null;
            if (request.getValidateTests()) {
                sessionManagementService.updateProgress(sessionId, 85.0, "Validating generated tests");
                validationResult = testGenerationService.validateGeneratedTests(repoDir, testFilePaths);
            }

            // Step 7: Calculate coverage improvement (95% progress)
            sessionManagementService.updateProgress(sessionId, 95.0, "Calculating coverage improvement");
            CoverageData estimatedCoverage = estimateFileCoverageImprovement(fileCoverageData, allGeneratedTests);

            // Step 8: Prepare results (100% progress)
            sessionManagementService.updateProgress(sessionId, 100.0, "Results ready for review");

            FileCoverageImprovementResult result = FileCoverageImprovementResult.builder()
                    .sessionId(sessionId)
                    .filePath(request.getFilePath())
                    .fileName(extractFileName(request.getFilePath()))
                    .packageName(analysis.getPackageName())
                    .originalCoverage(fileCoverageData.getLineCoverage())
                    .improvedCoverage(estimatedCoverage.getLineCoverage())
                    .coverageIncrease(estimatedCoverage.getLineCoverage() - fileCoverageData.getLineCoverage())
                    .beforeBreakdown(createCoverageBreakdown(fileCoverageData))
                    .afterBreakdown(createCoverageBreakdown(estimatedCoverage))
                    .generatedTests(allGeneratedTests)
                    .totalTestsGenerated(allGeneratedTests.size())
                    .testFilePaths(testFilePaths)
                    .startedAt(startTime)
                    .completedAt(LocalDateTime.now())
                    .processingTimeMs(java.time.Duration.between(startTime, LocalDateTime.now()).toMillis())
                    .batchesProcessed(totalBatches)
                    .retryCount(0)
                    .validationResult(validationResult)
                    .testsCompiled(validationResult != null ? validationResult.getSuccess() : null)
                    .testsExecuted(validationResult != null ? validationResult.getTestsExecuted() > 0 : null)
                    .status(FileCoverageImprovementResult.ProcessingStatus.COMPLETED)
                    .recommendations(generateRecommendations(analysis, allGeneratedTests))
                    .warnings(new ArrayList<>())
                    .errors(new ArrayList<>())
                    .build();

            sessionManagementService.setSessionResults(sessionId, result);
            sessionManagementService.updateSessionStatus(sessionId,
                    CoverageImprovementSession.SessionStatus.READY_FOR_REVIEW);

            return result;

        } catch (Exception e) {
            log.error("Failed to improve file coverage for: {}", request.getFilePath(), e);
            sessionManagementService.handleError(sessionId, e);
            throw new CoverageException("Failed to improve file coverage: " + e.getMessage(), e);
        }
    }

    // Helper methods for batch processing
    private int calculateBatchCount(FileAnalysisResult analysis, Integer maxTestsPerBatch) {
        int estimatedTestsNeeded = Math.max(analysis.getUncoveredMethods().size(),
                analysis.getComplexMethods().size());
        return Math.max(1, (int) Math.ceil((double) estimatedTestsNeeded / maxTestsPerBatch));
    }

    private CoverageData findFileCoverageData(List<CoverageData> coverageDataList, String filePath) {
        return coverageDataList.stream()
                .filter(data -> "FILE".equals(data.getType()) && filePath.equals(data.getPath()))
                .findFirst()
                .orElseThrow(() -> new CoverageException("No coverage data found for file: " + filePath));
    }

    private CoverageData estimateFileCoverageImprovement(CoverageData original, List<FileCoverageImprovementResult.GeneratedTestInfo> tests) {
        // Simple estimation - in reality would be more sophisticated
        double improvement = tests.stream()
                .mapToDouble(test -> test.getEstimatedCoverageContribution())
                .sum();

        return CoverageData.builder()
                .repoPath(original.getRepoPath())
                .path(original.getPath())
                .lineCoverage(Math.min(100.0, original.getLineCoverage() + improvement))
                .branchCoverage(Math.min(100.0, original.getBranchCoverage() + improvement * 0.8))
                .methodCoverage(Math.min(100.0, original.getMethodCoverage() + improvement * 0.9))
                .totalLines(original.getTotalLines())
                .coveredLines((int) (original.getTotalLines() * (original.getLineCoverage() + improvement) / 100))
                .build();
    }

    private FileCoverageImprovementResult.CoverageBreakdown createCoverageBreakdown(CoverageData data) {
        return FileCoverageImprovementResult.CoverageBreakdown.builder()
                .lineCoverage(data.getLineCoverage())
                .branchCoverage(data.getBranchCoverage())
                .methodCoverage(data.getMethodCoverage())
                .totalLines(data.getTotalLines())
                .coveredLines(data.getCoveredLines())
                .totalBranches(data.getTotalBranches())
                .coveredBranches(data.getCoveredBranches())
                .totalMethods(data.getTotalMethods())
                .coveredMethods(data.getCoveredMethods())
                .build();
    }

    private String extractFileName(String filePath) {
        return filePath.substring(filePath.lastIndexOf('/') + 1);
    }

    private List<String> generateRecommendations(FileAnalysisResult analysis, List<FileCoverageImprovementResult.GeneratedTestInfo> tests) {
        List<String> recommendations = new ArrayList<>();

        if (tests.size() < analysis.getUncoveredMethods().size()) {
            recommendations.add("Consider generating additional tests for remaining uncovered methods");
        }

        if (analysis.getComplexityScore() > 5.0) {
            recommendations.add("High complexity file - focus on edge cases and error conditions");
        }

        recommendations.add("Review generated tests and add additional assertions as needed");
        recommendations.add("Consider integration tests for this component");

        return recommendations;
    }

    private ProcessingResult processFilesForCoverage(List<FilePriority> files, double targetCoverage, CoverageData currentCoverage) {
        ProcessingResult result = new ProcessingResult();
        double currentOverallCoverage = currentCoverage.getOverallCoverage();

        for (FilePriority filePriority : files) {
            if (currentOverallCoverage >= targetCoverage) {
                log.info("Target coverage reached: {}%", currentOverallCoverage);
                break;
            }

            try {
                // Analyze file
                FileAnalysisResult analysis = fileAnalysisService.analyzeFile(filePriority.getFilePath());

                // Generate tests
                TestGenerationResult testResult = testGenerationService.generateTestsForFile(analysis);

                if (testResult.isSuccess()) {
                    result.addSuccessfulFile(filePriority.getFilePath(), testResult);
                    // Estimate coverage improvement (this would be more accurate with actual execution)
                    currentOverallCoverage += estimateCoverageImprovement(testResult);
                } else {
                    result.addFailedFile(filePriority.getFilePath(), testResult.getError());
                }

            } catch (Exception e) {
                log.warn("Failed to process file: {}", filePriority.getFilePath(), e);
                result.addFailedFile(filePriority.getFilePath(), e.getMessage());
            }
        }

        return result;
    }

    private double estimateCoverageImprovement(TestGenerationResult testResult) {
        // Simple estimation - in reality this would be more sophisticated
        return testResult.getGeneratedTests().size() * 0.5; // Rough estimate
    }

    private CoverageSummary createFileSummary(FileAnalysisResult analysis, TestGenerationResult testResult) {
        return CoverageSummary.builder()
                .sessionId(UUID.randomUUID().toString())
                .type(CoverageSummary.Type.FILE)
                .filePath(analysis.getFilePath())
                .originalCoverage(analysis.getCurrentCoverage())
                .estimatedCoverage(analysis.getCurrentCoverage() + estimateCoverageImprovement(testResult))
                .testsAdded(testResult.getGeneratedTests().size())
                .changes(List.of(createFileChange(analysis, testResult)))
                .build();
    }

    private CoverageSummary createRepoSummary(ProcessingResult result, CoverageData originalCoverage) {
        return CoverageSummary.builder()
                .sessionId(UUID.randomUUID().toString())
                .type(CoverageSummary.Type.REPOSITORY)
                .originalCoverage(originalCoverage.getOverallCoverage())
                .estimatedCoverage(calculateEstimatedCoverage(result, originalCoverage))
                .testsAdded(result.getTotalTestsAdded())
                .filesProcessed(result.getSuccessfulFiles().size())
                .failedFiles(result.getFailedFiles().size())
                .changes(result.getAllChanges())
                .build();
    }

    private double calculateEstimatedCoverage(ProcessingResult result, CoverageData originalCoverage) {
        // Calculate estimated improvement based on generated tests
        double improvement = result.getSuccessfulFiles().values().stream()
                .mapToDouble(this::estimateCoverageImprovement)
                .sum();
        return originalCoverage.getOverallCoverage() + improvement;
    }

    private FileChange createFileChange(FileAnalysisResult analysis, TestGenerationResult testResult) {
        return FileChange.builder()
                .filePath(analysis.getFilePath())
                .testFilePath(testResult.getTestFilePath())
                .changeType(FileChange.Type.TEST_ADDED)
                .description("Added " + testResult.getGeneratedTests().size() + " test methods")
                .content(testResult.getGeneratedTestContent())
                .build();
    }

    private String extractRepoName(String repositoryUrl) {
        // Extract repository name from URL
        String[] parts = repositoryUrl.split("/");
        String repoName = parts[parts.length - 1];
        return repoName.endsWith(".git") ? repoName.substring(0, repoName.length() - 4) : repoName;
    }
    /**
     * Generate a hash for repository URL to create persistent directory names
     */
    private String generateRepoUrlHash(String repositoryUrl) {
        // Remove protocol and special characters, create a safe directory name
        String cleaned = repositoryUrl
                .replaceAll("https?://", "")
                .replaceAll("[^a-zA-Z0-9.-]", "_")
                .toLowerCase();
        return cleaned.length() > 50 ? cleaned.substring(0, 50) : cleaned;
    }

    // Helper method to convert between GeneratedTestInfo types
    private List<FileCoverageImprovementResult.GeneratedTestInfo> convertToResultTestInfo(List<GeneratedTestInfo> tests) {
        return tests.stream()
                .map(test -> FileCoverageImprovementResult.GeneratedTestInfo.builder()
                        .testMethodName(test.getTestMethodName())
                        .testClass(test.getTestClass())
                        .description(test.getDescription())
                        .coveredMethods(test.getCoveredMethods())
                        .estimatedCoverageContribution(test.getEstimatedCoverageContribution())
                        .build())
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Write generated test files to disk with proper structure
     */
    private void writeGeneratedTestFiles(String repoDir, List<GeneratedTestInfo> generatedTests, List<String> testFilePaths) throws IOException {
        log.info("=== WRITING GENERATED TEST FILES ===");
        log.info("Repository directory: {}", repoDir);
        log.info("Number of test files to create: {}", testFilePaths.size());
        log.info("Number of generated tests: {}", generatedTests.size());
        
        for (int i = 0; i < testFilePaths.size() && i < generatedTests.size(); i++) {
            String testFilePath = testFilePaths.get(i);
            String absoluteTestPath = Paths.get(repoDir, testFilePath).toString();
            
            log.info("=== CREATING TEST FILE {} ===", i + 1);
            log.info("Relative test path: {}", testFilePath);
            log.info("Absolute test path: {}", absoluteTestPath);
            
            // Generate complete test class content
            String testContent = generateCompleteTestClass(generatedTests, testFilePath);
            log.info("Generated test content length: {} characters", testContent.length());
            log.info("Generated test content preview: {}", testContent.substring(0, Math.min(200, testContent.length())));
            
            // Create file change for GitService
            FileChange testFileChange = FileChange.builder()
                    .changeType(FileChange.Type.TEST_ADDED)
                    .testFilePath(absoluteTestPath)
                    .content(testContent)
                    .description("Generated test file with AI-generated test methods")
                    .build();
            
            log.info("FileChange created: {}", testFileChange.getDescription());
            
            // Write the test file using GitService
            try {
                log.info("Calling GitService.applyChanges for: {}", absoluteTestPath);
                gitService.applyChanges(List.of(testFileChange), repoDir);
                log.info("✅ SUCCESS: Test file created successfully: {}", absoluteTestPath);
                
                // Verify file exists
                Path createdFile = Paths.get(absoluteTestPath);
                if (Files.exists(createdFile)) {
                    long fileSize = Files.size(createdFile);
                    log.info("✅ VERIFIED: File exists with size: {} bytes", fileSize);
                } else {
                    log.error("❌ ERROR: File was not created: {}", absoluteTestPath);
                }
            } catch (Exception e) {
                log.error("❌ FAILED to write test file: {}", absoluteTestPath, e);
                throw new IOException("Failed to write test file: " + absoluteTestPath, e);
            }
        }
        
        log.info("=== TEST FILE WRITING COMPLETED ===");
    }

    /**
     * Generate complete test class content from generated test methods
     */
    private String generateCompleteTestClass(List<GeneratedTestInfo> generatedTests, String testFilePath) {
        if (generatedTests.isEmpty()) {
            throw new IllegalArgumentException("No test methods to generate class content");
        }
        
        // Extract package and class information from the first test
        String packageName = extractPackageFromTestPath(testFilePath);
        String className = extractClassNameFromTestPath(testFilePath);
        String sourceClassName = className.replace("Test", "");
        
        log.info("Generating test class: {} in package: {} for source class: {}", className, packageName, sourceClassName);
        
        StringBuilder testClass = new StringBuilder();
        
        // Add package declaration
        if (packageName != null && !packageName.isEmpty()) {
            testClass.append("package ").append(packageName).append(";\n\n");
        }
        
        // Add imports
        testClass.append("import org.junit.jupiter.api.Test;\n");
        testClass.append("import org.junit.jupiter.api.BeforeEach;\n");
        testClass.append("import org.junit.jupiter.api.DisplayName;\n");
        testClass.append("import org.mockito.Mock;\n");
        testClass.append("import org.mockito.MockitoAnnotations;\n");
        testClass.append("import static org.junit.jupiter.api.Assertions.*;\n");
        testClass.append("import static org.mockito.Mockito.*;\n\n");
        
        // Add class declaration
        testClass.append("/**\n");
        testClass.append(" * Generated test class for ").append(sourceClassName).append("\n");
        testClass.append(" * Auto-generated by DevGenie Coverage Improvement System\n");
        testClass.append(" */\n");
        testClass.append("class ").append(className).append(" {\n\n");
        
        // Add setup method
        testClass.append("    @BeforeEach\n");
        testClass.append("    void setUp() {\n");
        testClass.append("        MockitoAnnotations.openMocks(this);\n");
        testClass.append("    }\n\n");
        
        // Add generated test methods
        for (GeneratedTestInfo testInfo : generatedTests) {
            testClass.append("    @Test\n");
            testClass.append("    @DisplayName(\"").append(testInfo.getDescription()).append("\")\n");
            testClass.append("    ").append(testInfo.getTestCode());
            if (!testInfo.getTestCode().endsWith("\n")) {
                testClass.append("\n");
            }
            testClass.append("\n");
        }
        
        // Close class
        testClass.append("}\n");
        
        String result = testClass.toString();
        log.debug("Generated test class content for {}: {} characters", className, result.length());
        return result;
    }

    /**
     * Extract package name from test file path
     */
    private String extractPackageFromTestPath(String testFilePath) {
        // Extract package from path like: src/test/java/com/org/devgenie/TestClass.java
        String path = testFilePath.replace("\\", "/");
        if (path.contains("/src/test/java/")) {
            String packagePath = path.substring(path.indexOf("/src/test/java/") + "/src/test/java/".length());
            packagePath = packagePath.substring(0, packagePath.lastIndexOf("/"));
            return packagePath.replace("/", ".");
        }
        return "";
    }

    /**
     * Extract class name from test file path
     */
    private String extractClassNameFromTestPath(String testFilePath) {
        String fileName = Paths.get(testFilePath).getFileName().toString();
        return fileName.replace(".java", "");
    }
}
