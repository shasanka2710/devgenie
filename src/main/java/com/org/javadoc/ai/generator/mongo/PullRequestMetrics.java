package com.org.javadoc.ai.generator.mongo;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "pullrequest_metrics")
@Getter
@Setter
public class PullRequestMetrics {
    @Id
    private String id;
    private String issueKey;
    private String gitRepoName;
    private String pullRequestUrl;
    private int prCreatedCount;
    private int issuesResolved;
    private int engineeringTimeSaved;
    private double costSavings;
    private LocalDateTime createdDateTime;
}
