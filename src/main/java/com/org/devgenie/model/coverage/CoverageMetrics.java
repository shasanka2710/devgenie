package com.org.devgenie.model.coverage;

    import io.micrometer.core.instrument.*;
    import lombok.extern.slf4j.Slf4j;
    import org.springframework.stereotype.Component;

    @Component
    @Slf4j
    public class CoverageMetrics {

        private final MeterRegistry meterRegistry;
        private final Counter coverageRequestsCounter;
        private final Timer coverageProcessingTimer;
        private final Gauge activeCoverageSessions;

        public CoverageMetrics(MeterRegistry meterRegistry) {
            this.meterRegistry = meterRegistry;
            this.coverageRequestsCounter = Counter.builder("coverage.requests.total")
                    .description("Total number of coverage improvement requests")
                    .register(meterRegistry);
            this.coverageProcessingTimer = Timer.builder("coverage.processing.duration")
                    .description("Time taken to process coverage requests")
                    .register(meterRegistry);
            this.activeCoverageSessions = Gauge.builder("coverage.sessions.active", this, CoverageMetrics::getActiveSessions)
                    .description("Number of active coverage processing sessions")
                    .register(meterRegistry);
        }

        public void incrementCoverageRequests(String type) {
            coverageRequestsCounter.increment();
            // If you want to track by type, use a Counter per type or use tags when building the Counter
        }

        public Timer.Sample startProcessingTimer() {
            return Timer.start(meterRegistry);
        }

        public void recordProcessingTime(Timer.Sample sample, String result) {
            sample.stop(Timer.builder("coverage.processing.duration")
                    .tag("result", result)
                    .register(meterRegistry));
        }

        private double getActiveSessions() {
            // This would be implemented based on your session tracking mechanism
            return 0; // Placeholder
        }
    }