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
public class TestGenerationResult {
    private String sourceFilePath;
    private String testFilePath;
    private String testClassName;
    private List<GeneratedTest> generatedTests;
    private List<String> imports;
    private List<String> mockDependencies;
    private String generatedTestContent;
    private double estimatedCoverageIncrease;
    private String notes;
    private boolean success;
    private String error;

    public static TestGenerationResult failure(String filePath, String error) {
        return TestGenerationResult.builder()
                .sourceFilePath(filePath)
                .success(false)
                .error(error)
                .build();
    }
}
