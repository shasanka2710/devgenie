package com.org.devgenie.model.coverage;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProcessingResult {
    private Map<String, TestGenerationResult> successfulFiles = new HashMap<>();
    private Map<String, String> failedFiles = new HashMap<>();

    public void addSuccessfulFile(String filePath, TestGenerationResult result) {
        successfulFiles.put(filePath, result);
    }

    public void addFailedFile(String filePath, String error) {
        failedFiles.put(filePath, error);
    }

    public int getTotalTestsAdded() {
        return successfulFiles.values().stream()
                .mapToInt(result -> result.getGeneratedTests().size())
                .sum();
    }

    public List<FileChange> getAllChanges() {
        return successfulFiles.values().stream()
                .map(this::createFileChange)
                .collect(Collectors.toList());
    }

    private FileChange createFileChange(TestGenerationResult result) {
        return FileChange.builder()
                .filePath(result.getSourceFilePath())
                .testFilePath(result.getTestFilePath())
                .changeType(FileChange.Type.TEST_ADDED)
                .description("Added " + result.getGeneratedTests().size() + " test methods")
                .content(result.getGeneratedTestContent())
                .build();
    }
}