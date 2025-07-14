package com.org.devgenie.service.coverage;

@Service
@Async
@Slf4j
public class AsyncCoverageProcessor {

    @Autowired
    private CoverageAgentService coverageAgentService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @EventListener
    public void handleCoverageRequest(CoverageRequestEvent event) {
        log.info("Processing async coverage request: {}", event.getSessionId());

        try {
            if (event.getType() == CoverageRequestEvent.Type.REPOSITORY) {
                CoverageResponse response = coverageAgentService.increaseRepoCoverage(event.getRepoRequest());
                eventPublisher.publishEvent(new CoverageCompletedEvent(event.getSessionId(), response));
            } else {
                CoverageResponse response = coverageAgentService.increaseFileCoverage(event.getFileRequest());
                eventPublisher.publishEvent(new CoverageCompletedEvent(event.getSessionId(), response));
            }

        } catch (Exception e) {
            log.error("Async coverage processing failed", e);
            eventPublisher.publishEvent(new CoverageFailedEvent(event.getSessionId(), e.getMessage()));
        }
    }

    @Data
    @AllArgsConstructor
    public static class CoverageRequestEvent {
        private String sessionId;
        private Type type;
        private RepoCoverageRequest repoRequest;
        private FileCoverageRequest fileRequest;

        public enum Type {
            FILE, REPOSITORY
        }
    }

    @Data
    @AllArgsConstructor
    public static class CoverageCompletedEvent {
        private String sessionId;
        private CoverageResponse response;
    }

    @Data
    @AllArgsConstructor
    public static class CoverageFailedEvent {
        private String sessionId;
        private String error;
    }
}
