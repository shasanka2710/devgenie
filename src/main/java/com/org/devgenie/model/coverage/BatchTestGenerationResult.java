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
public class BatchTestGenerationResult {
    private Boolean success;
    private List<GeneratedTestInfo> generatedTests;
    private List<String> testFilePaths;
    private String error;
    private Integer batchIndex;
    private Integer testCount;
    
    public static BatchTestGenerationResult success(List<GeneratedTestInfo> tests, List<String> paths) {
        return BatchTestGenerationResult.builder()
                .success(true)
                .generatedTests(tests)
                .testFilePaths(paths)
                .build();
    }
    
    public static BatchTestGenerationResult failure(String error) {
        return BatchTestGenerationResult.builder()
                .success(false)
                .error(error)
                .build();
    }
}
