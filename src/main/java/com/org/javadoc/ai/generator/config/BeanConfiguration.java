package com.org.javadoc.ai.generator.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class BeanConfiguration {
    @Bean
    RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
