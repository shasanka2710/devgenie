package com.org.devgenie.service.coverage;

import com.org.devgenie.config.CoverageConfiguration;
import com.org.devgenie.dto.coverage.*;
import com.org.devgenie.exception.coverage.CoverageException;
import com.org.devgenie.model.SonarQubeMetricsResponse;
import com.org.devgenie.model.coverage.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

// --- Dummy DTOs/Models for compilation. In a real project, these would be in their own files. ---
// Assuming Lombok annotations are available for these DTOs/Models

// com.org.devgenie.dto.coverage.*
interface CoverageDataService { SonarQubeMetricsResponse getCurrentCoverage(String repoPath, String repositoryUrl, String branch); List<CoverageData> getCurrentCoverage(String repoPath, String branch); void saveCoverageData(CoverageData coverageData); }
interface FileAnalysisService { FileAnalysisResult analyzeFile(String filePath); List<FilePriority> prioritizeFiles(List<CoverageData> currentCoverage, double targetCoverage); }
interface TestGenerationService { TestGenerationResult generateTestsForFile(FileAnalysisResult analysis); TestValidationResult validateGeneratedTests(String repoDir, List<String> testFilePaths); BatchTestGenerationResult generateTestsBatch(FileAnalysisResult analysis, int batchIndex, int maxTestsPerBatch); TestGenerationResult generateTestsForFileWithStrategy(FileAnalysisResult analysis, TestGenerationStrategy strategy); }
interface JacocoService { CoverageComparisonResult validateCoverageImprovement(String repoDir, String branch, ProjectConfiguration projectConfig, CoverageData originalCoverage); }
interface GitService { void applyChanges(List<FileChange> changes, String workspaceDir) throws IOException; void rollbackChanges(String sessionId); PullRequestResult createPullRequest(String sessionId, CoverageData finalCoverage, String repoDir); }
interface ProjectConfigDetectionService { ProjectConfiguration detectProjectConfiguration(String repoDir); }
interface SessionManagementService { CoverageImprovementSession createSession(String repositoryUrl, String branch, String targetFilePath, CoverageImprovementSession.SessionType sessionType); Optional<CoverageImprovementSession> getSession(String sessionId); void updateProgress(String sessionId, double progress, String currentStep); void handleError(String sessionId, Exception e); void setSessionResults(String sessionId, Object results); void updateSessionStatus(String sessionId, CoverageImprovementSession.SessionStatus status); }
interface RepositoryService { String setupWorkspace(String repositoryUrl, String branch, String githubToken); }

@lombok.Data @lombok.Builder @lombok.NoArgsConstructor @lombok.AllArgsConstructor
class FileCoverageRequest { private String filePath; }

@lombok.Data @lombok.Builder @lombok.NoArgsConstructor @lombok.AllArgsConstructor
class CoverageResponse {
    private boolean success; private String message; private CoverageSummary summary;
    public static CoverageResponse success(CoverageSummary summary) { return CoverageResponse.builder().success(true).message("Success").summary(summary).build(); }
    public static CoverageResponse failure(String message) { return CoverageResponse.builder().success(false).message(message).build(); }
}

@lombok.Data @lombok.Builder @lombok.NoArgsConstructor @lombok.AllArgsConstructor
class FileAnalysisResult { private String filePath; private double currentCoverage; private String packageName; private List<String> uncoveredMethods; private List<String> complexMethods; private double complexityScore; }

@lombok.Data @lombok.Builder @lombok.NoArgsConstructor @lombok.AllArgsConstructor
class TestGenerationResult {
    private boolean success; private String error; private String testFilePath; private String generatedTestContent; private List<GeneratedTestInfo> generatedTests; private String testClassName;
    public static TestGenerationResult success(String testFilePath, String generatedTestContent, List<GeneratedTestInfo> tests) { return TestGenerationResult.builder().success(true).testFilePath(testFilePath).generatedTestContent(generatedTestContent).generatedTests(tests).build(); }
    public static TestGenerationResult failure(String error) { return TestGenerationResult.builder().success(false).error(error).build(); }
}

@lombok.Data @lombok.Builder @lombok.NoArgsConstructor @lombok.AllArgsConstructor
class GeneratedTestInfo { private String testMethodName; private String testClass; private String description; private String testCode; private List<String> coveredMethods; private String coverageImpact; private Double estimatedCoverageContribution; private String methodName; private String targetCodePath; }

