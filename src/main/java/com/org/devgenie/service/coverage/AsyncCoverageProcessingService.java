package com.org.devgenie.service.coverage;

import com.org.devgenie.dto.coverage.EnhancedFileCoverageRequest;
import com.org.devgenie.dto.coverage.FileCoverageImprovementResult;
import com.org.devgenie.dto.coverage.ProgressUpdate;
import com.org.devgenie.dto.coverage.RepositoryCoverageImprovementResult;
import com.org.devgenie.model.coverage.CoverageImprovementSession;
import com.org.devgenie.model.coverage.EnhancedRepoCoverageRequest;
import com.org.devgenie.websocket.CoverageProgressWebSocketHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class AsyncCoverageProcessingService {

    @Autowired
    private CoverageAgentService coverageAgentService;

    @Autowired
    private SessionManagementService sessionManagementService;

    @Autowired
    private CoverageProgressWebSocketHandler webSocketHandler;
    
    @Autowired
    private UniversalProgressService progressService; // NEW: Universal progress service

    @Autowired
    private RepositoryService repositoryService;

    // Track running sessions for cancellation
    private final ConcurrentHashMap<String, CompletableFuture<?>> runningSessions = new ConcurrentHashMap<>();

    /**
     * Start file coverage improvement and return session ID immediately
     */
    public String startFileCoverageImprovement(EnhancedFileCoverageRequest request) {
        // Create session using SessionManagementService (with optional provided session ID)
        CoverageImprovementSession session = sessionManagementService.createSession(
            request.getSessionId(), // Pass the session ID from request (may be null)
            request.getRepositoryUrl(),
            request.getBranch(),
            request.getFilePath(),
            CoverageImprovementSession.SessionType.FILE_IMPROVEMENT
        );
        
        // Start async processing
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> 
            processFileCoverageInBackground(session.getSessionId(), request));
        
        runningSessions.put(session.getSessionId(), future);
        
        // Remove from tracking when completed
        future.whenComplete((result, throwable) -> runningSessions.remove(session.getSessionId()));
        
        return session.getSessionId();
    }

    /**
     * Start repository coverage improvement and return session ID immediately
     */
    public String startRepositoryCoverageImprovement(EnhancedRepoCoverageRequest request) {
        log.info("🔍 Starting repository coverage improvement. Frontend provided sessionId: {}", request.getSessionId());
        
        // Create session using SessionManagementService (with optional provided session ID)
        CoverageImprovementSession session = sessionManagementService.createSession(
            request.getSessionId(), // Pass the session ID from request (may be null)
            request.getRepositoryUrl(),
            request.getBranch(),
            null, // No specific file for repo-wide improvement
            CoverageImprovementSession.SessionType.REPOSITORY_IMPROVEMENT
        );
        
        log.info("🔍 Created session with final sessionId: {}", session.getSessionId());
        
        // Start async processing
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> 
            processRepositoryCoverageInBackground(session.getSessionId(), request));
        
        runningSessions.put(session.getSessionId(), future);
        
        // Remove from tracking when completed
        future.whenComplete((result, throwable) -> runningSessions.remove(session.getSessionId()));
        
        return session.getSessionId();
    }

    /**
     * Cancel a running session
     */
    public boolean cancelSession(String sessionId) {
        CompletableFuture<?> future = runningSessions.get(sessionId);
        if (future != null) {
            boolean cancelled = future.cancel(true);
            if (cancelled) {
                sessionManagementService.updateSessionStatus(sessionId, 
                    CoverageImprovementSession.SessionStatus.CANCELLED);
                runningSessions.remove(sessionId);
            }
            return cancelled;
        }
        return false;
    }

    /**
     * Process file coverage improvement asynchronously
     */
    @Async("coverageImprovementExecutor")
    public CompletableFuture<FileCoverageImprovementResult> processFileCoverageAsync(
            String sessionId, EnhancedFileCoverageRequest request) {
        
        log.info("Starting async file coverage processing for session: {}", sessionId);
        
        try {
            // Update session status
            sessionManagementService.updateSessionStatus(sessionId, 
                    CoverageImprovementSession.SessionStatus.ANALYZING_FILE);
            
            // Process the file coverage improvement
            FileCoverageImprovementResult result = coverageAgentService.improveFileCoverageEnhanced(request);
            
            // Update session with final status
            sessionManagementService.updateSessionStatus(sessionId, 
                    CoverageImprovementSession.SessionStatus.READY_FOR_REVIEW);
            
            log.info("Async file coverage processing completed for session: {}", sessionId);
            return CompletableFuture.completedFuture(result);
            
        } catch (Exception e) {
            log.error("Async file coverage processing failed for session: {}", sessionId, e);
            sessionManagementService.handleError(sessionId, e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Alternative method that returns session ID immediately and processes in background
     */
    @Async("coverageImprovementExecutor")
    public void processFileCoverageInBackground(String sessionId, EnhancedFileCoverageRequest request) {
        
        log.info("Starting background file coverage processing for session: {}", sessionId);
        
        try {
            // Update session status
            sessionManagementService.updateSessionStatus(sessionId, 
                    CoverageImprovementSession.SessionStatus.ANALYZING_FILE);
            
            // Process the file coverage improvement
            FileCoverageImprovementResult result = coverageAgentService.improveFileCoverageEnhanced(request);
            
            // Store results in session
            sessionManagementService.setSessionResults(sessionId, result);
            
            // Update session with final status
            sessionManagementService.updateSessionStatus(sessionId, 
                    CoverageImprovementSession.SessionStatus.READY_FOR_REVIEW);
            
            // Send completion message after all operations are done
            sendProgressUpdate(sessionId, 100.0, "File improvement complete! Results are ready for review.", ProgressUpdate.ProgressType.COMPLETION);
            
            log.info("Background file coverage processing completed for session: {}", sessionId);
            
        } catch (Exception e) {
            log.error("Background file coverage processing failed for session: {}", sessionId, e);
            sessionManagementService.handleError(sessionId, e);
        }
    }

    /**
     * Process repository coverage improvement in background
     */
    @Async("coverageImprovementExecutor")
    public void processRepositoryCoverageInBackground(String sessionId, EnhancedRepoCoverageRequest request) {
        
        log.info("Starting background repository coverage processing for session: {}", sessionId);
        
        try {
            // Update session status
            sessionManagementService.updateSessionStatus(sessionId, 
                    CoverageImprovementSession.SessionStatus.ANALYZING_REPOSITORY);
            
            // Send initial progress update
            // NEW: Using Universal Progress Service - much cleaner!
            progressService.analysis(sessionId, 5.0, "Preparing workspace for analysis");
            
            // OLD way (still works but verbose):
            // sendProgressUpdate(sessionId, 5.0, "Preparing workspace for analysis", ProgressUpdate.ProgressType.ANALYSIS);
            
            // Setup repository workspace
            String workspaceDir = repositoryService.setupWorkspace(request.getRepositoryUrl(), 
                    request.getBranch(), request.getGithubToken());
            
            progressService.analysis(sessionId, 15.0, "Scanning project files");
            
            // Get list of Java files for coverage improvement
            List<String> javaFiles = repositoryService.findJavaFiles(workspaceDir, request.getExcludePatterns());
            
            if (javaFiles.isEmpty()) {
                sendProgressUpdate(sessionId, 100.0, "No Java files found that need coverage improvement", ProgressUpdate.ProgressType.COMPLETION);
                sessionManagementService.updateSessionStatus(sessionId, 
                        CoverageImprovementSession.SessionStatus.READY_FOR_REVIEW);
                return;
            }
            
            // Limit files if maxFilesToProcess is specified
            if (request.getMaxFilesToProcess() != null && request.getMaxFilesToProcess() > 0) {
                javaFiles = javaFiles.subList(0, Math.min(javaFiles.size(), request.getMaxFilesToProcess()));
            }
            
            log.info("Processing {} Java files for coverage improvement", javaFiles.size());
            sendProgressUpdate(sessionId, 25.0, String.format("Analyzing %d files for coverage opportunities", javaFiles.size()), ProgressUpdate.ProgressType.ANALYSIS);
            
            // Process files in batches
            int batchSize = 5; // Process 5 files at a time
            List<FileCoverageImprovementResult> allResults = new ArrayList<>();
            int totalFiles = javaFiles.size();
            int processedFiles = 0;
            
            for (int i = 0; i < javaFiles.size(); i += batchSize) {
                // Check if session was cancelled
                if (Thread.currentThread().isInterrupted()) {
                    log.info("Repository coverage processing cancelled for session: {}", sessionId);
                    return;
                }
                
                int endIndex = Math.min(i + batchSize, javaFiles.size());
                List<String> batch = javaFiles.subList(i, endIndex);
                
                log.info("Processing batch {}/{} with {} files", (i/batchSize) + 1, 
                        (javaFiles.size() + batchSize - 1) / batchSize, batch.size());
                
                // Process each file in the batch
                for (String filePath : batch) {
                    try {
                        processedFiles++;
                        double progress = 25.0 + (processedFiles * 60.0 / totalFiles); // 25% to 85%
                        
                        sendProgressUpdate(sessionId, progress, 
                                String.format("Improving coverage for %s (%d/%d files)", extractFileName(filePath), processedFiles, totalFiles), 
                                ProgressUpdate.ProgressType.TEST_GENERATION);
                        
                        // Create file coverage request
                        EnhancedFileCoverageRequest fileRequest = EnhancedFileCoverageRequest.builder()
                                .sessionId(sessionId) // 🔑 CRITICAL FIX: Pass the current session ID
                                .repositoryUrl(request.getRepositoryUrl())
                                .branch(request.getBranch())
                                .filePath(filePath)
                                .targetCoverageIncrease(request.getTargetCoverageIncrease())
                                .githubToken(request.getGithubToken())
                                .workspaceId(request.getWorkspaceId())
                                .build();
                        
                        // Process individual file
                        FileCoverageImprovementResult result = coverageAgentService.improveFileCoverageEnhanced(fileRequest);
                        
                        if (result != null && result.getStatus() == FileCoverageImprovementResult.ProcessingStatus.COMPLETED) {
                            allResults.add(result);
                            log.info("Successfully processed file: {}", filePath);
                        } else {
                            log.warn("Failed to process file: {}", filePath);
                        }
                        
                    } catch (Exception e) {
                        log.error("Error processing file: " + filePath, e);
                        // Continue with next file instead of failing entire batch
                    }
                }
                
                // Small delay between batches to avoid overwhelming the system
                Thread.sleep(1000);
            }
            
            sendProgressUpdate(sessionId, 90.0, "Preparing final results", ProgressUpdate.ProgressType.VALIDATION);
            
            // Store consolidated results in session
            RepositoryCoverageImprovementResult repoResult = RepositoryCoverageImprovementResult.builder()
                    .repositoryUrl(request.getRepositoryUrl())
                    .branch(request.getBranch())
                    .totalFilesProcessed(processedFiles)
                    .successfulFiles(allResults.size())
                    .fileResults(allResults)
                    .processingTimeMs(System.currentTimeMillis()) // This would be calculated properly
                    .build();
            
            sessionManagementService.setSessionResults(sessionId, repoResult);
            
            // Update session with final status
            sessionManagementService.updateSessionStatus(sessionId, 
                    CoverageImprovementSession.SessionStatus.READY_FOR_REVIEW);
            
            // Send completion message after all operations are done
            sendProgressUpdate(sessionId, 100.0, 
                    String.format("Repository improvement complete! Successfully improved coverage for %d files. Results are ready for review.", allResults.size()), 
                    ProgressUpdate.ProgressType.COMPLETION);
            
            log.info("Background repository coverage processing completed for session: {}. Processed {}/{} files successfully", 
                    sessionId, allResults.size(), processedFiles);
            
        } catch (InterruptedException e) {
            log.info("Repository coverage processing was interrupted for session: {}", sessionId);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("Background repository coverage processing failed for session: {}", sessionId, e);
            sessionManagementService.handleError(sessionId, e);
            sendErrorUpdate(sessionId, "Repository coverage improvement failed: " + e.getMessage());
        }
    }

    /**
     * Helper method to send progress updates via WebSocket
     */
    private void sendProgressUpdate(String sessionId, Double progress, String message, ProgressUpdate.ProgressType type) {
        log.info("Sending progress update for session {}: {}% - {} (type: {})", sessionId, progress, message, type);
        ProgressUpdate update = ProgressUpdate.builder()
                .sessionId(sessionId)
                .progress(progress)
                .message(message)
                .currentStep(message)
                .type(type)
                .timestamp(java.time.LocalDateTime.now())
                .build();
        
        webSocketHandler.sendProgressUpdate(sessionId, update);
    }

    /**
     * Helper method to send user-friendly error updates via WebSocket
     */
    private void sendErrorUpdate(String sessionId, String errorMessage) {
        // Make error messages more user-friendly
        String userFriendlyMessage = errorMessage;
        if (errorMessage.contains("Exception") || errorMessage.contains("Error")) {
            userFriendlyMessage = "An issue occurred during analysis. Please try again or contact support.";
        }
        
        ProgressUpdate errorUpdate = ProgressUpdate.builder()
                .sessionId(sessionId)
                .progress(0.0)
                .message(userFriendlyMessage)
                .currentStep("Analysis paused")
                .type(ProgressUpdate.ProgressType.ERROR)
                .timestamp(java.time.LocalDateTime.now())
                .build();
        
        webSocketHandler.sendProgressUpdate(sessionId, errorUpdate);
    }

    /**
     * Helper method to extract file name from full path
     */
    private String extractFileName(String filePath) {
        if (filePath == null) return "";
        return filePath.substring(filePath.lastIndexOf('/') + 1);
    }
}
