package com.org.devgenie.model.coverage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileChange {
    private String filePath;
    private String testFilePath;
    private Type changeType;
    private String description;
    private String content;

    public enum Type {
        TEST_ADDED, TEST_MODIFIED, SOURCE_MODIFIED
    }
}