@lombok.Data @lombok.Builder @lombok.NoArgsConstructor @lombok.AllArgsConstructor
class CoverageSummary { private String sessionId; private Type type; private String filePath; private double originalCoverage; private double estimatedCoverage; private int testsAdded; private int filesProcessed; private int failedFiles; private List<FileChange> changes; public enum Type { FILE, REPOSITORY } }

@lombok.Data @lombok.Builder @lombok.NoArgsConstructor @lombok.AllArgsConstructor
class ApplyChangesRequest { private String sessionId; private List<FileChange> changes; private boolean createPullRequest; }

@lombok.Data @lombok.Builder @lombok.NoArgsConstructor @lombok.AllArgsConstructor
class ApplyChangesResponse {
    private boolean success; private String message; private PullRequestResult pullRequest; private CoverageData finalCoverage; private CoverageComparisonResult coverageComparison; private String validationMethod; private double coverageImprovement;
    public static ApplyChangesResponseBuilder builder() { return new ApplyChangesResponseBuilder(); }
    public static class ApplyChangesResponseBuilder {
        private boolean success; private PullRequestResult pullRequest; private CoverageData finalCoverage; private CoverageComparisonResult coverageComparison; private String validationMethod; private double coverageImprovement;
        public ApplyChangesResponseBuilder success(boolean success) { this.success = success; return this; }
        public ApplyChangesResponseBuilder pullRequest(PullRequestResult pullRequest) { this.pullRequest = pullRequest; return this; }
        public ApplyChangesResponseBuilder finalCoverage(CoverageData finalCoverage) { this.finalCoverage = finalCoverage; return this; }
        public ApplyChangesResponseBuilder coverageComparison(CoverageComparisonResult coverageComparison) { this.coverageComparison = coverageComparison; return this; }
        public ApplyChangesResponseBuilder validationMethod(String validationMethod) { this.validationMethod = validationMethod; return this; }
        public ApplyChangesResponseBuilder coverageImprovement(double coverageImprovement) { this.coverageImprovement = coverageImprovement; return this; }
        public ApplyChangesResponse build() { return new ApplyChangesResponse(success, null, pullRequest, finalCoverage, coverageComparison, validationMethod, coverageImprovement); }
    }
}

@lombok.Data @lombok.Builder @lombok.NoArgsConstructor @lombok.AllArgsConstructor
class EnhancedFileCoverageRequest { private String sessionId; private String filePath; private String repositoryUrl; private String branch; private String githubToken; private Boolean validateTests; }

@lombok.Data @lombok.Builder @lombok.NoArgsConstructor @lombok.AllArgsConstructor
class FileCoverageImprovementResult {
    private String sessionId; private String filePath; private String fileName; private String packageName; private double originalCoverage; private double improvedCoverage; private double coverageIncrease; private CoverageBreakdown beforeBreakdown; private CoverageBreakdown afterBreakdown; private List<GeneratedTestInfo> generatedTests; private int totalTestsGenerated; private List<String> testFilePaths; private LocalDateTime startedAt; private LocalDateTime completedAt; private long processingTimeMs; private int batchesProcessed; private int retryCount; private TestValidationResult validationResult; private Boolean testsCompiled; private Boolean testsExecuted; private ProcessingStatus status; private List<String> recommendations; private List<String> warnings; private List<String> errors;
    public enum ProcessingStatus { STARTED, IN_PROGRESS, COMPLETED, FAILED, READY_FOR_REVIEW }
    @lombok.Data @lombok.Builder @lombok.NoArgsConstructor @lombok.AllArgsConstructor
    public static class GeneratedTestInfo { private String testMethodName; private String testClass; private String description; private List<String> coveredMethods; private double estimatedCoverageContribution; }
    @lombok.Data @lombok.Builder @lombok.NoArgsConstructor @lombok.AllArgsConstructor
    public static class CoverageBreakdown { private double lineCoverage; private double branchCoverage; private double methodCoverage; private int totalLines; private int coveredLines; private int totalBranches; private int coveredBranches; private int totalMethods; private int coveredMethods; }
}

@lombok.Data @lombok.Builder @lombok.NoArgsConstructor @lombok.AllArgsConstructor
class TestValidationResult { private boolean success; private String errorMessage; private int testsExecuted; private int testsFailed; private int compilationErrors; }

