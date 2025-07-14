package com.org.devgenie.exception.coverage;

public class CoverageException extends RuntimeException {
    public CoverageException(String message) {
        super(message);
    }

    public CoverageException(String message, Throwable cause) {
        super(message, cause);
    }
}
