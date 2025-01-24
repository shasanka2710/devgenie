package com.org.javadoc.ai.generator.service;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.org.javadoc.ai.generator.model.SonarIssue;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
public class SonarQubeService {
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public SonarQubeService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public List<SonarIssue> fetchIssues(String projectKey) {
        String url = "https://sonarqube.example.com/api/issues/search?projectKeys=" + projectKey;
        String response = restTemplate.getForObject(url, String.class);

        List<SonarIssue> issues = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode issuesNode = root.get("issues");

            if (issuesNode != null && issuesNode.isArray()) {
                for (JsonNode issueNode : issuesNode) {
                    SonarIssue issue = new SonarIssue();
                    issue.setId(issueNode.get("key").asText());
                    issue.setType(issueNode.get("type").asText());
                    issue.setSeverity(issueNode.get("severity").asText());
                    issue.setMessage(issueNode.get("message").asText());
                    issue.setComponent(issueNode.get("component").asText());
                    issues.add(issue);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return issues;
    }
}