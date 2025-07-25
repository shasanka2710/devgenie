package com.org.devgenie.dto.coverage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgressUpdate {
    private String sessionId;
    private Double progress; // 0-100
    private String currentStep;
    private String message;
    private LocalDateTime timestamp;
    private Integer currentBatch;
    private Integer totalBatches;
    private Integer processedFiles;
    private Integer totalFiles;
    private Map<String, Object> stepData;
    private ProgressType type;
    private MessageSeverity severity; // NEW: For different UI styling
    private String category; // NEW: For grouping messages
    private Map<String, Object> additionalData; // NEW: For extra context

    public enum ProgressType {
        INITIALIZATION,
        ANALYSIS,
        TEST_GENERATION,
        VALIDATION,
        COMPLETION,
        ERROR,
        WARNING,
        INFO,
        DEBUG,
        // Add any new types here and frontend will handle automatically
        CUSTOM
    }
    
    public enum MessageSeverity {
        INFO,    // Blue styling
        SUCCESS, // Green styling  
        WARNING, // Yellow styling
        ERROR,   // Red styling
        DEBUG    // Gray styling
    }
    
    // Helper method to create progress updates easily
    public static ProgressUpdate create(String sessionId, Double progress, String message, 
                                      ProgressType type, MessageSeverity severity) {
        return ProgressUpdate.builder()
                .sessionId(sessionId)
                .progress(progress)
                .message(message)
                .currentStep(message)
                .type(type)
                .severity(severity != null ? severity : MessageSeverity.INFO)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
