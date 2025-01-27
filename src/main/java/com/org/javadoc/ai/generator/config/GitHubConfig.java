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
    private String repository;

    @Value("${github.default.branch}")
    private String defaultBranch;

    @Bean
    public GitHub gitHub() {
        try {
            return GitHub.connectUsingOAuth(githubToken);
        } catch (Exception e) {
            // Create a dedicated exception class for GitHub connection errors.
            throw new GitHubConnectionException("Error connecting to GitHub.", e);
        }
    }
}

// Dedicated exception class for GitHub connection errors.
class GitHubConnectionException extends RuntimeException {
    public GitHubConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
