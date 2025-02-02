package com.org.javadoc.ai.generator.mongo;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "pull_request_metrics")
@Getter
@Setter
public class PullRequestMetrics {
    @Id
    private String id;
    private String gitRepoName;
    private String pullRequestUrl;
    private int prCreatedCount;
    private int issuesResolved;
    private String engineeringTimeSaved;
    private String costSavings;
}
