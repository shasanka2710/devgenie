package com.org.devgenie.controller.coverage;

import com.org.devgenie.service.coverage.AsyncCoverageProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Controller
@Slf4j
public class CoverageWebSocketController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void handleCoverageCompleted(AsyncCoverageProcessor.CoverageCompletedEvent event) {
        log.info("Sending coverage completion update for session: {}", event.getSessionId());
        messagingTemplate.convertAndSend(
                "/topic/coverage/" + event.getSessionId(),
                Map.of("status", "completed", "response", event.getResponse())
        );
    }

    @EventListener
    public void handleCoverageFailed(AsyncCoverageProcessor.CoverageFailedEvent event) {
        log.info("Sending coverage failure update for session: {}", event.getSessionId());
        messagingTemplate.convertAndSend(
                "/topic/coverage/" + event.getSessionId(),
                Map.of("status", "failed", "error", event.getError())
        );
    }

    @MessageMapping("/coverage/progress")
    public void sendProgressUpdate(String sessionId, String progress) {
        messagingTemplate.convertAndSend(
                "/topic/coverage/" + sessionId,
                Map.of("status", "progress", "message", progress)
        );
    }
}
