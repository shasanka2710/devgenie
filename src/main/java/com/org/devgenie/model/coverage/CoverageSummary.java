package com.org.devgenie.model.coverage;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoverageSummary {
    private String sessionId;
    private Type type;
    private String filePath;
    private double originalCoverage;
    private double estimatedCoverage;
    private int testsAdded;
    private int filesProcessed;
    private int failedFiles;
    private List<FileChange> changes;
    private LocalDateTime timestamp;

    public enum Type {
        FILE, REPOSITORY
    }
}
