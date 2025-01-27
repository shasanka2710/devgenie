package com.org.javadoc.ai.generator.config;

import lombok.Getter;
import lombok.Setter;
import org.kohsuke.github.GitHub;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Setter
@Getter
public class GitHubConfig {

    @Value("${github.token}")
    private String githubToken;
    @Value("${github.repo}")
    private  String repository;
    @Value("${github.default.branch}")
    private  String defaultBranch;

    @Bean
    public GitHub gitHub() {
        try {
            return GitHub.connectUsingOAuth(githubToken);
        } catch (Exception e) {
            throw new RuntimeException("Error connecting to GitHub: " + e.getMessage());
        }
    }

}
