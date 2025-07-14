package com.org.devgenie.exception.coverage;

public class JacocoException extends RuntimeException {
    private final String buildTool;
    private final String repoPath;

    public JacocoException(String message) {
        super(message);
        this.buildTool = null;
        this.repoPath = null;
    }

    public JacocoException(String message, Throwable cause) {
        super(message, cause);
        this.buildTool = null;
        this.repoPath = null;
    }

    public JacocoException(String message, String buildTool, String repoPath) {
        super(message);
        this.buildTool = buildTool;
        this.repoPath = repoPath;
    }

    public JacocoException(String message, Throwable cause, String buildTool, String repoPath) {
        super(message, cause);
        this.buildTool = buildTool;
        this.repoPath = repoPath;
    }

    public String getBuildTool() {
        return buildTool;
    }

    public String getRepoPath() {
        return repoPath;
    }

    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder(super.getMessage());

        if (buildTool != null) {
            sb.append(" [Build Tool: ").append(buildTool).append("]");
        }

        if (repoPath != null) {
            sb.append(" [Repository: ").append(repoPath).append("]");
        }

        return sb.toString();
    }
}