@lombok.Data @lombok.Builder @lombok.NoArgsConstructor @lombok.AllArgsConstructor
class FileChange { private Type changeType; private String filePath; private String testFilePath; private String description; private String content; public enum Type { TEST_ADDED, TEST_MODIFIED, SOURCE_MODIFIED } }

@lombok.Data @lombok.Builder @lombok.NoArgsConstructor @lombok.AllArgsConstructor
class BatchTestGenerationResult { private Boolean success; private String error; private List<GeneratedTestInfo> generatedTests; private List<String> testFilePaths; }

@lombok.Data @lombok.Builder @lombok.NoArgsConstructor @lombok.AllArgsConstructor
class TestGenerationStrategy {
    private Strategy strategy; private String reasoning; private Integer maxTestsPerBatch;
    public enum Strategy { DIRECT_FULL_FILE, BATCH_METHOD_BASED, MERGE_WITH_EXISTING }
    public static TestGenerationStrategy determine(FileAnalysisResult analysis, boolean existingTestFile, String sourceContent) {
        return TestGenerationStrategy.builder().strategy(Strategy.DIRECT_FULL_FILE).reasoning("Forced direct generation for test").maxTestsPerBatch(10).build();
    }
}

// com.org.devgenie.model.coverage.*
@lombok.Data @lombok.Builder @lombok.NoArgsConstructor @lombok.AllArgsConstructor
class CoverageImprovementSession { private String sessionId; private String repositoryUrl; private String branch; private String targetFilePath; private SessionType sessionType; private LocalDateTime startTime; private LocalDateTime endTime; private SessionStatus status; private double progress; private String currentStep; private String errorMessage; private Object results; public enum SessionType { FILE_IMPROVEMENT, REPO_IMPROVEMENT } public enum SessionStatus { CREATED, IN_PROGRESS, READY_FOR_REVIEW, COMPLETED, FAILED } }

@lombok.Data @lombok.Builder @lombok.NoArgsConstructor @lombok.AllArgsConstructor
class CoverageData { private String repoPath; private String path; private String type; private double lineCoverage; private double branchCoverage; private double methodCoverage; private int totalLines; private int coveredLines; private int totalBranches; private int coveredBranches; private int totalMethods; private int coveredMethods; private double overallCoverage; }

@lombok.Data @lombok.Builder @lombok.NoArgsConstructor @lombok.AllArgsConstructor
class SonarQubeMetricsResponse { private List<CoverageData> coverageDataList; }

@lombok.Data @lombok.Builder @lombok.NoArgsConstructor @lombok.AllArgsConstructor
class ProjectConfiguration { private String buildTool; private String language; private String sourceDir; private String testDir; }

@lombok.Data @lombok.Builder @lombok.NoArgsConstructor @lombok.AllArgsConstructor
class CoverageComparisonResult { private CoverageData originalCoverage; private CoverageData newCoverage; private double coverageImprovement; private String validationMethod; private String comparisonReport; }

@lombok.Data @lombok.Builder @lombok.NoArgsConstructor @lombok.AllArgsConstructor
class PullRequestResult { private boolean success; private String pullRequestUrl; private String message; }

@lombok.Data @lombok.Builder @lombok.NoArgsConstructor @lombok.AllArgsConstructor
class FilePriority { private String filePath; private double priorityScore; }

@lombok.Data @lombok.NoArgsConstructor @lombok.AllArgsConstructor
class ProcessingResult { private List<String> successfulFiles; private List<String> failedFiles; private int totalTestsAdded; private List<FileChange> allChanges;
    public ProcessingResult() { this.successfulFiles = new java.util.ArrayList<>(); this.failedFiles = new java.util.ArrayList<>(); this.allChanges = new java.util.ArrayList<>(); this.totalTestsAdded = 0; }
    public void addSuccessfulFile(String filePath, TestGenerationResult result) { this.successfulFiles.add(filePath); this.totalTestsAdded += result.getGeneratedTests().size(); }
    public void addFailedFile(String filePath, String error) { this.failedFiles.add(filePath); }
    public List<String> getSuccessfulFiles() { return successfulFiles; }
    public List<String> getFailedFiles() { return failedFiles; }
    public int getTotalTestsAdded() { return totalTestsAdded; }
    public List<FileChange> getAllChanges() { return allChanges; }
}

