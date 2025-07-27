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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

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
            // Retrieve session data to get repository URL and branch
            Optional<CoverageImprovementSession> sessionOpt = sessionManagementService.getSession(request.getSessionId());
            if (sessionOpt.isEmpty()) {
                throw new IllegalArgumentException("Session not found: " + request.getSessionId());
            }

            CoverageImprovementSession session = sessionOpt.get();
            String repositoryUrl = session.getRepositoryUrl();
            String branch = session.getBranch() != null ? session.getBranch() : "main";

            if (repositoryUrl == null) {
                throw new IllegalArgumentException("Repository URL not found in session: " + request.getSessionId());
            }

            // Get workspace directory
            String workspaceDir = config.getWorkspaceRootDir();

            String repoUrlHash = generateRepoUrlHash(repositoryUrl);
            String branchName = branch;
            String persistentDir = workspaceDir + "/" + repoUrlHash + "/" + branchName;
            String repoDir = persistentDir + "/" + extractRepoName(repositoryUrl);


            // Apply all generated test files
            //gitService.applyChanges(request.getChanges(), workspaceDir);

            // Get original coverage for comparison
            CoverageData originalCoverage = coverageDataService.getCurrentCoverage(repoDir, repositoryUrl, branch).getCoverageDataList().get(0);

            // Detect project configuration
            ProjectConfiguration projectConfig = projectConfigService.detectProjectConfiguration(repoDir);

            // ENHANCED: Use new validation method with multiple strategies
            CoverageComparisonResult comparisonResult = jacocoService.validateCoverageImprovement(
                    repoDir, branch, projectConfig, originalCoverage);

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
        log.info("🔍 CoverageAgentService received sessionId: {}", request.getSessionId());

        // Use existing session ID if provided, otherwise create a new session
        String sessionId;
        if (request.getSessionId() != null && !request.getSessionId().trim().isEmpty()) {
            sessionId = request.getSessionId();
            log.info("🔍 Using provided session ID: {}", sessionId);
        } else {
            // Create session for tracking progress (fallback for backward compatibility)
            CoverageImprovementSession session = sessionManagementService.createSession(
                    request.getRepositoryUrl(),
                    request.getBranch(),
                    request.getFilePath(),
                    CoverageImprovementSession.SessionType.FILE_IMPROVEMENT
            );
            sessionId = session.getSessionId();
            log.info("🔍 Created new session ID: {}", sessionId);
        }
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
            FileAnalysisResult analysis = fileAnalysisService.analyzeFile(repoDir+"/"+request.getFilePath());

            // Step 4: Generate tests using intelligent strategy selection (25% - 80% progress)
            sessionManagementService.updateProgress(sessionId, 30.0, "Determining optimal test generation strategy");
            
            // Read source file content for strategy analysis
            String sourceFilePath = repoDir + "/" + request.getFilePath();
            String sourceContent = "";
            try {
                if (Files.exists(Paths.get(sourceFilePath))) {
                    sourceContent = Files.readString(Paths.get(sourceFilePath));
                }
            } catch (Exception e) {
                log.warn("Could not read source file for strategy analysis: {}", e.getMessage());
            }
            
            // Determine if test file already exists
            String testFilePath = generateTestFilePath(sourceFilePath);
            boolean existingTestFile = Files.exists(Paths.get(repoDir, testFilePath));
            
            // Use TestGenerationStrategy to determine optimal approach
            TestGenerationStrategy strategy = TestGenerationStrategy.determine(analysis, existingTestFile, sourceContent);
            log.info("🎯 Selected test generation strategy: {} - {}", strategy.getStrategy(), strategy.getReasoning());
            
            sessionManagementService.updateProgress(sessionId, 35.0, 
                "Using " + strategy.getStrategy() + " strategy: " + strategy.getReasoning());
            
            List<FileCoverageImprovementResult.GeneratedTestInfo> allGeneratedTests = new ArrayList<>();
            List<GeneratedTestInfo> allGeneratedTestsWithCode = new ArrayList<>();
            List<String> testFilePaths = new ArrayList<>();
            TestGenerationResult directTestResult = null; // Store for DIRECT_FULL_FILE strategy
            
            // Execute strategy-based test generation
            switch (strategy.getStrategy()) {
                case DIRECT_FULL_FILE:
                    sessionManagementService.updateProgress(sessionId, 40.0, "Generating complete test file directly");
                    directTestResult = testGenerationService.generateTestsForFileWithStrategy(analysis, strategy);
                    if (directTestResult.isSuccess()) {
                        allGeneratedTests.addAll(convertDirectResultToResultTestInfo(directTestResult));
                        // For DIRECT_FULL_FILE, we'll use the complete test content directly, not convert to individual methods
                        testFilePaths.add(directTestResult.getTestFilePath());
                    }
                    sessionManagementService.updateProgress(sessionId, 80.0, "Direct generation completed");
                    break;
                    
                case BATCH_METHOD_BASED:
                    sessionManagementService.updateProgress(sessionId, 40.0, "Generating tests in optimized batches");
                    int totalBatches = Math.max(1, (int) Math.ceil((double) analysis.getUncoveredMethods().size() / strategy.getMaxTestsPerBatch()));
                    
                    for (int batchIndex = 0; batchIndex < totalBatches; batchIndex++) {
                        double progressStart = 40.0 + (batchIndex * 40.0 / totalBatches);
                        double progressEnd = 40.0 + ((batchIndex + 1) * 40.0 / totalBatches);

                        sessionManagementService.updateProgress(sessionId, progressStart,
                                String.format("Processing batch %d of %d", batchIndex + 1, totalBatches));

                        BatchTestGenerationResult batchResult = testGenerationService.generateTestsBatch(
                                analysis, batchIndex, strategy.getMaxTestsPerBatch());

                        if (batchResult.getSuccess()) {
                            allGeneratedTests.addAll(convertToResultTestInfo(batchResult.getGeneratedTests()));
                            allGeneratedTestsWithCode.addAll(batchResult.getGeneratedTests());
                            testFilePaths.addAll(batchResult.getTestFilePaths());
                        } else {
                            log.warn("Batch {} failed: {}", batchIndex + 1, batchResult.getError());
                        }

                        sessionManagementService.updateProgress(sessionId, progressEnd,
                                String.format("Completed batch %d", batchIndex + 1));
                    }
                    break;
                    
                case MERGE_WITH_EXISTING:
                    sessionManagementService.updateProgress(sessionId, 40.0, "Generating tests to merge with existing file");
                    int mergeBatches = Math.max(1, strategy.getMaxTestsPerBatch() / 2); // Fewer batches for merging
                    
                    for (int batchIndex = 0; batchIndex < mergeBatches; batchIndex++) {
                        double progressStart = 40.0 + (batchIndex * 40.0 / mergeBatches);
                        double progressEnd = 40.0 + ((batchIndex + 1) * 40.0 / mergeBatches);

                        sessionManagementService.updateProgress(sessionId, progressStart,
                                String.format("Generating merge batch %d of %d", batchIndex + 1, mergeBatches));

                        BatchTestGenerationResult batchResult = testGenerationService.generateTestsBatch(
                                analysis, batchIndex, strategy.getMaxTestsPerBatch());

                        if (batchResult.getSuccess()) {
                            allGeneratedTests.addAll(convertToResultTestInfo(batchResult.getGeneratedTests()));
                            allGeneratedTestsWithCode.addAll(batchResult.getGeneratedTests());
                            testFilePaths.addAll(batchResult.getTestFilePaths());
                        }

                        sessionManagementService.updateProgress(sessionId, progressEnd,
                                String.format("Completed merge batch %d", batchIndex + 1));
                    }
                    break;
            }

            // Step 5: Write generated test files using strategy-aware approach (80% progress)
            sessionManagementService.updateProgress(sessionId, 80.0, "Writing test files using " + strategy.getStrategy() + " approach");
            writeGeneratedTestFilesWithStrategy(repoDir, allGeneratedTestsWithCode, testFilePaths, strategy, sourceContent, directTestResult);
            
            // Step 5.1: Ensure files are written and flushed to disk (85% progress)
            sessionManagementService.updateProgress(sessionId, 85.0, "Finalizing test file creation");
            
            // Verify files were actually written
            int filesCreated = 0;
            for (String testPath : testFilePaths) {
                String absoluteTestPath = Paths.get(repoDir, testPath).toString();
                if (!Files.exists(Paths.get(absoluteTestPath))) {
                    log.warn("Test file was not created successfully: {}", absoluteTestPath);
                } else {
                    filesCreated++;
                    log.info("Test file verified successfully created: {}", absoluteTestPath);
                }
            }
            
            log.info("File verification complete: {}/{} test files created successfully", filesCreated, testFilePaths.size());

            // Step 6: Validate generated tests (90% progress)
            TestValidationResult validationResult = null;
            if (request.getValidateTests()) {
                sessionManagementService.updateProgress(sessionId, 90.0, "Validating generated tests");
                validationResult = testGenerationService.validateGeneratedTests(repoDir, testFilePaths);
            }

            // Step 7: Calculate coverage improvement (95% progress)
            sessionManagementService.updateProgress(sessionId, 95.0, "Calculating coverage improvement");
            CoverageData estimatedCoverage = estimateFileCoverageImprovement(fileCoverageData, allGeneratedTests);

            // Step 8: Prepare and store results (98% progress)
            sessionManagementService.updateProgress(sessionId, 98.0, "Preparing final results");
            
            // Calculate total batches processed based on strategy used
            int totalBatchesProcessed = switch (strategy.getStrategy()) {
                case DIRECT_FULL_FILE -> 1; // Direct generation is like 1 batch
                case BATCH_METHOD_BASED -> Math.max(1, (int) Math.ceil((double) analysis.getUncoveredMethods().size() / strategy.getMaxTestsPerBatch()));
                case MERGE_WITH_EXISTING -> Math.max(1, strategy.getMaxTestsPerBatch() / 2);
            };

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
                    .batchesProcessed(totalBatchesProcessed)
                    .retryCount(0)
                    .validationResult(validationResult)
                    .testsCompiled(validationResult != null ? validationResult.getSuccess() : null)
                    .testsExecuted(validationResult != null ? validationResult.getTestsExecuted() > 0 : null)
                    .status(FileCoverageImprovementResult.ProcessingStatus.COMPLETED)
                    .recommendations(generateRecommendations(analysis, allGeneratedTests, strategy))
                    .warnings(new ArrayList<>())
                    .errors(new ArrayList<>())
                    .build();

            sessionManagementService.setSessionResults(sessionId, result);
            sessionManagementService.updateSessionStatus(sessionId,
                    CoverageImprovementSession.SessionStatus.READY_FOR_REVIEW);
            
            // Step 9: Final completion update (100% progress) - only after all data is persisted
            sessionManagementService.updateProgress(sessionId, 100.0, "File improvement complete! Results are ready for review.");

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

    private List<String> generateRecommendations(FileAnalysisResult analysis, List<FileCoverageImprovementResult.GeneratedTestInfo> tests, TestGenerationStrategy strategy) {
        List<String> recommendations = new ArrayList<>();

        // Strategy-specific recommendations
        switch (strategy.getStrategy()) {
            case DIRECT_FULL_FILE:
                recommendations.add("✅ Used direct generation strategy - tests generated as complete file");
                recommendations.add("💡 Review the generated test class for completeness and add assertions as needed");
                break;
            case BATCH_METHOD_BASED:
                recommendations.add("⚡ Used batch generation strategy for optimal handling of complex class");
                recommendations.add("🔍 Consider reviewing each batch of generated tests for consistency");
                break;
            case MERGE_WITH_EXISTING:
                recommendations.add("🔄 Merged new tests with existing test file - review for conflicts");
                recommendations.add("🧪 Verify that new tests don't duplicate existing functionality");
                break;
        }

        if (tests.size() < analysis.getUncoveredMethods().size()) {
            recommendations.add("📈 Consider generating additional tests for remaining uncovered methods");
        }

        if (analysis.getComplexityScore() > 5.0) {
            recommendations.add("🔥 High complexity file - focus on edge cases and error conditions");
        }

        recommendations.add("🎯 Strategy used: " + strategy.getReasoning());
        recommendations.add("📝 Review generated tests and add additional assertions as needed");
        recommendations.add("🔗 Consider integration tests for this component");

        return recommendations;
    }
    
    private List<String> generateRecommendations(FileAnalysisResult analysis, List<FileCoverageImprovementResult.GeneratedTestInfo> tests) {
        // Fallback method for backward compatibility
        return generateRecommendations(analysis, tests, null);
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
     * Enhanced to handle existing test files intelligently with hybrid approach support
     */
    private void writeGeneratedTestFiles(String repoDir, List<GeneratedTestInfo> generatedTests, List<String> testFilePaths) throws IOException {
        log.info("=== WRITING GENERATED TEST FILES USING HYBRID APPROACH ===");
        log.info("Repository directory: {}", repoDir);
        log.info("Number of test files to create: {}", testFilePaths.size());
        log.info("Number of generated tests: {}", generatedTests.size());
        
        for (int i = 0; i < testFilePaths.size() && i < generatedTests.size(); i++) {
            String testFilePath = testFilePaths.get(i);
            String absoluteTestPath = Paths.get(repoDir, testFilePath).toString();
            
            log.info("=== PROCESSING TEST FILE {} ===", i + 1);
            log.info("Relative test path: {}", testFilePath);
            log.info("Absolute test path: {}", absoluteTestPath);
            
            // Check if test file already exists
            Path testFile = Paths.get(absoluteTestPath);
            boolean testFileExists = Files.exists(testFile);
            
            log.info("Test file exists: {}", testFileExists);
            
            String testContent;
            FileChange.Type changeType;
            
            if (testFileExists) {
                // Handle existing test file - merge new tests
                log.info("Existing test file detected. Using MERGE_WITH_EXISTING strategy...");
                testContent = mergeWithExistingTestFile(absoluteTestPath, generatedTests);
                changeType = FileChange.Type.TEST_MODIFIED;
            } else {
                // Create new complete test class using intelligent strategy selection
                log.info("Creating new test file using optimal generation strategy...");
                testContent = generateOptimalTestClass(generatedTests, testFilePath, repoDir);
                changeType = FileChange.Type.TEST_ADDED;
            }
            
            log.info("Generated test content length: {} characters", testContent.length());
            log.info("Generated test content preview: {}", testContent.substring(0, Math.min(200, testContent.length())));
            
            // Validate generated content
            if (!validateTestContent(testContent)) {
                log.warn("Generated test content failed validation, applying fixes...");
                testContent = fixTestContent(testContent, testFilePath);
            }
            
            // Create file change for GitService
            FileChange testFileChange = FileChange.builder()
                    .changeType(changeType)
                    .testFilePath(absoluteTestPath)
                    .content(testContent)
                    .description(testFileExists ? 
                        "Merged new AI-generated test methods with existing test class using hybrid approach" : 
                        "Generated test file with AI-generated test methods using hybrid approach")
                    .build();
            
            log.info("FileChange created: {}", testFileChange.getDescription());
            
            // Write the test file using GitService
            try {
                log.info("Calling GitService.applyChanges for: {}", absoluteTestPath);
                gitService.applyChanges(List.of(testFileChange), repoDir);
                log.info("✅ SUCCESS: Test file {} successfully: {}", 
                    testFileExists ? "updated" : "created", absoluteTestPath);
                
                // Verify file exists and validate content
                Path createdFile = Paths.get(absoluteTestPath);
                if (Files.exists(createdFile)) {
                    long fileSize = Files.size(createdFile);
                    log.info("✅ VERIFIED: File exists with size: {} bytes", fileSize);
                    
                    // Additional validation
                    if (fileSize < 100) {
                        log.warn("⚠️  WARNING: Generated test file is very small, may be incomplete");
                    }
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
     * Generate optimal test class based on content analysis and strategy selection
     */
    private String generateOptimalTestClass(List<GeneratedTestInfo> generatedTests, String testFilePath, String repoDir) {
        try {
            // Analyze the first test to understand the source class
            if (generatedTests.isEmpty()) {
                throw new IllegalArgumentException("No test methods to generate class content");
            }
            
            String sourceFilePath = testFilePath.replace("src/test/java", "src/main/java")
                                              .replace("Test.java", ".java");
            String fullSourcePath = Paths.get(repoDir, sourceFilePath).toString();
            
            // Check if we can read the source file for better analysis
            String sourceContent = "";
            boolean hasSourceContent = false;
            try {
                if (Files.exists(Paths.get(fullSourcePath))) {
                    sourceContent = Files.readString(Paths.get(fullSourcePath));
                    hasSourceContent = true;
                }
            } catch (Exception e) {
                log.debug("Could not read source file for analysis: {}", e.getMessage());
            }
            
            // Determine optimal approach based on source analysis
            if (hasSourceContent && shouldUseDirectGeneration(sourceContent, generatedTests)) {
                log.info("Using direct generation approach based on source analysis");
                return generateDirectStyleTestClass(generatedTests, testFilePath, sourceContent);
            } else {
                log.info("Using enhanced batch assembly approach");
                return generateCompleteTestClass(generatedTests, testFilePath);
            }
            
        } catch (Exception e) {
            log.warn("Failed optimal generation, falling back to standard approach", e);
            return generateCompleteTestClass(generatedTests, testFilePath);
        }
    }
    
    /**
     * Determine if direct generation approach should be used
     * Enhanced for all types of Java applications
     */
    private boolean shouldUseDirectGeneration(String sourceContent, List<GeneratedTestInfo> generatedTests) {
        // Analyze class characteristics
        int sourceLines = sourceContent.split("\n").length;
        boolean fewTests = generatedTests.size() <= 3;
        
        // 1. Main application classes (any framework)
        if (sourceContent.contains("public static void main")) {
            return true; // Always use direct for main classes
        }
        
        // 2. Utility classes (static methods)
        if (isUtilityClass(sourceContent)) {
            return true;
        }
        
        // 3. Data classes (POJOs, entities)
        if (isDataClass(sourceContent)) {
            return true;
        }
        
        // 4. Interfaces and abstract classes
        if (sourceContent.contains("public interface") || sourceContent.contains("interface") ||
            sourceContent.contains("abstract class")) {
            return true;
        }
        
        // 5. Enums
        if (sourceContent.contains("public enum") || sourceContent.contains("enum")) {
            return true;
        }
        
        // 6. Small classes regardless of framework
        if (sourceLines < 100 && fewTests) {
            return true;
        }
        
        // 7. Configuration classes (any type)
        if (sourceContent.contains("Config") && sourceContent.contains("class") && sourceLines < 150) {
            return true;
        }
        
        // 8. Simple classes with few methods
        int methodCount = countMethods(sourceContent);
        if (methodCount <= 5 && sourceLines < 120) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Check if class is a utility class (mainly static methods, any framework)
     */
    private boolean isUtilityClass(String sourceContent) {
        return sourceContent.contains("public static") && 
               sourceContent.contains("private") &&
               !sourceContent.contains("@Component") && 
               !sourceContent.contains("@Service") &&
               !sourceContent.contains("@Controller");
    }
    
    /**
     * Check if class is a data class (POJO, entity, DTO, etc.)
     */
    private boolean isDataClass(String sourceContent) {
        // Check for common data class indicators
        boolean hasDataAnnotations = sourceContent.contains("@Entity") || 
                                    sourceContent.contains("@Data") || 
                                    sourceContent.contains("@DTO") ||
                                    sourceContent.contains("@Model");
        
        if (hasDataAnnotations) {
            return true;
        }
        
        // Count getters/setters vs total methods
        int getterSetterCount = 0;
        int totalMethods = 0;
        
        String[] lines = sourceContent.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if ((trimmed.contains("public ") || trimmed.contains("private ")) && 
                trimmed.contains("(") && trimmed.contains(")") && 
                !trimmed.contains("class ")) {
                totalMethods++;
                if (trimmed.contains("get") || trimmed.contains("set") || 
                    (trimmed.contains("is") && trimmed.contains("()"))) {
                    getterSetterCount++;
                }
            }
        }
        
        return totalMethods > 2 && (getterSetterCount >= totalMethods * 0.6);
    }
    
    /**
     * Count methods in the class
     */
    private int countMethods(String sourceContent) {
        int count = 0;
        String[] lines = sourceContent.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if ((trimmed.contains("public ") || trimmed.contains("private ") || trimmed.contains("protected ")) &&
                trimmed.contains("(") && trimmed.contains(")") && 
                !trimmed.contains("class ") && !trimmed.contains("interface ") && 
                !trimmed.contains("=") && !trimmed.contains("//")) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * Generate test class using direct style (as if from AI full-file generation)
     */
    private String generateDirectStyleTestClass(List<GeneratedTestInfo> generatedTests, String testFilePath, String sourceContent) {
        String packageName = extractPackageFromTestPath(testFilePath);
        String className = extractClassNameFromTestPath(testFilePath);
        String sourceClassName = className.replace("Test", "");
        
        boolean isSpringApp = sourceContent.contains("@SpringBootApplication");
        
        StringBuilder testClass = new StringBuilder();
        
        // Add package declaration
        if (packageName != null && !packageName.isEmpty()) {
            testClass.append("package ").append(packageName).append(";\n\n");
        }
        
        // Add imports - minimal for Spring Boot apps, comprehensive for others
        if (isSpringApp) {
            testClass.append("import org.junit.jupiter.api.Test;\n");
            testClass.append("import org.springframework.boot.test.context.SpringBootTest;\n");
            testClass.append("import static org.junit.jupiter.api.Assertions.*;\n\n");
        } else {
            testClass.append("import org.junit.jupiter.api.Test;\n");
            testClass.append("import org.junit.jupiter.api.BeforeEach;\n");
            testClass.append("import org.junit.jupiter.api.DisplayName;\n");
            testClass.append("import org.junit.jupiter.api.extension.ExtendWith;\n");
            testClass.append("import org.mockito.junit.jupiter.MockitoExtension;\n");
            testClass.append("import static org.junit.jupiter.api.Assertions.*;\n");
            testClass.append("import static org.mockito.Mockito.*;\n\n");
        }
        
        // Add class declaration
        testClass.append("/**\n");
        testClass.append(" * Test class for ").append(sourceClassName).append("\n");
        testClass.append(" * Generated using DevGenie's hybrid test generation approach\n");
        if (isSpringApp) {
            testClass.append(" * Spring Boot Application class - focused on essential functionality\n");
        }
        testClass.append(" */\n");
        
        if (isSpringApp) {
            testClass.append("@SpringBootTest\n");
        } else {
            testClass.append("@ExtendWith(MockitoExtension.class)\n");
        }
        
        testClass.append("class ").append(className).append(" {\n\n");
        
        // Add setup method only if not a Spring Boot app
        if (!isSpringApp) {
            testClass.append("    @BeforeEach\n");
            testClass.append("    void setUp() {\n");
            testClass.append("        // Initialize test fixtures\n");
            testClass.append("    }\n\n");
        }
        
        // Add test methods
        for (GeneratedTestInfo testInfo : generatedTests) {
            String testCode = testInfo.getTestCode();
            testCode = cleanTestMethodCode(testCode);
            
            if (!testCode.startsWith("    ")) {
                testCode = indentCode(testCode, "    ");
            }
            
            testClass.append("    @Test\n");
            testClass.append("    @DisplayName(\"").append(testInfo.getDescription()).append("\")\n");
            testClass.append("    ").append(testCode);
            if (!testCode.endsWith("\n")) {
                testClass.append("\n");
            }
            testClass.append("\n");
        }
        
        // Close class
        testClass.append("}\n");
        
        return testClass.toString();
    }
    
    /**
     * Validate test content for basic correctness
     */
    private boolean validateTestContent(String testContent) {
        if (testContent == null || testContent.trim().isEmpty()) {
            return false;
        }
        
        // Check for essential elements
        boolean hasPackage = testContent.contains("package ");
        boolean hasImports = testContent.contains("import ");
        boolean hasClass = testContent.contains("class ");
        boolean hasTests = testContent.contains("@Test");
        
        // Check brace balance
        int openBraces = testContent.length() - testContent.replace("{", "").length();
        int closeBraces = testContent.length() - testContent.replace("}", "").length();
        boolean balancedBraces = openBraces == closeBraces;
        
        return hasPackage && hasImports && hasClass && hasTests && balancedBraces;
    }
    
    /**
     * Fix common issues in test content
     */
    private String fixTestContent(String testContent, String testFilePath) {
        if (testContent == null || testContent.trim().isEmpty()) {
            log.warn("Test content is empty, generating minimal placeholder");
            return generateMinimalTestClass(testFilePath);
        }
        
        // Fix missing package declaration
        if (!testContent.contains("package ")) {
            String packageName = extractPackageFromTestPath(testFilePath);
            if (packageName != null && !packageName.isEmpty()) {
                testContent = "package " + packageName + ";\n\n" + testContent;
            }
        }
        
        // Fix missing basic imports
        if (!testContent.contains("import org.junit.jupiter.api.Test")) {
            String importSection = "import org.junit.jupiter.api.Test;\n" +
                                 "import static org.junit.jupiter.api.Assertions.*;\n\n";
            
            if (testContent.contains("package ")) {
                int packageEnd = testContent.indexOf(";\n") + 2;
                testContent = testContent.substring(0, packageEnd) + "\n" + importSection + 
                             testContent.substring(packageEnd);
            } else {
                testContent = importSection + testContent;
            }
        }
        
        // Fix unbalanced braces by adding missing closing brace
        int openBraces = testContent.length() - testContent.replace("{", "").length();
        int closeBraces = testContent.length() - testContent.replace("}", "").length();
        
        while (openBraces > closeBraces) {
            testContent += "\n}";
            closeBraces++;
        }
        
        return testContent;
    }
    
    /**
     * Generate minimal test class as fallback
     */
    private String generateMinimalTestClass(String testFilePath) {
        String packageName = extractPackageFromTestPath(testFilePath);
        String className = extractClassNameFromTestPath(testFilePath);
        
        StringBuilder testClass = new StringBuilder();
        
        if (packageName != null && !packageName.isEmpty()) {
            testClass.append("package ").append(packageName).append(";\n\n");
        }
        
        testClass.append("import org.junit.jupiter.api.Test;\n");
        testClass.append("import static org.junit.jupiter.api.Assertions.*;\n\n");
        
        testClass.append("/**\n");
        testClass.append(" * Minimal test class generated as fallback\n");
        testClass.append(" */\n");
        testClass.append("class ").append(className).append(" {\n\n");
        testClass.append("    @Test\n");
        testClass.append("    void testPlaceholder() {\n");
        testClass.append("        // TODO: Implement actual test methods\n");
        testClass.append("        assertTrue(true, \"Placeholder test\");\n");
        testClass.append("    }\n");
        testClass.append("}\n");
        
        return testClass.toString();
    }

    /**
     * Merge new test methods with existing test file content
     */
    private String mergeWithExistingTestFile(String testFilePath, List<GeneratedTestInfo> newTests) throws IOException {
        log.info("Merging {} new test methods with existing file: {}", newTests.size(), testFilePath);
        
        String existingContent = Files.readString(Paths.get(testFilePath), StandardCharsets.UTF_8);
        
        // Find the last closing brace of the class
        int lastBraceIndex = existingContent.lastIndexOf("}");
        if (lastBraceIndex == -1) {
            log.warn("Could not find class closing brace in existing test file. Creating new file instead.");
            return generateCompleteTestClass(newTests, testFilePath);
        }
        
        // Extract existing content without the closing brace
        String contentBeforeClosingBrace = existingContent.substring(0, lastBraceIndex).trim();
        
        StringBuilder mergedContent = new StringBuilder(contentBeforeClosingBrace);
        mergedContent.append("\n\n");
        
        // Add comment indicating new generated tests
        mergedContent.append("    // ========================\n");
        mergedContent.append("    // AI-Generated Test Methods\n");
        mergedContent.append("    // ========================\n\n");
        
        // Add new test methods
        for (GeneratedTestInfo testInfo : newTests) {
            String testCode = testInfo.getTestCode();
            
            // Validate and clean the test method code
            testCode = cleanTestMethodCode(testCode);
            
            // Add proper indentation if missing
            if (!testCode.startsWith("    ")) {
                testCode = indentCode(testCode, "    ");
            }
            
            mergedContent.append("    @Test\n");
            mergedContent.append("    @DisplayName(\"").append(testInfo.getDescription()).append("\")\n");
            mergedContent.append("    ").append(testCode);
            if (!testCode.endsWith("\n")) {
                mergedContent.append("\n");
            }
            mergedContent.append("\n");
        }
        
        // Close class
        mergedContent.append("}\n");
        
        log.info("Successfully merged {} test methods into existing test class", newTests.size());
        return mergedContent.toString();
    }

    /**
     * Generate complete test class content from generated test methods
     * Enhanced with intelligent import detection based on source class type
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
        
        // Analyze source class to determine appropriate imports and test style
        ClassAnalysisResult classAnalysis = analyzeSourceClass(packageName, sourceClassName);
        
        StringBuilder testClass = new StringBuilder();
        
        // Add package declaration
        if (packageName != null && !packageName.isEmpty()) {
            testClass.append("package ").append(packageName).append(";\n\n");
        }
        
        // Add imports based on class analysis
        addImportsBasedOnClassType(testClass, classAnalysis);
        
        // Add class declaration
        testClass.append("/**\n");
        testClass.append(" * Generated test class for ").append(sourceClassName).append("\n");
        testClass.append(" * Auto-generated by DevGenie Coverage Improvement System\n");
        if (classAnalysis.isSpringBootApplication()) {
            testClass.append(" * Note: Spring Boot application class - limited testing recommended\n");
        }
        testClass.append(" */\n");
        
        // Add appropriate class annotations based on class type
        if (classAnalysis.isSpringBootApplication()) {
            testClass.append("@SpringBootTest\n");
        }
        
        testClass.append("class ").append(className).append(" {\n\n");
        
        // Add setup method only if needed (not for simple application classes)
        if (!classAnalysis.isSimpleApplicationClass()) {
            testClass.append("    @BeforeEach\n");
            testClass.append("    void setUp() {\n");
            testClass.append("        MockitoAnnotations.openMocks(this);\n");
            testClass.append("    }\n\n");
        }
        
        // Add generated test methods
        for (GeneratedTestInfo testInfo : generatedTests) {
            String testCode = testInfo.getTestCode();
            
            // Validate and clean the test method code
            testCode = cleanTestMethodCode(testCode);
            
            // Add proper indentation if missing
            if (!testCode.startsWith("    ")) {
                testCode = indentCode(testCode, "    ");
            }
            
            testClass.append("    @Test\n");
            testClass.append("    @DisplayName(\"").append(testInfo.getDescription()).append("\")\n");
            testClass.append("    ").append(testCode);
            if (!testCode.endsWith("\n")) {
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
     * Analyze source class to determine its type and appropriate testing approach
     */
    private ClassAnalysisResult analyzeSourceClass(String packageName, String sourceClassName) {
        // Simple heuristics to identify class types
        boolean isSpringBootApp = sourceClassName.contains("Application") || sourceClassName.endsWith("App");
        boolean isController = sourceClassName.contains("Controller");
        boolean isService = sourceClassName.contains("Service");
        boolean isRepository = sourceClassName.contains("Repository");
        boolean isConfiguration = sourceClassName.contains("Config");
        
        boolean isSimpleApplicationClass = isSpringBootApp && 
            !isController && !isService && !isRepository && !isConfiguration;
        
        return ClassAnalysisResult.builder()
            .className(sourceClassName)
            .packageName(packageName)
            .isSpringBootApplication(isSpringBootApp)
            .isController(isController)
            .isService(isService)
            .isRepository(isRepository)
            .isConfiguration(isConfiguration)
            .isSimpleApplicationClass(isSimpleApplicationClass)
            .build();
    }

    /**
     * Add appropriate imports based on the class type being tested
     * Enhanced for all types of Java applications
     */
    private void addImportsBasedOnClassType(StringBuilder testClass, ClassAnalysisResult analysis) {
        // Basic JUnit 5 imports for all Java applications
        testClass.append("import org.junit.jupiter.api.Test;\n");
        testClass.append("import org.junit.jupiter.api.DisplayName;\n");
        
        // Determine class type for appropriate test setup
        boolean isMainClass = analysis.getClassName().contains("Application") || analysis.getClassName().contains("Main");
        boolean isUtilityClass = analysis.getClassName().contains("Util") || analysis.getClassName().contains("Helper");
        boolean isDataClass = analysis.getClassName().contains("DTO") || analysis.getClassName().contains("Entity") || 
                             analysis.getClassName().contains("Model") || analysis.getClassName().contains("Data");
        
        // Add setup imports for complex classes (not for simple utilities, data classes, or main classes)
        if (!isMainClass && !isUtilityClass && !isDataClass && !analysis.isSimpleApplicationClass()) {
            testClass.append("import org.junit.jupiter.api.BeforeEach;\n");
            
            // Add Mockito for classes that likely need mocking
            if (analysis.isService() || analysis.isController() || 
                (!analysis.getClassName().contains("Config") && !analysis.getClassName().contains("Enum"))) {
                testClass.append("import org.mockito.Mock;\n");
                testClass.append("import org.mockito.MockitoAnnotations;\n");
                testClass.append("import org.junit.jupiter.api.extension.ExtendWith;\n");
                testClass.append("import org.mockito.junit.jupiter.MockitoExtension;\n");
            }
        }
        
        // Framework-specific imports
        if (analysis.isSpringBootApplication()) {
            // Spring Boot specific
            testClass.append("import org.springframework.boot.test.context.SpringBootTest;\n");
            testClass.append("import org.springframework.boot.SpringApplication;\n");
            testClass.append("import org.springframework.boot.autoconfigure.SpringBootApplication;\n");
            if (analysis.getClassName().contains("Async")) {
                testClass.append("import org.springframework.scheduling.annotation.EnableAsync;\n");
            }
            if (analysis.getClassName().contains("Schedule")) {
                testClass.append("import org.springframework.scheduling.annotation.EnableScheduling;\n");
            }
            if (analysis.getClassName().contains("Mongo") || analysis.isRepository()) {
                testClass.append("import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;\n");
            }
            testClass.append("import org.mockito.MockedStatic;\n");
            testClass.append("import org.mockito.Mockito;\n");
        } else if (analysis.isController()) {
            // Spring MVC specific (works with Spring Boot or Spring MVC)
            testClass.append("import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;\n");
            testClass.append("import org.springframework.test.web.servlet.MockMvc;\n");
            testClass.append("import org.springframework.beans.factory.annotation.Autowired;\n");
            testClass.append("import org.springframework.boot.test.mock.mockito.MockBean;\n");
        } else if (analysis.isService()) {
            // Service layer testing (any framework)
            if (analysis.getPackageName().contains("springframework")) {
                testClass.append("import org.springframework.boot.test.mock.mockito.MockBean;\n");
                testClass.append("import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;\n");
            }
        } else if (analysis.isRepository()) {
            // Repository testing (JPA, MongoDB, etc.)
            if (analysis.getPackageName().contains("springframework") || analysis.getClassName().contains("Jpa")) {
                testClass.append("import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;\n");
                testClass.append("import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;\n");
            }
            if (analysis.getClassName().contains("Mongo")) {
                testClass.append("import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;\n");
            }
        }
        
        // Standard assertion imports for all Java applications
        testClass.append("import static org.junit.jupiter.api.Assertions.*;\n");
        
        // Add Mockito static imports only if mocking is likely needed
        if (!isMainClass && !isUtilityClass && !isDataClass && !analysis.isSimpleApplicationClass()) {
            testClass.append("import static org.mockito.Mockito.*;\n");
        }
        
        // Add parametrized test support for utility classes
        if (isUtilityClass) {
            testClass.append("import org.junit.jupiter.params.ParameterizedTest;\n");
            testClass.append("import org.junit.jupiter.params.provider.ValueSource;\n");
        }
        
        testClass.append("\n");
    }

    /**
     * Data class for class analysis results
     */
    private static class ClassAnalysisResult {
        private String className;
        private String packageName;
        private boolean isSpringBootApplication;
        private boolean isController;
        private boolean isService;
        private boolean isRepository;
        private boolean isConfiguration;
        private boolean isSimpleApplicationClass;
        
        // Builder pattern implementation
        public static ClassAnalysisResultBuilder builder() {
            return new ClassAnalysisResultBuilder();
        }
        
        public static class ClassAnalysisResultBuilder {
            private String className;
            private String packageName;
            private boolean isSpringBootApplication;
            private boolean isController;
            private boolean isService;
            private boolean isRepository;
            private boolean isConfiguration;
            private boolean isSimpleApplicationClass;
            
            public ClassAnalysisResultBuilder className(String className) {
                this.className = className;
                return this;
            }
            
            public ClassAnalysisResultBuilder packageName(String packageName) {
                this.packageName = packageName;
                return this;
            }
            
            public ClassAnalysisResultBuilder isSpringBootApplication(boolean isSpringBootApplication) {
                this.isSpringBootApplication = isSpringBootApplication;
                return this;
            }
            
            public ClassAnalysisResultBuilder isController(boolean isController) {
                this.isController = isController;
                return this;
            }
            
            public ClassAnalysisResultBuilder isService(boolean isService) {
                this.isService = isService;
                return this;
            }
            
            public ClassAnalysisResultBuilder isRepository(boolean isRepository) {
                this.isRepository = isRepository;
                return this;
            }
            
            public ClassAnalysisResultBuilder isConfiguration(boolean isConfiguration) {
                this.isConfiguration = isConfiguration;
                return this;
            }
            
            public ClassAnalysisResultBuilder isSimpleApplicationClass(boolean isSimpleApplicationClass) {
                this.isSimpleApplicationClass = isSimpleApplicationClass;
                return this;
            }
            
            public ClassAnalysisResult build() {
                ClassAnalysisResult result = new ClassAnalysisResult();
                result.className = this.className;
                result.packageName = this.packageName;
                result.isSpringBootApplication = this.isSpringBootApplication;
                result.isController = this.isController;
                result.isService = this.isService;
                result.isRepository = this.isRepository;
                result.isConfiguration = this.isConfiguration;
                result.isSimpleApplicationClass = this.isSimpleApplicationClass;
                return result;
            }
        }
        
        // Getters
        public String getClassName() { return className; }
        public String getPackageName() { return packageName; }
        public boolean isSpringBootApplication() { return isSpringBootApplication; }
        public boolean isController() { return isController; }
        public boolean isService() { return isService; }
        public boolean isRepository() { return isRepository; }
        public boolean isConfiguration() { return isConfiguration; }
        public boolean isSimpleApplicationClass() { return isSimpleApplicationClass; }
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
        String fileName = testFilePath.substring(testFilePath.lastIndexOf("/") + 1);
        return fileName.replace(".java", "");
    }

    /**
     * Clean test method code to ensure it's properly formatted for inclusion in a class
     */
    private String cleanTestMethodCode(String testCode) {
        if (testCode == null || testCode.trim().isEmpty()) {
            return "void testPlaceholder() {\n        // TODO: Add test implementation\n    }";
        }
        
        String cleaned = testCode.trim();
        
        // Remove any class-level artifacts that might have leaked through
        cleaned = cleaned.replaceAll("package\\s+[^;]+;\\s*", "");
        cleaned = cleaned.replaceAll("import\\s+[^;]+;\\s*", "");
        cleaned = cleaned.replaceAll("(?s)class\\s+\\w+\\s*\\{", "");
        cleaned = cleaned.replaceAll("(?s)public\\s+class\\s+\\w+\\s*\\{", "");
        
        // Remove duplicate @Test annotations (in case they're already in the code)
        if (cleaned.startsWith("@Test")) {
            // Find the method signature and remove the @Test annotation since we'll add it
            cleaned = cleaned.replaceFirst("@Test\\s*", "");
        }
        
        // Remove @DisplayName if present since we'll add it
        cleaned = cleaned.replaceAll("@DisplayName\\([^)]*\\)\\s*", "");
        
        // Ensure the method starts with proper visibility and format
        if (!cleaned.matches("(?s)\\s*(public\\s+|private\\s+|protected\\s+)?void\\s+\\w+\\s*\\([^)]*\\)\\s*\\{.*")) {
            // If it doesn't look like a proper method, wrap it
            if (!cleaned.startsWith("void ")) {
                cleaned = "void testMethod() {\n    " + cleaned + "\n}";
            }
        }
        
        return cleaned.trim();
    }

    /**
     * Add proper indentation to code
     */
    private String indentCode(String code, String indent) {
        if (code == null || code.trim().isEmpty()) {
            return code;
        }
        
        return Arrays.stream(code.split("\n"))
                .map(line -> line.trim().isEmpty() ? line : indent + line)
                .collect(Collectors.joining("\n"));
    }
    
    /**
     * Generate test file path from source file path
     */
    private String generateTestFilePath(String sourceFilePath) {
        return sourceFilePath.replace("src/main/java", "src/test/java")
                           .replace(".java", "Test.java");
    }
    
    /**
     * Convert direct TestGenerationResult to result test info format
     */
    private List<FileCoverageImprovementResult.GeneratedTestInfo> convertDirectResultToResultTestInfo(TestGenerationResult result) {
        return result.getGeneratedTests().stream()
                .map(test -> FileCoverageImprovementResult.GeneratedTestInfo.builder()
                        .testMethodName(test.getMethodName())
                        .testClass(result.getTestClassName())
                        .description(test.getDescription())
                        .coveredMethods(test.getTargetCodePath() != null ? List.of(test.getTargetCodePath()) : new ArrayList<>())
                        .estimatedCoverageContribution(test.getCoverageImpact() != null ? 
                            getCoverageImpactValue(test.getCoverageImpact()) : 5.0)
                        .build())
                .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Convert direct TestGenerationResult to GeneratedTestInfo format
     */
    private List<GeneratedTestInfo> convertDirectResultToGeneratedTestInfo(TestGenerationResult result) {
        return result.getGeneratedTests().stream()
                .map(test -> GeneratedTestInfo.builder()
                        .testMethodName(test.getMethodName())
                        .testClass(result.getTestClassName())
                        .description(test.getDescription())
                        .testCode(extractTestMethodCode(result.getGeneratedTestContent(), test.getMethodName()))
                        .coveredMethods(test.getTargetCodePath() != null ? List.of(test.getTargetCodePath()) : new ArrayList<>())
                        .estimatedCoverageContribution(test.getCoverageImpact() != null ? 
                            getCoverageImpactValue(test.getCoverageImpact()) : 5.0)
                        .build())
                .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Convert coverage impact enum to numeric value
     */
    private double getCoverageImpactValue(String coverageImpact) {
        switch (coverageImpact.toUpperCase()) {
            case "HIGH": return 10.0;
            case "MEDIUM": return 5.0;
            case "LOW": return 2.0;
            default: return 5.0;
        }
    }
    
    /**
     * Extract specific test method code from complete test class content
     */
    private String extractTestMethodCode(String completeTestContent, String methodName) {
        if (completeTestContent == null || methodName == null) {
            return "void " + methodName + "() {\n        // Generated test method\n        assertTrue(true);\n    }";
        }
        
        // Try to extract the specific method
        int methodStart = completeTestContent.indexOf("void " + methodName);
        if (methodStart == -1) {
            // Fallback: return a simple test method
            return "void " + methodName + "() {\n        // Generated test method\n        assertTrue(true);\n    }";
        }
        
        // Find the method end by counting braces
        int braceCount = 0;
        int methodEnd = methodStart;
        boolean foundOpenBrace = false;
        
        for (int i = methodStart; i < completeTestContent.length(); i++) {
            char c = completeTestContent.charAt(i);
            if (c == '{') {
                braceCount++;
                foundOpenBrace = true;
            } else if (c == '}') {
                braceCount--;
                if (foundOpenBrace && braceCount == 0) {
                    methodEnd = i + 1;
                    break;
                }
            }
        }
        
        if (methodEnd > methodStart) {
            return completeTestContent.substring(methodStart, methodEnd);
        } else {
            return "void " + methodName + "() {\n        // Generated test method\n        assertTrue(true);\n    }";
        }
    }
    
    /**
     * Write generated test files using strategy-aware approach
     */
    private void writeGeneratedTestFilesWithStrategy(String repoDir, 
                                                   List<GeneratedTestInfo> generatedTests, 
                                                   List<String> testFilePaths, 
                                                   TestGenerationStrategy strategy,
                                                   String sourceContent,
                                                   TestGenerationResult directTestResult) throws IOException {
        log.info("=== WRITING TEST FILES USING {} STRATEGY ===", strategy.getStrategy());
        log.info("Repository directory: {}", repoDir);
        log.info("Number of test files to create: {}", testFilePaths.size());
        log.info("Number of generated tests: {}", generatedTests.size());
        
        switch (strategy.getStrategy()) {
            case DIRECT_FULL_FILE:
                // For DIRECT_FULL_FILE, use the complete test class content from TestGenerationResult
                writeDirectFullFileTests(repoDir, testFilePaths, strategy, directTestResult);
                break;
                
            case BATCH_METHOD_BASED:
            case MERGE_WITH_EXISTING:
                // Use existing hybrid approach for these strategies
                writeGeneratedTestFiles(repoDir, generatedTests, testFilePaths);
                break;
                
            default:
                log.warn("Unknown strategy {}, falling back to standard approach", strategy.getStrategy());
                writeGeneratedTestFiles(repoDir, generatedTests, testFilePaths);
                break;
        }
    }
    
    /**
     * Write test files for DIRECT_FULL_FILE strategy with minimal processing
     */
    private void writeDirectFullFileTests(String repoDir, 
                                        List<String> testFilePaths,
                                        TestGenerationStrategy strategy,
                                        TestGenerationResult testResult) throws IOException {
        log.info("Using DIRECT_FULL_FILE approach - preserving LLM output quality");
        
        if (testResult == null || !testResult.isSuccess()) {
            log.warn("No valid test result available for DIRECT_FULL_FILE strategy");
            return;
        }
        
        if (testFilePaths.isEmpty()) {
            log.warn("No test file paths provided for DIRECT_FULL_FILE strategy");
            return;
        }
        
        String testFilePath = testFilePaths.get(0); // DIRECT_FULL_FILE generates one complete test file
        String absoluteTestPath = Paths.get(repoDir, testFilePath).toString();
        
        log.info("Writing direct test file: {}", absoluteTestPath);
        
        // For DIRECT_FULL_FILE, use the complete test class content from TestGenerationResult
        String testContent = testResult.getGeneratedTestContent();
        
        if (testContent == null || testContent.trim().isEmpty()) {
            log.warn("No test content available in TestGenerationResult, skipping");
            return;
        }
        
        log.info("=== DEBUG: Full test content preview (first 500 chars) ===");
        log.info("{}", testContent.substring(0, Math.min(500, testContent.length())));
        log.info("=== DEBUG: Full test content length: {} characters ===", testContent.length());
        
        // Create directory if it doesn't exist
        Path testFile = Paths.get(absoluteTestPath);
        Files.createDirectories(testFile.getParent());
        
        // Write the complete test class directly
        Files.write(testFile, testContent.getBytes(StandardCharsets.UTF_8), 
                   java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.TRUNCATE_EXISTING);
        
        // Ensure the file is fully written to disk
        try {
            testFile.getFileSystem().provider().checkAccess(testFile);
        } catch (Exception e) {
            log.warn("Unable to verify file access after write: {}", e.getMessage());
        }
        
        log.info("Successfully wrote direct test file: {}", absoluteTestPath);
        log.info("=== DEBUG: File content that was actually written (first 200 chars) ===");
        log.info("{}", testContent.substring(0, Math.min(200, testContent.length())));
        log.info("=== DEBUG: Total file size written: {} characters ===", testContent.length());
    }
}
