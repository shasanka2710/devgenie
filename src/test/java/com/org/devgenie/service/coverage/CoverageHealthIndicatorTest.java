package com.org.devgenie.service.coverage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.Builder;
import org.springframework.ai.chat.client.ChatClient.CallPromptResponse;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the CoverageHealthIndicator class.
 * This class tests the health check logic for MongoDB and AI services
 * by mocking its external dependencies.
 */
@ExtendWith(MockitoExtension.class)
class CoverageHealthIndicatorTest {

    @InjectMocks
    private CoverageHealthIndicator coverageHealthIndicator;

    @Mock
    private CoverageDataService coverageDataService;

    @Mock
    private ChatClient chatClient;

    // Mocks for the ChatClient chain to control its behavior
    @Mock
    private Builder chatClientBuilder;
    @Mock
    private CallPromptResponse callPromptResponse;

    @BeforeEach
    void setUp() {
        // Common setup for mocking the ChatClient chain:
        // chatClient.prompt(anyString()) returns a Builder
        when(chatClient.prompt(anyString())).thenReturn(chatClientBuilder);
        // Builder.call() returns a CallPromptResponse
        when(chatClientBuilder.call()).thenReturn(callPromptResponse);
    }

    /**
     * Tests the health indicator when both MongoDB and AI services are fully functional.
     * Ensures the Health status is UP and correct details are present.
     */
    @Test
    void testHealth_UpWhenAllServicesAreUp() {
        // Arrange
        // Simulate MongoDB service being healthy (no exception on call)
        doNothing().when(coverageDataService).getCurrentCoverage(anyString(), anyString(), anyString());
        // Simulate AI service responding correctly (containing "OK")
        when(callPromptResponse.content()).thenReturn("Hello, this is OK.");

        // Act
        Health health = coverageHealthIndicator.health();

        // Assert
        assertEquals(Status.UP, health.getStatus(), "Health status should be UP when all services are functional.");
        assertEquals("UP", health.getDetails().get("mongodb"), "MongoDB detail should be 'UP'.");
        assertEquals("UP", health.getDetails().get("ai-service"), "AI service detail should be 'UP'.");

        // Verify interactions with dependencies
        verify(coverageDataService, times(1)).getCurrentCoverage("", "health-check", "main");
        verify(chatClient, times(1)).prompt("Say 'OK' if you can respond");
        verify(chatClientBuilder, times(1)).call();
        verify(callPromptResponse, times(1)).content();
    }

    /**
     * Tests the health indicator when the AI service responds but does not contain "OK".
     * Ensures the Health status is DOWN with specific AI service error detail.
     */
    @Test
    void testHealth_DownWhenAIServiceRespondsIncorrectly() {
        // Arrange
        // Simulate MongoDB service being healthy
        doNothing().when(coverageDataService).getCurrentCoverage(anyString(), anyString(), anyString());
        // Simulate AI service responding incorrectly (e.g., "Not good")
        when(callPromptResponse.content()).thenReturn("I am not doing well.");

        // Act
        Health health = coverageHealthIndicator.health();

        // Assert
        assertEquals(Status.DOWN, health.getStatus(), "Health status should be DOWN when AI service responds incorrectly.");
        assertNull(health.getDetails().get("mongodb"), "MongoDB detail should not be present in AI service down case.");
        assertEquals("AI service not responding correctly", health.getDetails().get("ai-service"), "AI service detail should indicate incorrect response.");
        assertFalse(health.getDetails().containsKey("error"), "Error detail should not be present when AI service responds incorrectly.");

        // Verify interactions with dependencies
        verify(coverageDataService, times(1)).getCurrentCoverage("", "health-check", "main");
        verify(chatClient, times(1)).prompt("Say 'OK' if you can respond");
        verify(chatClientBuilder, times(1)).call();
        verify(callPromptResponse, times(1)).content();
    }

    /**
     * Tests the health indicator when the MongoDB service throws an exception.
     * Ensures the Health status is DOWN with a general error detail.
     */
    @Test
    void testHealth_DownWhenMongoDBServiceThrowsException() {
        // Arrange
        String errorMessage = "MongoDB connection failed unexpectedly.";
        // Simulate MongoDB service throwing an exception
        doThrow(new RuntimeException(errorMessage)).when(coverageDataService).getCurrentCoverage(anyString(), anyString(), anyString());
        // No need to mock AI service response as the flow should exit early

        // Act
        Health health = coverageHealthIndicator.health();

        // Assert
        assertEquals(Status.DOWN, health.getStatus(), "Health status should be DOWN when MongoDB service throws an exception.");
        assertTrue(health.getDetails().containsKey("error"), "Health details should contain an 'error' key.");
        assertEquals(errorMessage, health.getDetails().get("error"), "Error message should match the thrown exception message.");
        assertNull(health.getDetails().get("mongodb"), "MongoDB detail should not be present when it fails.");
        assertNull(health.getDetails().get("ai-service"), "AI service detail should not be present as it was not checked.");

        // Verify interactions with dependencies
        verify(coverageDataService, times(1)).getCurrentCoverage("", "health-check", "main");
        // Ensure AI service related methods were NOT called
        verify(chatClient, never()).prompt(anyString());
        verify(chatClientBuilder, never()).call();
        verify(callPromptResponse, never()).content();
    }

    /**
     * Tests the health indicator when the AI service throws an exception during its call.
     * Ensures the Health status is DOWN with a general error detail.
     */
    @Test
    void testHealth_DownWhenAIServiceThrowsException() {
        // Arrange
        String errorMessage = "AI service API call failed.";
        // Simulate MongoDB service being healthy
        doNothing().when(coverageDataService).getCurrentCoverage(anyString(), anyString(), anyString());
        // Simulate AI service throwing an exception when getting content
        doThrow(new RuntimeException(errorMessage)).when(callPromptResponse).content();

        // Act
        Health health = coverageHealthIndicator.health();

        // Assert
        assertEquals(Status.DOWN, health.getStatus(), "Health status should be DOWN when AI service throws an exception.");
        assertTrue(health.getDetails().containsKey("error"), "Health details should contain an 'error' key.");
        assertEquals(errorMessage, health.getDetails().get("error"), "Error message should match the thrown exception message.");
        assertNull(health.getDetails().get("mongodb"), "MongoDB detail should not be present in error case.");
        assertNull(health.getDetails().get("ai-service"), "AI service detail should not be present in error case.");

        // Verify interactions with dependencies
        verify(coverageDataService, times(1)).getCurrentCoverage("", "health-check", "main");
        verify(chatClient, times(1)).prompt("Say 'OK' if you can respond");
        verify(chatClientBuilder, times(1)).call();
        verify(callPromptResponse, times(1)).content(); // This is the call that throws the exception
    }
}