// com.org.devgenie.config.CoverageConfiguration
@lombok.Data @org.springframework.stereotype.Component
class CoverageConfiguration { private double defaultCoverageIncrease = 5.0; private String workspaceRootDir = "/tmp/devgenie_workspace"; }

// com.org.devgenie.exception.coverage.CoverageException
class CoverageException extends RuntimeException { public CoverageException(String message, Throwable cause) { super(message, cause); } }


/**
 * Unit tests for {@link CoverageAgentService}.
 * Focuses on testing the main public methods and their critical paths,
 * including success and error scenarios.
 */
@ExtendWith(MockitoExtension.class)
class CoverageAgentServiceTest {

    @Mock
    private CoverageDataService coverageDataService;
    @Mock
    private FileAnalysisService fileAnalysisService;
    @Mock
    private TestGenerationService testGenerationService;
    @Mock
    private JacocoService jacocoService;
    @Mock
    private GitService gitService;
    @Mock
    private CoverageConfiguration config;
    @Mock
    private ProjectConfigDetectionService projectConfigService;
    @Mock
    private SessionManagementService sessionManagementService;
    @Mock
    private RepositoryService repositoryService;

    @InjectMocks
    private CoverageAgentService coverageAgentService;

    private String testRepoPath = "/test/repo";
    private String testFilePath = "src/main/java/com/example/MyClass.java";
    private String testSessionId = UUID.randomUUID().toString();
    private String testRepositoryUrl = "https://github.com/test/repo.git";
    private String testBranch = "main";
    private String testWorkspaceDir = "/tmp/devgenie_workspace";

    @BeforeEach
    void setUp() {
        // Common mock setup for config
        when(config.getWorkspaceRootDir()).thenReturn(testWorkspaceDir);
        when(config.getDefaultCoverageIncrease()).thenReturn(5.0);
    }

    @Test
    @DisplayName("Should successfully increase file coverage for a given file")
    void increaseFileCoverage_Success() {
        // Arrange
        FileCoverageRequest request = FileCoverageRequest.builder().filePath(testFilePath).build();
        FileAnalysisResult analysisResult = FileAnalysisResult.builder()
                .filePath(testFilePath)
                .currentCoverage(50.0)
                .packageName("com.example")
                .build();
        GeneratedTestInfo generatedTestInfo = GeneratedTestInfo.builder()
                .testMethodName("testMethod")
                .description("Tests a method")
                .estimatedCoverageContribution(5.0)
                .build();
        TestGenerationResult testResult = TestGenerationResult.builder()
                .success(true)
                .testFilePath("src/test/java/com/example/MyClassTest.java")
                .generatedTestContent("class MyClassTest {}")
                .generatedTests(Collections.singletonList(generatedTestInfo))
                .build();

        when(fileAnalysisService.analyzeFile(testFilePath)).thenReturn(analysisResult);
        when(testGenerationService.generateTestsForFile(analysisResult)).thenReturn(testResult);

        // Act
        CoverageResponse response = coverageAgentService.increaseFileCoverage(request);

        // Assert
        assertTrue(response.isSuccess());
        assertNotNull(response.getSummary());
        assertEquals(testFilePath, response.getSummary().getFilePath());
        assertEquals(50.0, response.getSummary().getOriginalCoverage());
        assertEquals(50.0 + 5.0, response.getSummary().getEstimatedCoverage()); // 5.0 from estimateCoverageImprovement
        assertEquals(1, response.getSummary().getTestsAdded());

        verify(fileAnalysisService).analyzeFile(testFilePath);
        verify(testGenerationService).generateTestsForFile(analysisResult);
    }

    @Test
    @DisplayName("Should throw CoverageException when file coverage increase fails due to internal error")
    void increaseFileCoverage_Exception() {
        // Arrange
        FileCoverageRequest request = FileCoverageRequest.builder().filePath(testFilePath).build();

        when(fileAnalysisService.analyzeFile(testFilePath)).thenThrow(new RuntimeException("Analysis failed"));

        // Act & Assert
        CoverageException thrown = assertThrows(CoverageException.class, () ->
                coverageAgentService.increaseFileCoverage(request)
        );
        assertTrue(thrown.getMessage().contains("Failed to process file"));
        assertTrue(thrown.getMessage().contains("Analysis failed"));

        verify(fileAnalysisService).analyzeFile(testFilePath);
        verifyNoInteractions(testGenerationService);
    }

