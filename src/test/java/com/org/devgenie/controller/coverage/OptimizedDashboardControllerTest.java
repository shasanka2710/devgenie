package com.org.devgenie.controller.coverage;

import com.org.devgenie.service.coverage.OptimizedCoverageService;
import com.org.devgenie.service.coverage.CoverageDataMigrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test cases for OptimizedDashboardController
 */
@ExtendWith(MockitoExtension.class)
class OptimizedDashboardControllerTest {

    @Mock
    private OptimizedCoverageService optimizedCoverageService;

    @Mock
    private CoverageDataMigrationService migrationService;

    @InjectMocks
    private OptimizedDashboardController controller;

    private static final String TEST_REPO_PATH = "test/repo";
    private static final String TEST_BRANCH = "main";

    @BeforeEach
    void setUp() {
        // Setup any common mock behavior
    }

    @Test
    void testGetOptimizedCoverageData_Success() {
        // Given
        when(optimizedCoverageService.isDataMigrated(TEST_REPO_PATH, TEST_BRANCH)).thenReturn(true);
        
        var mockDashboardData = Map.of(
            "overallMetrics", Map.of("lineCoverage", 85.5),
            "fileCount", 50
        );
        when(optimizedCoverageService.getFastDashboardData(TEST_REPO_PATH, TEST_BRANCH))
            .thenReturn(mockDashboardData);

        // When
        ResponseEntity<?> response = controller.getOptimizedCoverageData(TEST_REPO_PATH, TEST_BRANCH);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        
        assertEquals("success", responseBody.get("status"));
        assertEquals(TEST_REPO_PATH, responseBody.get("repoPath"));
        assertEquals(TEST_BRANCH, responseBody.get("branch"));
        assertTrue((Boolean) responseBody.get("optimized"));
        assertEquals(mockDashboardData, responseBody.get("data"));
        
        verify(optimizedCoverageService).isDataMigrated(TEST_REPO_PATH, TEST_BRANCH);
        verify(optimizedCoverageService).getFastDashboardData(TEST_REPO_PATH, TEST_BRANCH);
        verifyNoInteractions(migrationService);
    }

    @Test
    void testGetOptimizedCoverageData_RequiresMigration() {
        // Given
        when(optimizedCoverageService.isDataMigrated(TEST_REPO_PATH, TEST_BRANCH)).thenReturn(false);
        when(migrationService.migrateRepository(TEST_REPO_PATH, TEST_BRANCH)).thenReturn(true);
        
        var mockDashboardData = Map.of(
            "overallMetrics", Map.of("lineCoverage", 85.5),
            "fileCount", 50
        );
        when(optimizedCoverageService.getFastDashboardData(TEST_REPO_PATH, TEST_BRANCH))
            .thenReturn(mockDashboardData);

        // When
        ResponseEntity<?> response = controller.getOptimizedCoverageData(TEST_REPO_PATH, TEST_BRANCH);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        verify(optimizedCoverageService).isDataMigrated(TEST_REPO_PATH, TEST_BRANCH);
        verify(migrationService).migrateRepository(TEST_REPO_PATH, TEST_BRANCH);
        verify(optimizedCoverageService).getFastDashboardData(TEST_REPO_PATH, TEST_BRANCH);
    }

    @Test
    void testGetOptimizedCoverageData_Error() {
        // Given
        when(optimizedCoverageService.isDataMigrated(TEST_REPO_PATH, TEST_BRANCH)).thenReturn(true);
        when(optimizedCoverageService.getFastDashboardData(TEST_REPO_PATH, TEST_BRANCH))
            .thenThrow(new RuntimeException("Database connection failed"));

        // When
        ResponseEntity<?> response = controller.getOptimizedCoverageData(TEST_REPO_PATH, TEST_BRANCH);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        
        assertEquals("error", responseBody.get("status"));
        assertTrue(responseBody.get("message").toString().contains("Database connection failed"));
    }

    @Test
    void testGetFileTree_Success() {
        // Given
        var mockFileTree = List.of(
            Map.of("name", "src", "type", "DIRECTORY", "children", List.of()),
            Map.of("name", "test", "type", "DIRECTORY", "children", List.of())
        );
        when(optimizedCoverageService.getFileTreeStructure(TEST_REPO_PATH, TEST_BRANCH, null, 2))
            .thenReturn(mockFileTree);

        // When
        ResponseEntity<?> response = controller.getFileTree(TEST_REPO_PATH, TEST_BRANCH, null, 2);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        
        assertEquals("success", responseBody.get("status"));
        assertEquals(mockFileTree, responseBody.get("tree"));
        assertEquals("", responseBody.get("parentPath"));
        assertEquals(2, responseBody.get("maxDepth"));
    }

