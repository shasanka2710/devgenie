package com.org.devgenie;

import com.org.devgenie.config.CoverageConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@EnableConfigurationProperties(CoverageConfiguration.class)
@EnableAsync
@EnableScheduling
@EnableWebSocketMessageBroker
public class DevgenieApplication {
    public static void main(String[] args) {
        SpringApplication.run(DevgenieApplication.class, args);
    }
}