    @Test
    @DisplayName("Should successfully apply changes with pull request creation")
    void applyChanges_Success_WithPR() throws IOException {
        // Arrange
        ApplyChangesRequest request = ApplyChangesRequest.builder()
                .sessionId(testSessionId)
                .createPullRequest(true)
                .changes(Collections.emptyList())
                .build();

        CoverageImprovementSession session = CoverageImprovementSession.builder()
                .sessionId(testSessionId)
                .repositoryUrl(testRepositoryUrl)
                .branch(testBranch)
                .build();

        CoverageData originalCoverage = CoverageData.builder()
                .lineCoverage(60.0)
                .build();
        CoverageData newCoverage = CoverageData.builder()
                .lineCoverage(75.0)
                .build();
        CoverageComparisonResult comparisonResult = CoverageComparisonResult.builder()
                .originalCoverage(originalCoverage)
                .newCoverage(newCoverage)
                .coverageImprovement(15.0)
                .validationMethod("Jacoco")
                .build();
        PullRequestResult prResult = PullRequestResult.builder()
                .success(true)
                .pullRequestUrl("http://pr.url")
                .build();

        when(sessionManagementService.getSession(testSessionId)).thenReturn(Optional.of(session));
        when(gitService.applyChanges(anyList(), anyString())).thenReturn(null); // Return void
        when(coverageDataService.getCurrentCoverage(anyString(), anyString(), anyString()))
                .thenReturn(SonarQubeMetricsResponse.builder().coverageDataList(Collections.singletonList(originalCoverage)).build());
        when(projectConfigService.detectProjectConfiguration(anyString())).thenReturn(ProjectConfiguration.builder().build());
        when(jacocoService.validateCoverageImprovement(anyString(), anyString(), any(ProjectConfiguration.class), any(CoverageData.class)))
                .thenReturn(comparisonResult);
        when(gitService.createPullRequest(eq(testSessionId), eq(newCoverage), anyString())).thenReturn(prResult);

        // Act
        ApplyChangesResponse response = coverageAgentService.applyChanges(request);

        // Assert
        assertTrue(response.isSuccess());
        assertNotNull(response.getPullRequest());
        assertEquals("http://pr.url", response.getPullRequest().getPullRequestUrl());
        assertEquals(75.0, response.getFinalCoverage().getLineCoverage());
        assertEquals(15.0, response.getCoverageImprovement());
        assertEquals("Jacoco", response.getValidationMethod());

        verify(sessionManagementService).getSession(testSessionId);
        verify(gitService).applyChanges(anyList(), anyString());
        verify(coverageDataService).getCurrentCoverage(anyString(), anyString(), anyString());
        verify(projectConfigService).detectProjectConfiguration(anyString());
        verify(jacocoService).validateCoverageImprovement(anyString(), anyString(), any(ProjectConfiguration.class), any(CoverageData.class));
        verify(gitService).createPullRequest(eq(testSessionId), eq(newCoverage), anyString());
        verify(sessionManagementService, never()).handleError(anyString(), any(Exception.class));
        verify(gitService, never()).rollbackChanges(anyString());
    }

