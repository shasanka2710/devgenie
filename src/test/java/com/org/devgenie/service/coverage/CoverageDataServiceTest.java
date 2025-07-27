package com.org.devgenie.service.coverage;

import com.org.devgenie.exception.coverage.CoverageDataNotFoundException;
import com.org.devgenie.model.SonarBaseComponentMetrics;
import com.org.devgenie.model.SonarQubeMetricsResponse;
import com.org.devgenie.model.coverage.CoverageData;
import com.org.devgenie.model.coverage.FileCoverageData;
import com.org.devgenie.mongo.RepositoryAnalysisMongoUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the {@link CoverageDataService} class.
 * This class focuses on testing the business logic of the service by mocking its dependencies.
 */
@ExtendWith(MockitoExtension.class)
class CoverageDataServiceTest {

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private JacocoService jacocoService;

    @Mock
    private RepositoryAnalysisMongoUtil analysisMongoUtil;

    @InjectMocks
    private CoverageDataService coverageDataService;

    private final String REPO_DIR = "/path/to/repo";
    private final String REPO_PATH = "github.com/org/repo";
    private final String BRANCH = "main";
    private final String FILE_PATH = "src/main/java/File.java";

    @BeforeEach
    void setUp() {
        // Reset mocks and inject again for each test to ensure isolation
        // ReflectionTestUtils is used to inject the @Value field 'useMongoData'
    }

    /**
     * Test case for `getCurrentCoverage` when `useMongoData` is true and MongoDB operation succeeds.
     * Verifies that data is fetched from MongoDB and JacocoService is not called.
     */
    @Test
    void getCurrentCoverage_useMongoDataTrue_mongoSuccess() {
        // Arrange
        ReflectionTestUtils.setField(coverageDataService, "useMongoData", true);

        CoverageData mockCoverageData = new CoverageData();
        mockCoverageData.setRepositoryUrl(REPO_PATH);
        mockCoverageData.setBranch(BRANCH);
        List<CoverageData> mockCoverageList = Collections.singletonList(mockCoverageData);

        SonarBaseComponentMetrics mockSonarMetrics = SonarBaseComponentMetrics.builder()
                .branch(BRANCH)
                .repositoryUrl(REPO_PATH)
                .lines(100)
                .ncloc(90)
                .build();

        // Mock the internal calls to getCoverageFromMongo
        when(mongoTemplate.find(any(Query.class), eq(CoverageData.class), eq("coverage_data")))
                .thenReturn(mockCoverageList);
        when(analysisMongoUtil.getSonarBaseComponentMetrics(REPO_PATH, BRANCH))
                .thenReturn(mockSonarMetrics);

        // Act
        SonarQubeMetricsResponse response = coverageDataService.getCurrentCoverage(REPO_DIR, REPO_PATH, BRANCH);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getCoverageDataList().size());
        assertEquals(mockCoverageData, response.getCoverageDataList().get(0));
        assertEquals(mockSonarMetrics, response.getSonarBaseComponentMetrics());