    @Test
    void testGetFileDetails_Success() {
        // Given
        String filePath = "src/main/java/Example.java";
        Map<String, Object> mockFileDetails = Map.of(
            "fileName", "Example.java",
            "lineCoverage", 85.5,
            "branchCoverage", 70.0
        );
        when(optimizedCoverageService.getFileDetailsAsMap(TEST_REPO_PATH, TEST_BRANCH, filePath))
            .thenReturn(mockFileDetails);

        // When
        ResponseEntity<?> response = controller.getFileDetails(TEST_REPO_PATH, TEST_BRANCH, filePath);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        
        assertEquals("success", responseBody.get("status"));
        assertEquals(mockFileDetails, responseBody.get("file"));
        assertEquals(filePath, responseBody.get("filePath"));
    }

    @Test
    void testGetFileDetails_NotFound() {
        // Given
        String filePath = "src/main/java/NonExistent.java";
        when(optimizedCoverageService.getFileDetailsAsMap(TEST_REPO_PATH, TEST_BRANCH, filePath))
            .thenReturn(null);

        // When
        ResponseEntity<?> response = controller.getFileDetails(TEST_REPO_PATH, TEST_BRANCH, filePath);

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        
        assertEquals("error", responseBody.get("status"));
        assertTrue(responseBody.get("message").toString().contains("File not found"));
    }

    @Test
    void testSearchFiles_Success() {
        // Given
        String query = "Example";
        List<Map<String, Object>> mockSearchResults = List.of(
            Map.of("fileName", "Example.java", "path", "src/main/java/Example.java"),
            Map.of("fileName", "ExampleTest.java", "path", "src/test/java/ExampleTest.java")
        );
        when(optimizedCoverageService.searchFiles(TEST_REPO_PATH, TEST_BRANCH, query, 20))
            .thenReturn(mockSearchResults);

        // When
        ResponseEntity<?> response = controller.searchFiles(TEST_REPO_PATH, TEST_BRANCH, query, 20);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        
        assertEquals("success", responseBody.get("status"));
        assertEquals(mockSearchResults, responseBody.get("results"));
        assertEquals(query, responseBody.get("query"));
        assertEquals(2, responseBody.get("count"));
    }

    @Test
    void testMigrateCoverageData_Success() {
        // Given
        when(migrationService.migrateRepository(TEST_REPO_PATH, TEST_BRANCH)).thenReturn(true);

        // When
        ResponseEntity<?> response = controller.migrateCoverageData(TEST_REPO_PATH, TEST_BRANCH);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        
        assertEquals("success", responseBody.get("status"));
        assertTrue((Boolean) responseBody.get("migrated"));
        assertEquals("Migration completed successfully", responseBody.get("message"));
    }

    @Test
    void testMigrateCoverageData_NoDataToMigrate() {
        // Given
        when(migrationService.migrateRepository(TEST_REPO_PATH, TEST_BRANCH)).thenReturn(false);

        // When
        ResponseEntity<?> response = controller.migrateCoverageData(TEST_REPO_PATH, TEST_BRANCH);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        
        assertEquals("success", responseBody.get("status"));
        assertFalse((Boolean) responseBody.get("migrated"));
        assertEquals("No data to migrate", responseBody.get("message"));
    }

    @Test
    void testGetMigrationStatus_Success() {
        // Given
        when(optimizedCoverageService.isDataMigrated(TEST_REPO_PATH, TEST_BRANCH)).thenReturn(true);
        when(optimizedCoverageService.getCoverageNodeCount(TEST_REPO_PATH, TEST_BRANCH)).thenReturn(150L);

        // When
        ResponseEntity<?> response = controller.getMigrationStatus(TEST_REPO_PATH, TEST_BRANCH);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        
        assertEquals("success", responseBody.get("status"));
        assertTrue((Boolean) responseBody.get("migrated"));
        assertEquals(150L, responseBody.get("nodeCount"));
    }

    @Test
    void testClearOptimizedCache_Success() {
        // Given
        doNothing().when(optimizedCoverageService).clearCache(TEST_REPO_PATH, TEST_BRANCH);

        // When
        ResponseEntity<?> response = controller.clearOptimizedCache(TEST_REPO_PATH, TEST_BRANCH);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        
        assertEquals("success", responseBody.get("status"));
        assertEquals("Optimized cache cleared successfully", responseBody.get("message"));
        
        verify(optimizedCoverageService).clearCache(TEST_REPO_PATH, TEST_BRANCH);
    }

    @Test
    void testHealth_Success() {
        // When
        ResponseEntity<?> response = controller.health();

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        
        assertEquals("healthy", responseBody.get("status"));
        assertEquals("OptimizedDashboardController", responseBody.get("service"));
        assertEquals("v2", responseBody.get("version"));
        
        @SuppressWarnings("unchecked")
        List<String> features = (List<String>) responseBody.get("features");
        assertTrue(features.contains("flat_coverage_nodes"));
        assertTrue(features.contains("optimized_indexing"));
        assertTrue(features.contains("fast_tree_navigation"));
        assertTrue(features.contains("enterprise_scalability"));
    }
}
