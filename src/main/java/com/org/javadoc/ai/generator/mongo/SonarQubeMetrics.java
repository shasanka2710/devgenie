package com.org.javadoc.ai.generator.mongo;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
@Getter
@Setter
@Document(collection = "sonarqube_metrics")
public class SonarQubeMetrics {
    @Id
    private String id;
    private String gitRepoName;
    private int totalSonarIssues;
    private String technicalDebtTime;
    private String estimatedDollarImpact;
}
