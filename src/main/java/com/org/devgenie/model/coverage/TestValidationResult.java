package com.org.devgenie.model.coverage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestValidationResult {
    private Boolean success;
    private Integer testsExecuted;
    private Integer testsPassed;
    private Integer testsFailed;
    private List<String> compilationErrors;
    private List<String> executionErrors;
    private String validationMethod; // COMPILATION_ONLY, EXECUTION, COVERAGE_ANALYSIS
    private Long executionTimeMs;
}
