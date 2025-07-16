package com.org.devgenie.service.coverage;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

@Component
public class CoverageHealthIndicator implements HealthIndicator {

    @Autowired
    private CoverageDataService coverageDataService;

    @Autowired
    private ChatClient chatClient;

    @Override
    public Health health() {
        try {
            // Check MongoDB connection
            coverageDataService.getCurrentCoverage("health-check", "main");

            // Check AI service
            String testResponse = chatClient.prompt("Say 'OK' if you can respond").call().content();

            if (testResponse.toLowerCase().contains("ok")) {
                return Health.up()
                        .withDetail("mongodb", "UP")
                        .withDetail("ai-service", "UP")
                        .build();
            } else {
                return Health.down()
                        .withDetail("ai-service", "AI service not responding correctly")
                        .build();
            }

        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
