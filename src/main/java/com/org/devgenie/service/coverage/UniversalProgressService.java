package com.org.devgenie.service.coverage;

import com.org.devgenie.dto.coverage.ProgressUpdate;
import com.org.devgenie.websocket.CoverageProgressWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Universal Progress Service - Handles any type of progress message dynamically
 * 
 * Usage Examples:
 * - progressService.info(sessionId, 25.0, "Scanning files", "FILE_SCAN");
 * - progressService.success(sessionId, 100.0, "All tests generated!");
 * - progressService.warning(sessionId, 75.0, "Some files skipped");
 * - progressService.error(sessionId, 0.0, "Failed to connect to repository");
 * - progressService.custom(sessionId, 50.0, "Custom operation", CUSTOM_TYPE, WARNING);
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UniversalProgressService {
    
    private final CoverageProgressWebSocketHandler webSocketHandler;
    
    /**
     * Send an info message (blue styling)
     */
    public void info(String sessionId, Double progress, String message) {
        sendProgress(sessionId, progress, message, ProgressUpdate.ProgressType.INFO, 
                    ProgressUpdate.MessageSeverity.INFO);
    }
    
    /**
     * Send an info message with custom category
     */
    public void info(String sessionId, Double progress, String message, String category) {
        sendProgress(sessionId, progress, message, ProgressUpdate.ProgressType.INFO, 
                    ProgressUpdate.MessageSeverity.INFO, category, null);
    }
    
    /**
     * Send a success message (green styling)
     */
    public void success(String sessionId, Double progress, String message) {
        sendProgress(sessionId, progress, message, ProgressUpdate.ProgressType.COMPLETION, 
                    ProgressUpdate.MessageSeverity.SUCCESS);
    }
    
    /**
     * Send a warning message (yellow styling)
     */
    public void warning(String sessionId, Double progress, String message) {
        sendProgress(sessionId, progress, message, ProgressUpdate.ProgressType.WARNING, 
                    ProgressUpdate.MessageSeverity.WARNING);
    }
    
    /**
     * Send an error message (red styling)
     */
    public void error(String sessionId, Double progress, String message) {
        sendProgress(sessionId, progress, message, ProgressUpdate.ProgressType.ERROR, 
                    ProgressUpdate.MessageSeverity.ERROR);
    }
    
    /**
     * Send a debug message (gray styling) - only shown in debug mode
     */
    public void debug(String sessionId, Double progress, String message) {
        sendProgress(sessionId, progress, message, ProgressUpdate.ProgressType.DEBUG, 
                    ProgressUpdate.MessageSeverity.DEBUG);
    }
    
    /**
     * Send analysis progress
     */
    public void analysis(String sessionId, Double progress, String message) {
        sendProgress(sessionId, progress, message, ProgressUpdate.ProgressType.ANALYSIS, 
                    ProgressUpdate.MessageSeverity.INFO);
    }
    
    /**
     * Send test generation progress
     */
    public void testGeneration(String sessionId, Double progress, String message) {
        sendProgress(sessionId, progress, message, ProgressUpdate.ProgressType.TEST_GENERATION, 
                    ProgressUpdate.MessageSeverity.INFO);
    }
    
    /**
     * Send validation progress
     */
    public void validation(String sessionId, Double progress, String message) {
        sendProgress(sessionId, progress, message, ProgressUpdate.ProgressType.VALIDATION, 
                    ProgressUpdate.MessageSeverity.INFO);
    }
    
    /**
     * Send completely custom message with any type and severity
     */
    public void custom(String sessionId, Double progress, String message, 
                      ProgressUpdate.ProgressType type, ProgressUpdate.MessageSeverity severity) {
        sendProgress(sessionId, progress, message, type, severity);
    }
    
    /**
     * Send custom message with additional data
     */
    public void customWithData(String sessionId, Double progress, String message, 
                              ProgressUpdate.ProgressType type, ProgressUpdate.MessageSeverity severity,
                              String category, Map<String, Object> additionalData) {
        sendProgress(sessionId, progress, message, type, severity, category, additionalData);
    }
    
    /**
     * Core method to send any progress update
     */
    private void sendProgress(String sessionId, Double progress, String message, 
                             ProgressUpdate.ProgressType type, ProgressUpdate.MessageSeverity severity) {
        sendProgress(sessionId, progress, message, type, severity, null, null);
    }
    
    /**
     * Most flexible method - handles any combination of parameters
     */
    private void sendProgress(String sessionId, Double progress, String message, 
                             ProgressUpdate.ProgressType type, ProgressUpdate.MessageSeverity severity,
                             String category, Map<String, Object> additionalData) {
        
        log.info("📡 Sending {} progress: {}% - {} (session: {})", 
                severity.name(), progress, message, sessionId);
        
        ProgressUpdate update = ProgressUpdate.builder()
                .sessionId(sessionId)
                .progress(progress)
                .message(message)
                .currentStep(message)
                .type(type)
                .severity(severity)
                .category(category)
                .additionalData(additionalData)
                .timestamp(java.time.LocalDateTime.now())
                .build();
        
        webSocketHandler.sendProgressUpdate(sessionId, update);
    }
}
