package com.org.devgenie.exception.coverage;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(CoverageException.class)
    public ResponseEntity<ErrorResponse> handleCoverageException(CoverageException e) {
        log.error("Coverage exception occurred", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("COVERAGE_ERROR", e.getMessage()));
    }

    @ExceptionHandler(FileAnalysisException.class)
    public ResponseEntity<ErrorResponse> handleFileAnalysisException(FileAnalysisException e) {
        log.error("File analysis exception occurred", e);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("FILE_ANALYSIS_ERROR", e.getMessage()));
    }

    @ExceptionHandler(JacocoException.class)
    public ResponseEntity<ErrorResponse> handleJacocoException(JacocoException e) {
        log.error("Jacoco exception occurred", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("JACOCO_ERROR", e.getMessage()));
    }

    @ExceptionHandler(GitException.class)
    public ResponseEntity<ErrorResponse> handleGitException(GitException e) {
        log.error("Git exception occurred", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("GIT_ERROR", e.getMessage()));
    }

    @ExceptionHandler(CoverageDataNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCoverageDataNotFoundException(CoverageDataNotFoundException e) {
        log.error("Coverage data not found", e);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("DATA_NOT_FOUND", e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception e) {
        log.error("Unexpected exception occurred", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred"));
    }

    @Data
    @AllArgsConstructor
    public static class ErrorResponse {
        private String code;
        private String message;
    }
}
