package com.org.devgenie.config;

import org.apache.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class SonarConfig {
    @Value("${sonar.url}") String sonarUrl;
    @Value("${sonar.username}") String sonarUsername;
    @Value("${sonar.password}") String sonarPassword;

    @Bean
    public WebClient sonarWebClient() {
        String basicAuth = "Basic " + java.util.Base64.getEncoder()
                .encodeToString((sonarUsername + ":" + sonarPassword).getBytes());
        return WebClient.builder()
                .baseUrl(sonarUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, basicAuth)
                .build();
    }

}