    @Test
    @DisplayName("Should successfully apply changes without pull request creation")
    void applyChanges_Success_NoPR() throws IOException {
        // Arrange
        ApplyChangesRequest request = ApplyChangesRequest.builder()
                .sessionId(testSessionId)
                .createPullRequest(false)
                .changes(Collections.emptyList())
                .build();

        CoverageImprovementSession session = CoverageImprovementSession.builder()
                .sessionId(testSessionId)
                .repositoryUrl(testRepositoryUrl)
                .branch(testBranch)
                .build();

        CoverageData originalCoverage = CoverageData.builder().lineCoverage(60.0).build();
        CoverageData newCoverage = CoverageData.builder().lineCoverage(75.0).build();
        CoverageComparisonResult comparisonResult = CoverageComparisonResult.builder()
                .originalCoverage(originalCoverage)
                .newCoverage(newCoverage)
                .coverageImprovement(15.0)
                .build();

        when(sessionManagementService.getSession(testSessionId)).thenReturn(Optional.of(session));
        when(gitService.applyChanges(anyList(), anyString())).thenReturn(null);
        when(coverageDataService.getCurrentCoverage(anyString(), anyString(), anyString()))
                .thenReturn(SonarQubeMetricsResponse.builder().coverageDataList(Collections.singletonList(originalCoverage)).build());
        when(projectConfigService.detectProjectConfiguration(anyString())).thenReturn(ProjectConfiguration.builder().build());
        when(jacocoService.validateCoverageImprovement(anyString(), anyString(), any(ProjectConfiguration.class), any(CoverageData.class)))
                .thenReturn(comparisonResult);

        // Act
        ApplyChangesResponse response = coverageAgentService.applyChanges(request);

        // Assert
        assertTrue(response.isSuccess());
        assertNull(response.getPullRequest());
        verify(gitService, never()).createPullRequest(anyString(), any(CoverageData.class), anyString());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when session is not found for applyChanges")
    void applyChanges_SessionNotFound_ThrowsException() {
        // Arrange
        ApplyChangesRequest request = ApplyChangesRequest.builder()
                .sessionId(testSessionId)
                .createPullRequest(false)
                .changes(Collections.emptyList())
                .build();

        when(sessionManagementService.getSession(testSessionId)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () ->
                coverageAgentService.applyChanges(request)
        );
        assertTrue(thrown.getMessage().contains("Session not found: " + testSessionId));

        verify(sessionManagementService).getSession(testSessionId);
        verifyNoInteractions(gitService, coverageDataService, projectConfigService, jacocoService);
        verify(gitService, never()).rollbackChanges(anyString());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when repository URL is not found in session for applyChanges")
    void applyChanges_RepoUrlNotFoundInSession_ThrowsException() {
        // Arrange
        ApplyChangesRequest request = ApplyChangesRequest.builder()
                .sessionId(testSessionId)
                .createPullRequest(false)
                .changes(Collections.emptyList())
                .build();

        CoverageImprovementSession session = CoverageImprovementSession.builder()
                .sessionId(testSessionId)
                .repositoryUrl(null) // Simulate null repository URL
                .branch(testBranch)
                .build();

        when(sessionManagementService.getSession(testSessionId)).thenReturn(Optional.of(session));

        // Act & Assert
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () ->
                coverageAgentService.applyChanges(request)
        );
        assertTrue(thrown.getMessage().contains("Repository URL not found in session: " + testSessionId));

        verify(sessionManagementService).getSession(testSessionId);
        verifyNoInteractions(gitService, coverageDataService, projectConfigService, jacocoService);
        verify(gitService, never()).rollbackChanges(anyString());
    }

    @Test
    @DisplayName("Should rollback changes and throw CoverageException when an error occurs during applyChanges")
    void applyChanges_ExceptionDuringProcessing_RollsBackChanges() throws IOException {
        // Arrange
        ApplyChangesRequest request = ApplyChangesRequest.builder()
                .sessionId(testSessionId)
                .createPullRequest(true)
                .changes(Collections.emptyList())
                .build();

        CoverageImprovementSession session = CoverageImprovementSession.builder()
                .sessionId(testSessionId)
                .repositoryUrl(testRepositoryUrl)
                .branch(testBranch)
                .build();

        when(sessionManagementService.getSession(testSessionId)).thenReturn(Optional.of(session));
        // Simulate an error during gitService.applyChanges
        doThrow(new IOException("Git operation failed")).when(gitService).applyChanges(anyList(), anyString());

        // Act & Assert
        CoverageException thrown = assertThrows(CoverageException.class, () ->
                coverageAgentService.applyChanges(request)
        );
        assertTrue(thrown.getMessage().contains("Failed to apply changes"));
        assertTrue(thrown.getMessage().contains("Git operation failed"));

        verify(sessionManagementService).getSession(testSessionId);
        verify(gitService).applyChanges(anyList(), anyString());
        verify(gitService).rollbackChanges(testSessionId); // Verify rollback is called
        verify(sessionManagementService, never()).handleError(anyString(), any(Exception.class)); // Error handling is done by the caller of applyChanges
    }

    @Test
    @DisplayName("Should successfully improve file coverage enhanced with DIRECT_FULL_FILE strategy")
    void improveFileCoverageEnhanced_DirectFullFileStrategy_Success() throws IOException {
        // Arrange
        String sourceFilePath = testRepoPath + "/" + testFilePath;
        String testTestFilePath = "src/test/java/com/example/MyClassTest.java"; // Relative path
        String absoluteTestFilePath = testRepoPath + "/" + testTestFilePath;

        EnhancedFileCoverageRequest request = EnhancedFileCoverageRequest.builder()
                .sessionId(null) // New session
                .filePath(testFilePath)
                .repositoryUrl(testRepositoryUrl)
                .branch(testBranch)
                .githubToken("dummy-token")
                .validateTests(true)
                .build();

        CoverageImprovementSession newSession = CoverageImprovementSession.builder()
                .sessionId("new-session-id")
                .repositoryUrl(testRepositoryUrl)
                .branch(testBranch)
                .targetFilePath(testFilePath)
                .sessionType(CoverageImprovementSession.SessionType.FILE_IMPROVEMENT)
                .build();
        
        CoverageData fileCoverageData = CoverageData.builder()
                .type("FILE")
                .path(testFilePath)
                .lineCoverage(50.0)
                .totalLines(100)
                .coveredLines(50)
                .build();
        SonarQubeMetricsResponse currentCoverageResponse = SonarQubeMetricsResponse.builder()
                .coverageDataList(Collections.singletonList(fileCoverageData))
                .build();

        FileAnalysisResult analysisResult = FileAnalysisResult.builder()
                .filePath(sourceFilePath)
                .packageName("com.example")
                .uncoveredMethods(Arrays.asList("methodA", "methodB"))
                .build();
        
        GeneratedTestInfo directGeneratedTestInfo = GeneratedTestInfo.builder()
                .methodName("testMethodA")
                .testClass("MyClassTest")
                .description("Tests method A")
                .testCode("void testMethodA() { /* test code */ }")
                .coverageImpact("HIGH")
                .build();
        
        TestGenerationResult directTestResult = TestGenerationResult.builder()
                .success(true)
                .testFilePath(testTestFilePath) // Relative path
                .generatedTestContent("package com.example;\n\nimport org.junit.jupiter.api.Test;\n\nclass MyClassTest {\n    @Test\n    void testMethodA() {}\n}\n")
                .generatedTests(Collections.singletonList(directGeneratedTestInfo))
                .build();

        TestValidationResult validationResult = TestValidationResult.builder()
                .success(true)
                .testsExecuted(1)
                .testsFailed(0)
                .compilationErrors(0)
                .build();

        // Mock static Files methods
        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            // Arrange mocks
            when(sessionManagementService.createSession(anyString(), anyString(), anyString(), any(CoverageImprovementSession.SessionType.class)))
                    .thenReturn(newSession);
            when(repositoryService.setupWorkspace(anyString(), anyString(), anyString())).thenReturn(testRepoPath);
            when(coverageDataService.getCurrentCoverage(eq(testRepoPath), anyString(), anyString()))
                    .thenReturn(currentCoverageResponse);
            when(fileAnalysisService.analyzeFile(sourceFilePath)).thenReturn(analysisResult);
            
            // Mock TestGenerationStrategy.determine to force DIRECT_FULL_FILE
            mockedFiles.when(() -> Files.exists(Paths.get(sourceFilePath))).thenReturn(true);
            mockedFiles.when(() -> Files.readString(Paths.get(sourceFilePath))).thenReturn("public class MyClass {}"); // Dummy source content
            mockedFiles.when(() -> Files.exists(Paths.get(testRepoPath, testTestFilePath))).thenReturn(false); // No existing test file
            
            // Mock static method to return our desired strategy
            try (MockedStatic<TestGenerationStrategy> mockedStrategy = mockStatic(TestGenerationStrategy.class)) {
                mockedStrategy.when(() -> TestGenerationStrategy.determine(any(FileAnalysisResult.class), anyBoolean(), anyString()))
                        .thenReturn(TestGenerationStrategy.builder()
                                .strategy(TestGenerationStrategy.Strategy.DIRECT_FULL_FILE)
                                .reasoning("Forced direct generation for test")
                                .maxTestsPerBatch(10)
                                .build());

                when(testGenerationService.generateTestsForFileWithStrategy(eq(analysisResult), any(TestGenerationStrategy.class)))
                        .thenReturn(directTestResult);
                when(testGenerationService.validateGeneratedTests(eq(testRepoPath), anyList())).thenReturn(validationResult);

                // Mock Files.createDirectories and Files.write for the internal helper
                mockedFiles.when(() -> Files.createDirectories(any(Path.class))).thenReturn(Paths.get("/dummy/path"));
                mockedFiles.when(() -> Files.write(any(Path.class), any(byte[].class), any())).thenReturn(any()); // Return void

                // Mock gitService.applyChanges called by writeDirectFullFileTests
                doNothing().when(gitService).applyChanges(anyList(), anyString());

                // Act
                FileCoverageImprovementResult result = coverageAgentService.improveFileCoverageEnhanced(request);

                // Assert
                assertNotNull(result);
                assertEquals(newSession.getSessionId(), result.getSessionId());
                assertEquals(testFilePath, result.getFilePath());
                assertEquals(50.0, result.getOriginalCoverage());
                assertTrue(result.getImprovedCoverage() > 50.0);
                assertEquals(1, result.getTotalTestsGenerated());
                assertTrue(result.getTestFilePaths().contains(testTestFilePath));
                assertTrue(result.getTestsCompiled());
                assertTrue(result.getTestsExecuted());
                assertEquals(FileCoverageImprovementResult.ProcessingStatus.COMPLETED, result.getStatus());

                // Verify session management calls
                verify(sessionManagementService).createSession(testRepositoryUrl, testBranch, testFilePath, CoverageImprovementSession.SessionType.FILE_IMPROVEMENT);
                verify(sessionManagementService, atLeast(5)).updateProgress(anyString(), anyDouble(), anyString());
                verify(sessionManagementService).setSessionResults(eq(newSession.getSessionId()), any(FileCoverageImprovementResult.class));
                verify(sessionManagementService).updateSessionStatus(newSession.getSessionId(), CoverageImprovementSession.SessionStatus.READY_FOR_REVIEW);

                // Verify service calls
                verify(repositoryService).setupWorkspace(testRepositoryUrl, testBranch, "dummy-token");
                verify(coverageDataService).getCurrentCoverage(testRepoPath, testRepositoryUrl, testBranch);
                verify(fileAnalysisService).analyzeFile(sourceFilePath);
                verify(testGenerationService).generateTestsForFileWithStrategy(eq(analysisResult), any(TestGenerationStrategy.class));
                verify(testGenerationService).validateGeneratedTests(eq(testRepoPath), anyList());
                verify(gitService).applyChanges(anyList(), eq(testRepoPath)); // Verify gitService.applyChanges is called

                // Verify static Files interactions (inside writeDirectFullFileTests)
                mockedFiles.verify(() -> Files.exists(Paths.get(sourceFilePath)), atLeastOnce());
                mockedFiles.verify(() -> Files.readString(Paths.get(sourceFilePath)), atLeastOnce());
                mockedFiles.verify(() -> Files.createDirectories(any(Path.class)), atLeastOnce());
                mockedFiles.verify(() -> Files.write(eq(Paths.get(absoluteTestFilePath)), any(byte[].class), any()), atLeastOnce());
            }
        }
    }

