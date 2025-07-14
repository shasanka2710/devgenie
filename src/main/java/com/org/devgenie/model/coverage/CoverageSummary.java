package com.org.devgenie.model.coverage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoverageSummary {
    private String sessionId;
    private String workspaceId;
    private Type type;
    private String filePath;
    private double originalCoverage;
    private double estimatedCoverage;
    private int testsAdded;
    private int filesProcessed;
    private int failedFiles;
    private List<FileChange> changes;
    private LocalDateTime timestamp;
    private ProjectConfiguration projectConfiguration;

    public enum Type {
        FILE, REPOSITORY
    }
}
