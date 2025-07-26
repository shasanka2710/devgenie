package com.org.devgenie.controller.coverage;

import com.org.devgenie.dto.coverage.*;
import com.org.devgenie.model.coverage.CoverageImprovementSession;
import com.org.devgenie.model.coverage.EnhancedRepoCoverageRequest;
import com.org.devgenie.model.coverage.RepositoryAnalysisRequest;
import com.org.devgenie.model.coverage.RepositoryAnalysisResponse;
import com.org.devgenie.model.coverage.WorkspaceStatusResponse;
import com.org.devgenie.service.coverage.AsyncCoverageProcessingService;
import com.org.devgenie.service.coverage.CoverageAgentService;
import com.org.devgenie.service.coverage.RepositoryAnalysisService;
import com.org.devgenie.service.coverage.RepositoryService;
import com.org.devgenie.service.coverage.SessionManagementService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/coverage")
@Slf4j
public class CoverageController {

    @Autowired
    private CoverageAgentService coverageAgentService;

    @Autowired
    private RepositoryAnalysisService repositoryAnalysisService;

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private SessionManagementService sessionManagementService;

    @Autowired
    private AsyncCoverageProcessingService asyncCoverageProcessingService;

    /**
     * NEW: Analyze repository and provide summary before coverage improvement
     * Sample Request:
     * {
     *   "repositoryUrl": "https://github.com/user/repo",
     *   "branch": "main",
     *   "githubToken": "ghp_xxxx" (optional for public repos)
     * }
     */
    @PostMapping("/analyze-repository")
    public ResponseEntity<RepositoryAnalysisResponse> analyzeRepository(@RequestBody RepositoryAnalysisRequest request) {
        try {
            RepositoryAnalysisResponse response = repositoryAnalysisService.analyzeRepository(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error analyzing repository", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(RepositoryAnalysisResponse.error(e.getMessage()));
        }
    }

    /**
     * Enhanced file coverage improvement with repository context
     * Sample Request:
     * {
     *   "repositoryUrl": "https://github.com/user/repo",
     *   "branch": "main",
     *   "filePath": "src/main/java/com/example/service/UserService.java",
     *   "targetCoverageIncrease": 25.0,
     *   "githubToken": "ghp_xxxx",
     *   "workspaceId": "user-workspace-123"
     * }
     */
    @PostMapping("/increase-file")
    public ResponseEntity<CoverageResponse> increaseFileCoverage(@RequestBody FileCoverageRequest request) {
        try {
            CoverageResponse response = coverageAgentService.increaseFileCoverage(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error increasing file coverage", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CoverageResponse.error(e.getMessage()));
        }
    }

    /**
     * Enhanced repository coverage improvement with auto-detection
     * Sample Request:
     * {
     *   "repositoryUrl": "https://github.com/user/repo",
     *   "branch": "main",
     *   "targetCoverageIncrease": 20.0,
     *   "githubToken": "ghp_xxxx",
     *   "workspaceId": "user-workspace-123",
     *   "excludePatterns": ["test/**", "generated/**"],
     *   "maxFilesToProcess": 10,
     *   "forceProjectDetection": false
     * }
     */
    @PostMapping("/increase-repo")
    public ResponseEntity<CoverageResponse> increaseRepoCoverage(@RequestBody RepoCoverageRequest request) {
        try {
            CoverageResponse response = coverageAgentService.increaseRepoCoverage(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error increasing repo coverage", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CoverageResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/apply-changes")
    public ResponseEntity<ApplyChangesResponse> applyChanges(@RequestBody ApplyChangesRequest request) {
        try {
            ApplyChangesResponse response = coverageAgentService.applyChanges(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error applying changes", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApplyChangesResponse.error(e.getMessage()));
        }
    }

    /**
     * Get workspace status and cached repository information
     */
    @GetMapping("/workspace/{workspaceId}/status")
    public ResponseEntity<WorkspaceStatusResponse> getWorkspaceStatus(@PathVariable String workspaceId) {
        try {
            WorkspaceStatusResponse response = repositoryService.getWorkspaceStatus(workspaceId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting workspace status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(WorkspaceStatusResponse.error(e.getMessage()));
        }
    }

    /**
     * Clean up workspace (remove cloned repository)
     */
    @DeleteMapping("/workspace/{workspaceId}")
    public ResponseEntity<Void> cleanupWorkspace(@PathVariable String workspaceId) {
        try {
            repositoryService.cleanupWorkspace(workspaceId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error cleaning up workspace", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get workspace status by repository URL and branch (optimized)
     */
    @GetMapping("/repository/status")
    public ResponseEntity<WorkspaceStatusResponse> getRepositoryStatus(
            @RequestParam String repositoryUrl,
            @RequestParam(required = false, defaultValue = "main") String branch) {
        try {
            WorkspaceStatusResponse response = repositoryService.getWorkspaceStatusByRepo(repositoryUrl, branch);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting repository status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(WorkspaceStatusResponse.error(e.getMessage()));
        }
    }

    /**
     * Clean up repository cache by URL and branch (optimized)
     */
    @DeleteMapping("/repository/cache")
    public ResponseEntity<Void> cleanupRepositoryCache(
            @RequestParam String repositoryUrl,
            @RequestParam(required = false, defaultValue = "main") String branch) {
        try {
            repositoryService.cleanupRepositoryCache(repositoryUrl, branch);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error cleaning up repository cache", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Enhanced file coverage improvement with session-based processing
     * Sample Request:
     * {
     *   "repositoryUrl": "https://github.com/user/repo",
     *   "branch": "main",
     *   "filePath": "src/main/java/com/example/service/UserService.java",
     *   "targetCoverageIncrease": 25.0,
     *   "githubToken": "ghp_xxxx",
     *   "workspaceId": "user-workspace-123",
     *   "maxTestsPerBatch": 5,
     *   "validateTests": true,
     *   "createPullRequest": false
     * }
     */
    @PostMapping("/file/improve-enhanced")
    public ResponseEntity<?> improveFileCoverageEnhanced(@RequestBody EnhancedFileCoverageRequest request) {
        try {
            FileCoverageImprovementResult result = coverageAgentService.improveFileCoverageEnhanced(request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error in enhanced file coverage improvement", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage(), "timestamp", LocalDateTime.now()));
        }
    }

    /**
     * Async file coverage improvement with real-time progress tracking
     * Returns immediately with a session ID for tracking progress via WebSocket
     * Sample Request:
     * {
     *   "repositoryUrl": "https://github.com/user/repo",
     *   "branch": "main",
     *   "filePath": "src/main/java/com/example/service/UserService.java",
     *   "targetCoverageIncrease": 25.0,
     *   "githubToken": "ghp_xxxx",
     *   "workspaceId": "user-workspace-123",
     *   "maxTestsPerBatch": 5,
     *   "validateTests": true,
     *   "createPullRequest": false
     * }
     */
    @PostMapping("/file/improve-async")
    public ResponseEntity<?> improveFileCoverageAsync(@RequestBody EnhancedFileCoverageRequest request) {
        try {
            String sessionId = asyncCoverageProcessingService.startFileCoverageImprovement(request);
            return ResponseEntity.ok(Map.of(
                "sessionId", sessionId,
                "message", "File coverage improvement started. Connect to WebSocket for real-time progress.",
                "websocketUrl", "/ws/coverage-progress/" + sessionId,
                "statusUrl", "/api/coverage/file/session/" + sessionId + "/status",
                "timestamp", LocalDateTime.now()
            ));
        } catch (Exception e) {
            log.error("Error starting async file coverage improvement", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage(), "timestamp", LocalDateTime.now()));
        }
    }

    /**
     * Async repository coverage improvement with batch processing
     * Returns immediately with a session ID for tracking progress via WebSocket
     * Sample Request:
     * {
     *   "repositoryUrl": "https://github.com/user/repo",
     *   "branch": "main",
     *   "targetCoverageIncrease": 20.0,
     *   "githubToken": "ghp_xxxx",
     *   "workspaceId": "user-workspace-123",
     *   "excludePatterns": ["test/**", "generated/**"],
     *   "maxFilesToProcess": 10,
     *   "forceProjectDetection": false,
     *   "maxTestsPerBatch": 5,
     *   "validateTests": true,
     *   "createPullRequest": false
     * }
     */
    @PostMapping("/repo/improve-async")
    public ResponseEntity<?> improveRepositoryCoverageAsync(@RequestBody EnhancedRepoCoverageRequest request) {
        try {
            String sessionId = asyncCoverageProcessingService.startRepositoryCoverageImprovement(request);
            return ResponseEntity.ok(Map.of(
                "sessionId", sessionId,
                "message", "Repository coverage improvement started. Connect to WebSocket for real-time progress.",
                "websocketUrl", "/ws/coverage-progress/" + sessionId,
                "statusUrl", "/api/coverage/repo/session/" + sessionId + "/status",
                "timestamp", LocalDateTime.now()
            ));
        } catch (Exception e) {
            log.error("Error starting async repository coverage improvement", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage(), "timestamp", LocalDateTime.now()));
        }
    }

    /**
     * Cancel an ongoing async coverage improvement session
     */
    @PostMapping("/session/{sessionId}/cancel")
    public ResponseEntity<?> cancelCoverageImprovementSession(@PathVariable String sessionId) {
        try {
            boolean cancelled = asyncCoverageProcessingService.cancelSession(sessionId);
            if (cancelled) {
                return ResponseEntity.ok(Map.of(
                    "sessionId", sessionId,
                    "message", "Session cancelled successfully",
                    "timestamp", LocalDateTime.now()
                ));
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Error cancelling session", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get session status for file coverage improvement
     */
    @GetMapping("/file/session/{sessionId}/status")
    public ResponseEntity<?> getFileCoverageSessionStatus(@PathVariable String sessionId) {
        try {
            Optional<CoverageImprovementSession> sessionOpt = sessionManagementService.getSession(sessionId);
            if (sessionOpt.isPresent()) {
                return ResponseEntity.ok(sessionOpt.get());
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Error getting session status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get session results for file coverage improvement
     */
    @GetMapping("/file/session/{sessionId}/results")
    public ResponseEntity<?> getFileCoverageSessionResults(@PathVariable String sessionId) {
        try {
            Optional<CoverageImprovementSession> sessionOpt = sessionManagementService.getSession(sessionId);
            if (sessionOpt.isPresent()) {
                CoverageImprovementSession session = sessionOpt.get();

                // Check if session has results and is in a completed state
                boolean hasResults = session.getResults() != null;
                boolean isCompletedState = session.getStatus() == CoverageImprovementSession.SessionStatus.COMPLETED ||
                                         session.getStatus() == CoverageImprovementSession.SessionStatus.READY_FOR_REVIEW ||
                                         session.getStatus() == CoverageImprovementSession.SessionStatus.PARTIALLY_COMPLETED;

                if (hasResults && isCompletedState) {
                    return ResponseEntity.ok(Map.of(
                        "sessionId", sessionId,
                        "status", session.getStatus(),
                        "results", session.getResults(),
                        "startedAt", session.getStartedAt(),
                        "progress", session.getProgress()
                    ));
                } else if (session.getStatus() == CoverageImprovementSession.SessionStatus.FAILED) {
                    return ResponseEntity.ok(Map.of(
                        "sessionId", sessionId,
                        "status", session.getStatus(),
                        "errors", session.getErrors()
                    ));
                } else {
                    return ResponseEntity.ok(Map.of(
                        "sessionId", sessionId,
                        "status", session.getStatus(),
                        "progress", session.getProgress(),
                        "currentStep", session.getCurrentStep(),
                        "hasResults", hasResults,
                        "message", hasResults ? "Results are available" : "Results are not yet available. Please wait for the process to complete."
                    ));
                }
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Error getting session results", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get repository coverage session status (for async repo improvement)
     */
    @GetMapping("/repo/session/{sessionId}/status")
    public ResponseEntity<?> getRepositoryCoverageSessionStatus(@PathVariable String sessionId) {
        try {
            Optional<CoverageImprovementSession> sessionOpt = sessionManagementService.getSession(sessionId);
            if (sessionOpt.isPresent()) {
                return ResponseEntity.ok(sessionOpt.get());
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Error getting repository session status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Apply generated test changes for a file coverage improvement session
     */
    @PostMapping("/file/session/{sessionId}/apply")
    public ResponseEntity<?> applyFileCoverageChanges(
            @PathVariable String sessionId,
            @RequestBody ApplyChangesRequest request) {
        try {
            request.setSessionId(sessionId); // Ensure session ID is set
            ApplyChangesResponse response = coverageAgentService.applyChanges(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error applying file coverage changes", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Apply generated test changes for a repository coverage improvement session
     */
    @PostMapping("/repo/session/{sessionId}/apply")
    public ResponseEntity<?> applyRepositoryCoverageChanges(
            @PathVariable String sessionId,
            @RequestBody ApplyChangesRequest request) {
        try {
            request.setSessionId(sessionId); // Ensure session ID is set
            ApplyChangesResponse response = coverageAgentService.applyChanges(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error applying repository coverage changes", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