    @Test
    @DisplayName("Should handle exception during enhanced file coverage improvement and log error")
    void improveFileCoverageEnhanced_ExceptionHandling() throws IOException {
        // Arrange
        EnhancedFileCoverageRequest request = EnhancedFileCoverageRequest.builder()
                .sessionId(null)
                .filePath(testFilePath)
                .repositoryUrl(testRepositoryUrl)
                .branch(testBranch)
                .githubToken("dummy-token")
                .validateTests(true)
                .build();

        CoverageImprovementSession newSession = CoverageImprovementSession.builder()
                .sessionId("new-session-id")
                .build();

        when(sessionManagementService.createSession(anyString(), anyString(), anyString(), any(CoverageImprovementSession.SessionType.class)))
                .thenReturn(newSession);
        // Simulate an error during workspace setup
        doThrow(new RuntimeException("Workspace setup failed")).when(repositoryService).setupWorkspace(anyString(), anyString(), anyString());

        // Act & Assert
        CoverageException thrown = assertThrows(CoverageException.class, () ->
                coverageAgentService.improveFileCoverageEnhanced(request)
        );
        assertTrue(thrown.getMessage().contains("Failed to improve file coverage"));
        assertTrue(thrown.getMessage().contains("Workspace setup failed"));

        verify(sessionManagementService).createSession(anyString(), anyString(), anyString(), any(CoverageImprovementSession.SessionType.class));
        verify(sessionManagementService, atLeastOnce()).updateProgress(anyString(), anyDouble(), anyString());
        verify(sessionManagementService).handleError(eq(newSession.getSessionId()), any(RuntimeException.class));
        verifyNoMoreInteractions(coverageDataService, fileAnalysisService, testGenerationService, jacocoService, gitService);
    }
}