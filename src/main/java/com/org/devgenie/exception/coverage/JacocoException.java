package com.org.devgenie.exception.coverage;

public class JacocoException extends RuntimeException {
    public JacocoException(String message) {
        super(message);
    }

    public JacocoException(String message, Throwable cause) {
        super(message, cause);
    }
}
