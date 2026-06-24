package com.org.devgenie.service.coverage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the {@link CoverageScheduledTasks} class.
 * These tests focus on the business logic within the scheduled methods,
 * mocking external dependencies and static logger behavior.
 * 
 * The tests assume that the comment "// Implementation would depend on your MongoDB repository"
 * implies a call to `coverageDataService` within the `cleanupOldCoverageData` method's try block.
 * This allows for comprehensive testing of the exception handling path.
 */
@ExtendWith(MockitoExtension.class)
class CoverageScheduledTasksTest {

    @InjectMocks
    private CoverageScheduledTasks coverageScheduledTasks;

    @Mock
    private CoverageDataService coverageDataService;

    @Mock
    private JacocoService jacocoService;

    // Mock for the static Slf4j logger
    @Mock
    private Logger mockLogger;

    private MockedStatic<LoggerFactory> mockedStaticLoggerFactory;

    @BeforeEach
    void setUp() {
        // Mock the static LoggerFactory to return our mockLogger
        mockedStaticLoggerFactory = mockStatic(LoggerFactory.class);
        when(LoggerFactory.getLogger(any(Class.class))).thenReturn(mockLogger);

        // Verify that the logger is obtained for the correct class (optional, but good practice)
        mockedStaticLoggerFactory.verify(() -> LoggerFactory.getLogger(CoverageScheduledTasks.class));
    }

    @AfterEach
    void tearDown() {
        // Close the mocked static LoggerFactory
        if (mockedStaticLoggerFactory != null) {
            mockedStaticLoggerFactory.close();
        }
    }

    @Test
    @DisplayName("Should successfully clean up old coverage data")
    void testCleanupOldCoverageData_success() {
        // Arrange
        // Assume a method call to coverageDataService for cleanup, as hinted by the source comment.
        // We just need to verify it's called; no specific return value needed for success.
        doNothing().when(coverageDataService).deleteOldData(any(LocalDateTime.class));

        // Act
        coverageScheduledTasks.cleanupOldCoverageData();

        // Assert
        verify(mockLogger).info("Starting cleanup of old coverage data");

        ArgumentCaptor<LocalDateTime> cutoffDateCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(mockLogger).info(eq("Cleaned up coverage data older than {}"), cutoffDateCaptor.capture());

        // Verify that deleteOldData was called with a date approximately 30 days ago
        // This assertion checks if the captured date is roughly 30 days prior to now.
        LocalDateTime capturedCutoffDate = cutoffDateCaptor.getValue();
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        // Allow a small window for execution time differences
        assertTrue(capturedCutoffDate.isAfter(thirtyDaysAgo.minusMinutes(1)) &&
                   capturedCutoffDate.isBefore(thirtyDaysAgo.plusMinutes(1)),
                   "Cutoff date should be approximately 30 days ago");

        // Verify interaction with the hypothetical service method
        verify(coverageDataService).deleteOldData(any(LocalDateTime.class));

        // Ensure jacocoService is not interacted with
        verifyNoInteractions(jacocoService);
    }

    @Test
    @DisplayName("Should handle exceptions during cleanup of old coverage data")
    void testCleanupOldCoverageData_exception() {
        // Arrange
        RuntimeException testException = new RuntimeException("Simulated cleanup error");
        // Simulate an exception when the hypothetical deleteOldData is called
        doThrow(testException).when(coverageDataService).deleteOldData(any(LocalDateTime.class));

        // Act
        coverageScheduledTasks.cleanupOldCoverageData();

        // Assert
        verify(mockLogger).info("Starting cleanup of old coverage data");

        // Verify that deleteOldData was attempted (and threw an exception)
        verify(coverageDataService).deleteOldData(any(LocalDateTime.class));

        // Verify the error log
        verify(mockLogger).error(eq("Failed to cleanup old coverage data"), eq(testException));

        // Ensure no other info log about cleanup success when an exception occurs
        verify(mockLogger, never()).info(eq("Cleaned up coverage data older than {}"), any(LocalDateTime.class));

        // Ensure jacocoService is not interacted with
        verifyNoInteractions(jacocoService);
    }

    @Test
    @DisplayName("Should successfully refresh coverage cache")
    void testRefreshCoverageCache_success() {
        // Arrange - No specific arrangements needed for this simple method

        // Act
        coverageScheduledTasks.refreshCoverageCache();

        // Assert
        verify(mockLogger).debug("Refreshing coverage cache");

        // Ensure no interactions with services
        verifyNoInteractions(coverageDataService);
        verifyNoInteractions(jacocoService);
    }
}