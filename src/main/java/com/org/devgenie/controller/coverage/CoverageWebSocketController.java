package com.org.devgenie.controller.coverage;

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
