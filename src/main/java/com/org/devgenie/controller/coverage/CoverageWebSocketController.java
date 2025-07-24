package com.org.devgenie.controller.coverage;

import com.org.devgenie.service.coverage.AsyncCoverageProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

// Disabled STOMP-based WebSocket controller in favor of native WebSocket handler
// @Controller - Commented out to avoid conflicts with native WebSocket
@Component
@Slf4j
public class CoverageWebSocketController {

    // Native WebSocket handler is used instead of SimpMessagingTemplate
    // @Autowired
    // private SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void handleCoverageCompleted(AsyncCoverageProcessor.CoverageCompletedEvent event) {
        log.info("Coverage completion event received for session: {} (handled by native WebSocket)", event.getSessionId());
        // Progress updates are now handled by CoverageProgressWebSocketHandler
    }

    @EventListener
    public void handleCoverageFailed(AsyncCoverageProcessor.CoverageFailedEvent event) {
        log.info("Coverage failure event received for session: {} (handled by native WebSocket)", event.getSessionId());
        // Progress updates are now handled by CoverageProgressWebSocketHandler
    }

    // STOMP message mapping disabled in favor of native WebSocket
    // @MessageMapping("/coverage/progress")
    // public void sendProgressUpdate(String sessionId, String progress) { ... }
}
