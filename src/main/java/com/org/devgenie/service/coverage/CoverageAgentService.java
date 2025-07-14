package com.org.devgenie.service.coverage;

import com.org.devgenie.config.CoverageConfiguration;
import com.org.devgenie.dto.coverage.*;
import com.org.devgenie.exception.coverage.CoverageException;
import com.org.devgenie.model.coverage.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

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

    public CoverageResponse increaseRepoCoverage(RepoCoverageRequest request) {
        log.info("Starting repository coverage increase for target: {}%", request.getTargetCoverageIncrease());

        try {
            // Initialize coverage analysis
            CoverageData currentCoverage = coverageDataService.getCurrentCoverage(request.getRepoPath());

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
        }
    }

    public ApplyChangesResponse applyChanges(ApplyChangesRequest request) {
        log.info("Applying changes for session: {}", request.getSessionId());

        try {
            // Apply all generated test files
            gitService.applyChanges(request.getChanges());

            // Run final Jacoco analysis
            CoverageData finalCoverage = jacocoService.runAnalysis(request.getRepoPath());

            // Create PR
            PullRequestResult prResult = gitService.createPullRequest(request.getSessionId(), finalCoverage);

            return ApplyChangesResponse.success(prResult, finalCoverage);

        } catch (Exception e) {
            log.error("Failed to apply changes", e);
            // Rollback changes
            gitService.rollbackChanges(request.getSessionId());
            throw new CoverageException("Failed to apply changes: " + e.getMessage(), e);
        }
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
}
