package com.org.devgenie.service.coverage;

import com.org.devgenie.dto.coverage.ProgressUpdate;
import com.org.devgenie.model.coverage.CoverageImprovementSession;
import com.org.devgenie.mongo.CoverageSessionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class SessionManagementService {

    @Autowired
    private CoverageSessionRepository sessionRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    public CoverageImprovementSession createSession(String repositoryUrl, String branch, 
                                                  String filePath, CoverageImprovementSession.SessionType type) {
        return createSession(null, repositoryUrl, branch, filePath, type);
    }

    public CoverageImprovementSession createSession(String sessionId, String repositoryUrl, String branch, 
                                                  String filePath, CoverageImprovementSession.SessionType type) {
        log.info("🔍 SessionManagementService.createSession called with sessionId: {}", sessionId);
        
        // Use provided session ID or generate a new one
        String finalSessionId = (sessionId != null && !sessionId.trim().isEmpty()) 
            ? sessionId 
            : UUID.randomUUID().toString();
            
        log.info("🔍 Final sessionId will be: {} (provided: {}, generated: {})", 
            finalSessionId, sessionId, sessionId == null || sessionId.trim().isEmpty());
            
        CoverageImprovementSession session = CoverageImprovementSession.builder()
                .sessionId(finalSessionId)
                .repositoryUrl(repositoryUrl)
                .branch(branch)
                .filePath(filePath)
                .type(type)
                .status(CoverageImprovementSession.SessionStatus.CREATED)
                .progress(0.0)
                .currentStep("Initializing session")
                .startedAt(LocalDateTime.now())
                .build();

        return sessionRepository.save(session);
    }

    public void updateProgress(String sessionId, Double progress, String step) {
        updateProgress(sessionId, progress, step, null, null);
    }

    public void updateProgress(String sessionId, Double progress, String step, 
                             String message, Map<String, Object> stepData) {
        Optional<CoverageImprovementSession> sessionOpt = sessionRepository.findById(sessionId);
        if (sessionOpt.isPresent()) {
            CoverageImprovementSession session = sessionOpt.get();
            session.setProgress(progress);
            session.setCurrentStep(step);
            
            sessionRepository.save(session);

            // Send progress update via WebSocket
            ProgressUpdate update = ProgressUpdate.builder()
                    .sessionId(sessionId)
                    .progress(progress)
                    .currentStep(step)
                    .message(message)
                    .timestamp(LocalDateTime.now())
                    .currentBatch(session.getCurrentBatch())
                    .totalBatches(session.getTotalBatches())
                    .processedFiles(session.getProcessedFiles())
                    .totalFiles(session.getTotalFiles())
                    .stepData(stepData)
                    .type(ProgressUpdate.ProgressType.ANALYSIS)
                    .build();

            eventPublisher.publishEvent(new ProgressUpdateEvent(update));
        }
    }

    public void updateSessionStatus(String sessionId, CoverageImprovementSession.SessionStatus status) {
        Optional<CoverageImprovementSession> sessionOpt = sessionRepository.findById(sessionId);
        if (sessionOpt.isPresent()) {
            CoverageImprovementSession session = sessionOpt.get();
            session.setStatus(status);
            sessionRepository.save(session);
        }
    }

    public void updateBatchProgress(String sessionId, Integer currentBatch, Integer totalBatches) {
        Optional<CoverageImprovementSession> sessionOpt = sessionRepository.findById(sessionId);
        if (sessionOpt.isPresent()) {
            CoverageImprovementSession session = sessionOpt.get();
            session.setCurrentBatch(currentBatch);
            session.setTotalBatches(totalBatches);
            sessionRepository.save(session);
        }
    }

    public void setSessionResults(String sessionId, Object results) {
        Optional<CoverageImprovementSession> sessionOpt = sessionRepository.findById(sessionId);
        if (sessionOpt.isPresent()) {
            CoverageImprovementSession session = sessionOpt.get();
            session.setResults(results);
            sessionRepository.save(session);
        }
    }

    public Optional<CoverageImprovementSession> getSession(String sessionId) {
        return sessionRepository.findById(sessionId);
    }

    public void handleError(String sessionId, Exception error) {
        Optional<CoverageImprovementSession> sessionOpt = sessionRepository.findById(sessionId);
        if (sessionOpt.isPresent()) {
            CoverageImprovementSession session = sessionOpt.get();
            session.setStatus(CoverageImprovementSession.SessionStatus.FAILED);
            
            // Initialize errors list if null to prevent NPE
            if (session.getErrors() == null) {
                session.setErrors(new java.util.ArrayList<>());
            }
            session.getErrors().add(error.getMessage());
            sessionRepository.save(session);

            // Send error update via WebSocket
            ProgressUpdate update = ProgressUpdate.builder()
                    .sessionId(sessionId)
                    .progress(session.getProgress())
                    .currentStep("Error occurred")
                    .message(error.getMessage())
                    .timestamp(LocalDateTime.now())
                    .type(ProgressUpdate.ProgressType.ERROR)
                    .build();

            eventPublisher.publishEvent(new ProgressUpdateEvent(update));
        }
    }

    // Event class for WebSocket communication
    public static class ProgressUpdateEvent {
        private final ProgressUpdate progressUpdate;

        public ProgressUpdateEvent(ProgressUpdate progressUpdate) {
            this.progressUpdate = progressUpdate;
        }

        public ProgressUpdate getProgressUpdate() {
            return progressUpdate;
        }
    }
}
