package com.org.devgenie.service.coverage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Unit tests for the {@link CoverageScheduledTasks} class.
 * These tests focus on verifying that the scheduled methods execute without errors
 * and handle exceptions as expected, mocking external dependencies.
 */
@ExtendWith(MockitoExtension.class)
class CoverageScheduledTasksTest {

    @InjectMocks
    private CoverageScheduledTasks coverageScheduledTasks;

    @Mock
    private CoverageDataService coverageDataService;

    @Mock
    private JacocoService jacocoService;

    /**
     * Tests the happy path for the cleanupOldCoverageData method.
     * Verifies that the method executes without throwing an exception
     * and that no interactions occur with the mocked services (as per current implementation).
     */
    @Test
    void testCleanupOldCoverageData_success() {
        // Arrange - No specific arrangement needed for happy path beyond mocks

        // Act
        coverageScheduledTasks.cleanupOldCoverageData();

        // Assert
        // Verify that no exceptions were thrown
        // Verify that the autowired services were not interacted with directly by this method
        verifyNoInteractions(coverageDataService);
        verifyNoInteractions(jacocoService);
    }

    /**
     * Tests the exception handling for the cleanupOldCoverageData method.
     * Simulates an internal error (e.g., during date calculation) to ensure
     * the method catches the exception and does not rethrow it.
     */
    @Test
    void testCleanupOldCoverageData_handlesException() {
        // Arrange
        // Mock LocalDateTime.now() to throw an exception to simulate an internal error
        try (MockedStatic<LocalDateTime> mockedStatic = Mockito.mockStatic(LocalDateTime.class)) {
            mockedStatic.when(LocalDateTime::now).thenThrow(new RuntimeException("Simulated date calculation error"));

            // Act
            // The method should catch the RuntimeException internally
            coverageScheduledTasks.cleanupOldCoverageData();

            // Assert
            // Verify that no exceptions were propagated out of the method
            // Verify that the autowired services were not interacted with directly by this method
            verifyNoInteractions(coverageDataService);
            verifyNoInteractions(jacocoService);
        } // The mockedStatic is closed automatically here
    }

    /**
     * Tests the happy path for the refreshCoverageCache method.
     * Verifies that the method executes without throwing an exception
     * and that no interactions occur with the mocked services (as per current implementation).
     */
    @Test
    void testRefreshCoverageCache_success() {
        // Arrange - No specific arrangement needed for happy path beyond mocks

        // Act
        coverageScheduledTasks.refreshCoverageCache();

        // Assert
        // Verify that no exceptions were thrown
        // Verify that the autowired services were not interacted with directly by this method
        verifyNoInteractions(coverageDataService);
        verifyNoInteractions(jacocoService);
    }
}