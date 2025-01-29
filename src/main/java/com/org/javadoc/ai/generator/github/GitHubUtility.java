package com.org.javadoc.ai.generator.github;

import org.kohsuke.github.GHPullRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.List;

@Component
public class GitHubUtility {

    private static final Logger logger = LoggerFactory.getLogger(GitHubUtility.class);

    private final PullRequestHandler pullRequestHandler;

    public GitHubUtility(PullRequestHandler pullRequestHandler) {
        this.pullRequestHandler = pullRequestHandler;
    }

    public String createPullRequest(List<String> classNames, String description) throws IOException {
        try {
            GHPullRequest pullRequest = pullRequestHandler.createPullRequest(classNames, description);
            if (pullRequest == null) {
                throw new IOException("Error creating pull request");
            }
            logger.info("Pull Request Created:");
            logger.info("Title: {}", pullRequest.getTitle());
            logger.info("Number: {}", pullRequest.getNumber());
            logger.info("URL: {}", pullRequest.getHtmlUrl());
            return pullRequest.getHtmlUrl().toString();
        } catch (IOException e) {
            throw new IOException("Error creating pull request", e);
        }
    }
}
