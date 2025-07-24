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
public class TestExecutionResult {
    private Boolean success;
    private Integer testsExecuted;
    private Integer testsPassed;
    private Integer testsFailed;
    private List<String> errors;
    private Long executionTimeMs;
}
