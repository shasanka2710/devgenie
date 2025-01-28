package com.org.javadoc.ai.generator.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.org.javadoc.ai.generator.model.SonarIssue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class SonarService {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${sonar.url}")
    private String sonarUrl;

    @Value("${sonar.componentKeys}")
    private String componentKeys;

    @Value("${sonar.severities}")
    private String severities;

    @Value("${sonar.pageSize}")
    private int pageSize;

    @Value("${sonar.username}")
    private String sonarUsername;

    @Value("${sonar.password}")
    private String sonarPassword;

    public List<SonarIssue> fetchSonarIssues() throws IOException {
        return parseSonarIssues(fetchIssuesFromSonar());
    }

    public String fetchIssuesFromSonar() {
        String url = String.format("%s?componentKeys=%s&severities=%s&ps=%d", sonarUrl, componentKeys, severities, pageSize);
        restTemplate.getInterceptors().add(new BasicAuthenticationInterceptor(sonarUsername, sonarPassword));
        // Immediately return the expression instead of assigning it to a temporary variable
        return restTemplate.getForObject(url, String.class);
    }

    private List<SonarIssue> parseSonarIssues(String responseBody) throws IOException {
        JsonNode rootNode = objectMapper.readTree(responseBody);
        JsonNode issuesNode = rootNode.path("issues");
        List<SonarIssue> issues = new ArrayList<>();
        for (JsonNode issueNode : issuesNode) {
            SonarIssue issue = new SonarIssue();
            issue.setKey(issueNode.path("key").asText());
            issue.setType(issueNode.path("type").asText());
            issue.setSeverity(issueNode.path("severity").asText());
            issue.setDescription(issueNode.path("message").asText());
            issue.setCategory(replaceText(issueNode.path("component").asText()));
            issues.add(issue);
        }
        return issues;
    }

    private String replaceText(String input) {
        return (input != null) ? (input.replaceFirst("^[^:]+:", "").replace("/", ".")) : input;
    }

    public JsonNode getRootNode() throws IOException {
        JsonNode rootNode = objectMapper.readTree(fetchIssuesFromSonar());
        return rootNode;
    }
}
