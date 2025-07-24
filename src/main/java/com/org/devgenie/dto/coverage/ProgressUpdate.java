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

    public enum ProgressType {
        INITIALIZATION,
        ANALYSIS,
        TEST_GENERATION,
        VALIDATION,
        COMPLETION,
        ERROR
    }
}
