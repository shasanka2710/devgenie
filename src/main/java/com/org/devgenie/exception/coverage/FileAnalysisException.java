package com.org.devgenie.exception.coverage;

public class FileAnalysisException extends RuntimeException {
    public FileAnalysisException(String message) {
        super(message);
    }

    public FileAnalysisException(String message, Throwable cause) {
        super(message, cause);
    }
}
