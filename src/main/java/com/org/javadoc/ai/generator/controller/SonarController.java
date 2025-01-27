package com.org.javadoc.ai.generator.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.org.javadoc.ai.generator.model.SonarIssue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequestMapping("/sonar")
public class SonarController {

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

    @GetMapping("/issues")
    public String getIssues(
            @RequestParam(value = "filterType", required = false) String filterType,
            Model model) {
        log.info("Fetching issues with filterType: {}", filterType);

        String url = String.format("%s?componentKeys=%s&severities=%s&ps=%d", sonarUrl, componentKeys, severities, pageSize);
        restTemplate.getInterceptors().add(new BasicAuthenticationInterceptor(sonarUsername, sonarPassword));

        String response = restTemplate.getForObject(url, String.class);
        try {
            List<SonarIssue> issues = parseSonarIssues(response);
            List<String> issueTypes = getDistinctIssueTypes(issues);

            if (filterType != null && !filterType.isEmpty()) {
                issues = issues.stream()
                        .filter(issue -> filterType.equals(issue.getType()))
                        .collect(Collectors.toList());
            }

            model.addAttribute("issues", issues);
            model.addAttribute("issueTypes", issueTypes);
            model.addAttribute("selectedType", filterType);
            return "insights";
        } catch (IOException e) {
            log.error("Error parsing issues: {}", e.getMessage());
            return "error";
        }
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

    private List<String> getDistinctIssueTypes(List<SonarIssue> issues) {
        return issues.stream()
                .map(SonarIssue::getType)
                .distinct()
                .collect(Collectors.toList());
    }

    private String replaceText(String input) {
        return (input != null) ? (input.replaceFirst("^[^:]+:", "").replace("/", ".")) : input;
    }
}