        verify(mongoTemplate, times(1)).find(any(Query.class), eq(CoverageData.class), eq("coverage_data"));
        verify(analysisMongoUtil, times(1)).getSonarBaseComponentMetrics(REPO_PATH, BRANCH);
        verifyNoInteractions(jacocoService); // Ensure JacocoService is not called
    }

    /**
     * Test case for `getCurrentCoverage` when `useMongoData` is true but MongoDB operation fails,
     * triggering a fallback to JacocoService.
     */
    @Test
    void getCurrentCoverage_useMongoDataTrue_mongoFailsFallbackToJacoco() {
        // Arrange
        ReflectionTestUtils.setField(coverageDataService, "useMongoData", true);

        // Simulate an exception from MongoDB (e.g., connection error or data not found)
        when(mongoTemplate.find(any(Query.class), eq(CoverageData.class), eq("coverage_data")))
                .thenThrow(new RuntimeException("MongoDB connection error"));

        SonarQubeMetricsResponse mockJacocoResponse = SonarQubeMetricsResponse.builder()
                .coverageDataList(Collections.emptyList())
                .sonarBaseComponentMetrics(null)
                .build();
        when(jacocoService.runAnalysis(REPO_DIR, REPO_PATH, BRANCH)).thenReturn(mockJacocoResponse);

        // Act
        SonarQubeMetricsResponse response = coverageDataService.getCurrentCoverage(REPO_DIR, REPO_PATH, BRANCH);

        // Assert
        assertNotNull(response);
        assertEquals(mockJacocoResponse, response);

        verify(mongoTemplate, times(1)).find(any(Query.class), eq(CoverageData.class), eq("coverage_data"));
        verify(jacocoService, times(1)).runAnalysis(REPO_DIR, REPO_PATH, BRANCH);
        verifyNoInteractions(analysisMongoUtil); // Should not interact if mongo fails before calling it
    }

    /**
     * Test case for `getCurrentCoverage` when `useMongoData` is false.
     * Verifies that JacocoService is called directly and MongoDB is not consulted.
     */
    @Test
    void getCurrentCoverage_useMongoDataFalse_callsJacocoDirectly() {
        // Arrange
        ReflectionTestUtils.setField(coverageDataService, "useMongoData", false);

        SonarQubeMetricsResponse mockJacocoResponse = SonarQubeMetricsResponse.builder()
                .coverageDataList(Collections.emptyList())
                .sonarBaseComponentMetrics(null)
                .build();
        when(jacocoService.runAnalysis(REPO_DIR, REPO_PATH, BRANCH)).thenReturn(mockJacocoResponse);

        // Act
        SonarQubeMetricsResponse response = coverageDataService.getCurrentCoverage(REPO_DIR, REPO_PATH, BRANCH);

        // Assert
        assertNotNull(response);
        assertEquals(mockJacocoResponse, response);

        verify(jacocoService, times(1)).runAnalysis(REPO_DIR, REPO_PATH, BRANCH);
        verifyNoInteractions(mongoTemplate); // Ensure MongoDB is not consulted
        verifyNoInteractions(analysisMongoUtil);
    }

    /**
     * Test case for `getFileCoverage` when coverage data is found in MongoDB.
     */
    @Test
    void getFileCoverage_dataFound() {
        // Arrange
        FileCoverageData mockFileCoverage = FileCoverageData.builder()
                .filePath(FILE_PATH)
                .lineCoverage(80.0)
                .build();

        when(mongoTemplate.findOne(any(Query.class), eq(FileCoverageData.class), eq("file_coverage")))
                .thenReturn(mockFileCoverage);

        // Act
        FileCoverageData result = coverageDataService.getFileCoverage(FILE_PATH);

        // Assert
        assertNotNull(result);
        assertEquals(FILE_PATH, result.getFilePath());
        assertEquals(80.0, result.getLineCoverage());
        verify(mongoTemplate, times(1)).findOne(any(Query.class), eq(FileCoverageData.class), eq("file_coverage"));
    }

    /**
     * Test case for `getFileCoverage` when no coverage data is found in MongoDB.
     * Verifies that a default `FileCoverageData` object is created and returned.
     */
    @Test
    void getFileCoverage_dataNotFound_returnsDefault() {
        // Arrange
        when(mongoTemplate.findOne(any(Query.class), eq(FileCoverageData.class), eq("file_coverage")))
                .thenReturn(null);

        // Act
        FileCoverageData result = coverageDataService.getFileCoverage(FILE_PATH);

        // Assert
        assertNotNull(result);
        assertEquals(FILE_PATH, result.getFilePath());
        assertEquals(0.0, result.getLineCoverage());
        assertEquals(0.0, result.getBranchCoverage());
        assertEquals(0.0, result.getMethodCoverage());
        assertTrue(result.getUncoveredLines().isEmpty());
        assertTrue(result.getUncoveredBranches().isEmpty());
        assertEquals(0, result.getTotalLines());
        assertEquals(0, result.getTotalBranches());
        assertEquals(0, result.getTotalMethods());

        verify(mongoTemplate, times(1)).findOne(any(Query.class), eq(FileCoverageData.class), eq("file_coverage"));
    }

    /**
     * Test case for `getCoverageFromMongo` when data and Sonar metrics are found.
     */
    @Test
    void getCoverageFromMongo_success() {
        // Arrange
        CoverageData mockCoverageData = new CoverageData();
        mockCoverageData.setRepositoryUrl(REPO_PATH);
        mockCoverageData.setBranch(BRANCH);
        List<CoverageData> mockCoverageList = Collections.singletonList(mockCoverageData);

        SonarBaseComponentMetrics mockSonarMetrics = SonarBaseComponentMetrics.builder()
                .branch(BRANCH)
                .repositoryUrl(REPO_PATH)
                .lines(100)
                .ncloc(90)
                .build();

        when(mongoTemplate.find(any(Query.class), eq(CoverageData.class), eq("coverage_data")))
                .thenReturn(mockCoverageList);
        when(analysisMongoUtil.getSonarBaseComponentMetrics(REPO_PATH, BRANCH))
                .thenReturn(mockSonarMetrics);

        // Act
        SonarQubeMetricsResponse response = coverageDataService.getCoverageFromMongo(REPO_PATH, BRANCH);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getCoverageDataList().size());
        assertEquals(mockCoverageData, response.getCoverageDataList().get(0));
        assertEquals(mockSonarMetrics, response.getSonarBaseComponentMetrics());

        verify(mongoTemplate, times(1)).find(any(Query.class), eq(CoverageData.class), eq("coverage_data"));
        verify(analysisMongoUtil, times(1)).getSonarBaseComponentMetrics(REPO_PATH, BRANCH);
    }

    /**
     * Test case for `getCoverageFromMongo` when no coverage data is found in MongoDB.
     * Expects `CoverageDataNotFoundException`.
     */
    @Test
    void getCoverageFromMongo_noCoverageDataThrowsException() {
        // Arrange
        when(mongoTemplate.find(any(Query.class), eq(CoverageData.class), eq("coverage_data")))
                .thenReturn(Collections.emptyList());

        // Act & Assert
        CoverageDataNotFoundException thrown = assertThrows(CoverageDataNotFoundException.class,
                () -> coverageDataService.getCoverageFromMongo(REPO_PATH, BRANCH));

        assertTrue(thrown.getMessage().contains("No coverage data found"));
        verify(mongoTemplate, times(1)).find(any(Query.class), eq(CoverageData.class), eq("coverage_data"));
        verifyNoInteractions(analysisMongoUtil); // Should not be called if coverage data is empty
    }

    /**
     * Test case for `getCoverageFromMongo` when coverage data is found but Sonar metrics are null.
     * Expects `CoverageDataNotFoundException`.
     */
    @Test
    void getCoverageFromMongo_noSonarMetricsThrowsException() {
        // Arrange
        CoverageData mockCoverageData = new CoverageData();
        mockCoverageData.setRepositoryUrl(REPO_PATH);
        mockCoverageData.setBranch(BRANCH);
        List<CoverageData> mockCoverageList = Collections.singletonList(mockCoverageData);

        when(mongoTemplate.find(any(Query.class), eq(CoverageData.class), eq("coverage_data")))
                .thenReturn(mockCoverageList);
        when(analysisMongoUtil.getSonarBaseComponentMetrics(REPO_PATH, BRANCH))
                .thenReturn(null); // Simulate Sonar metrics not found

        // Act & Assert
        CoverageDataNotFoundException thrown = assertThrows(CoverageDataNotFoundException.class,
                () -> coverageDataService.getCoverageFromMongo(REPO_PATH, BRANCH));

        assertTrue(thrown.getMessage().contains("No coverage data found"));
        verify(mongoTemplate, times(1)).find(any(Query.class), eq(CoverageData.class), eq("coverage_data"));
        verify(analysisMongoUtil, times(1)).getSonarBaseComponentMetrics(REPO_PATH, BRANCH);
    }
}