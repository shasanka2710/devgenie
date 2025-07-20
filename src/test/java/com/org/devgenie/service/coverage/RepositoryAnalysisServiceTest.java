package com.org.devgenie.service.coverage;

import com.org.devgenie.model.coverage.*;
import com.org.devgenie.service.metadata.MetadataAnalyzer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
class RepositoryAnalysisServiceTest {

    @InjectMocks
    private RepositoryAnalysisService repositoryAnalysisService;

    @Test
    void testAnalyzeRepository_WithValidRequest_ReturnsSuccessResponse() {
        RepositoryAnalysisRequest request = new RepositoryAnalysisRequest();
        request.setRepositoryUrl("https://github.com/test/repo.git");
        request.setBranch("main");
        request.setWorkspaceId("test-workspace");

        // Mock file operations to avoid actual file I/O and external calls
        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.exists(any(Path.class))).thenReturn(false);

            RepositoryAnalysisResponse response = repositoryAnalysisService.analyzeRepository(request);

            assertNotNull(response);
            assertNotNull(response.getError()); // Should have an error since we're mocking no files exist
            assertFalse(response.isSuccess());
        }
    }

    @Test
    void testGetSimplifiedRepositoryInsights_CreatesDefaultInsights() {
        String testRepoDir = "/test/repo";
        List<String> testJavaFiles = Arrays.asList(
                "/test/repo/src/main/java/com/example/Service.java",
                "/test/repo/src/main/java/com/example/Controller.java"
        );
        
        ProjectConfiguration testConfig = ProjectConfiguration.builder()
                .buildTool("Maven")
                .testFramework("JUnit5")
                .javaVersion("11")
                .dependencies(Arrays.asList("spring-boot-starter", "spring-boot-starter-test"))
                .testDirectories(Arrays.asList("src/test/java"))
                .isSpringBoot(true)
                .build();

        List<MetadataAnalyzer.FileMetadata> testFileMetadata = Collections.emptyList();
        List<CoverageData> testExistingCoverage = Collections.emptyList();

        // Mock file operations to avoid actual file I/O
        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.exists(any(Path.class))).thenReturn(true);
            mockedFiles.when(() -> Files.readString(any(Path.class))).thenReturn("public class TestClass {}");

            SimplifiedRepositoryInsights result = repositoryAnalysisService.getSimplifiedRepositoryInsights(
                    testRepoDir, testJavaFiles, testConfig, testFileMetadata, testExistingCoverage);

            assertNotNull(result);
            assertNotNull(result.getRepositorySummary());
            assertNotNull(result.getCriticalFindings());
            assertNotNull(result.getRecommendations());
            
            // Verify basic structure
            assertNotNull(result.getRepositorySummary().getOverallRiskLevel());
            assertNotNull(result.getRepositorySummary().getComplexityScore());
            assertNotNull(result.getRepositorySummary().getCoverageGrade());
            assertNotNull(result.getRepositorySummary().getPrimaryConcerns());
        }
    }

    @Test
    void testCreateDefaultSimplifiedInsights_HasAllRequiredFields() {
        // Use reflection to access the private method for testing
        try {
            var method = RepositoryAnalysisService.class.getDeclaredMethod("createDefaultSimplifiedInsights");
            method.setAccessible(true);
            SimplifiedRepositoryInsights result = (SimplifiedRepositoryInsights) method.invoke(repositoryAnalysisService);

            assertNotNull(result);
            assertNotNull(result.getRepositorySummary());
            assertNotNull(result.getCriticalFindings());
            assertNotNull(result.getRecommendations());
            
            // Verify default values
            SimplifiedRepositoryInsights.RepositorySummary summary = result.getRepositorySummary();
            assertNotNull(summary.getOverallRiskLevel());
            assertNotNull(summary.getComplexityScore());
            assertNotNull(summary.getCoverageGrade());
            assertNotNull(summary.getPrimaryConcerns());
            
            SimplifiedRepositoryInsights.CriticalFindings findings = result.getCriticalFindings();
            assertNotNull(findings.getHighestRiskFiles());
            assertNotNull(findings.getCoverageGaps());
            assertNotNull(findings.getArchitecturalIssues());
            
            assertNotNull(result.getRecommendations());
            assertFalse(result.getRecommendations().isEmpty());
        } catch (Exception e) {
            fail("Failed to test createDefaultSimplifiedInsights: " + e.getMessage());
        }
    }

    @Test
    void testSimplifiedRepositoryInsights_StructureIntegrity() {
        // Test that the simplified insights model has the expected structure
        SimplifiedRepositoryInsights insights = SimplifiedRepositoryInsights.builder()
                .repositorySummary(SimplifiedRepositoryInsights.RepositorySummary.builder()
                        .overallRiskLevel("MEDIUM")
                        .complexityScore(5)
                        .coverageGrade("B")
                        .primaryConcerns(Arrays.asList("Low test coverage", "High complexity"))
                        .build())
                .criticalFindings(SimplifiedRepositoryInsights.CriticalFindings.builder()
                        .highestRiskFiles(Arrays.asList(
                                SimplifiedRepositoryInsights.HighRiskFile.builder()
                                        .fileName("ComplexService.java")
                                        .riskScore(8.5)
                                        .reason("High cyclomatic complexity")
                                        .build()
                        ))
                        .coverageGaps(Arrays.asList("Missing unit tests for service layer"))
                        .architecturalIssues(Arrays.asList("Tight coupling detected"))
                        .build())
                .recommendations(Arrays.asList(
                        SimplifiedRepositoryInsights.Recommendation.builder()
                                .priority("HIGH")
                                .title("Add unit tests for service classes")
                                .description("Implement comprehensive unit tests for all service layer classes")
                                .impact("Improved reliability and maintainability")
                                .effort("Medium")
                                .build()
                ))
                .build();

        assertNotNull(insights);
        assertEquals("MEDIUM", insights.getRepositorySummary().getOverallRiskLevel());
        assertEquals(5, insights.getRepositorySummary().getComplexityScore());
        assertEquals("B", insights.getRepositorySummary().getCoverageGrade());
        assertEquals(2, insights.getRepositorySummary().getPrimaryConcerns().size());
        
        assertEquals(1, insights.getCriticalFindings().getHighestRiskFiles().size());
        assertEquals("ComplexService.java", insights.getCriticalFindings().getHighestRiskFiles().get(0).getFileName());
        assertEquals(8.5, insights.getCriticalFindings().getHighestRiskFiles().get(0).getRiskScore());
        
        assertEquals(1, insights.getRecommendations().size());
        assertEquals("HIGH", insights.getRecommendations().get(0).getPriority());
    }
}
