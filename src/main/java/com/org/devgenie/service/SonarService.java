package com.org.devgenie.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Projections;
import com.org.devgenie.model.SonarIssue;
import com.org.devgenie.model.SonarMetricsModel;
import com.org.devgenie.util.LoggerUtil;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.org.devgenie.util.ConverterUtil.convertToHours;
import static com.org.devgenie.util.ConverterUtil.roundToTwoDecimalPlaces;

@Service
public class SonarService {

    private Logger logger = LoggerFactory.getLogger(SonarService.class);

    private final RestTemplate restTemplate;

    private final ObjectMapper objectMapper;

    private final String sonarUrl;

    private final String componentKeys;

    private final String severities;

    private final int pageSize;

    private final String sonarUsername;

    private final String sonarPassword;

    private final MongoTemplate mongoTemplate;

    @Value("${developer.dollarValuePerMinute}")
    double dollarValuePerMinute;

    public SonarService(RestTemplate restTemplate, ObjectMapper objectMapper, @Value("${sonar.url}") String sonarUrl, @Value("${sonar.componentKeys}") String componentKeys,
                        @Value("${sonar.severities}") String severities, @Value("${sonar.pageSize}") int pageSize,
                        @Value("${sonar.username}") String sonarUsername, @Value("${sonar.password}") String sonarPassword,
                        MongoTemplate mongoTemplate) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.sonarUrl = sonarUrl;
        this.componentKeys = componentKeys;
        this.severities = severities;
        this.pageSize = pageSize;
        this.sonarUsername = sonarUsername;
        this.sonarPassword = sonarPassword;
        this.mongoTemplate = mongoTemplate;
    }

    public List<SonarIssue> fetchSonarIssues() throws IOException {
        return parseSonarIssues(fetchIssuesFromSonar());
    }

    public String fetchIssuesFromSonar() {
        String url = String.format("%s?componentKeys=%s&severities=%s&resolved=false&ps=%d", sonarUrl+"issues/search", componentKeys, severities, pageSize);
        logger.info("Fetching issues from Sonar: {}", LoggerUtil.maskSensitive(url));
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, getAuthHeaders(), String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("Fetched issues from Sonar successfully.");
                saveIssuesToMongo(response.getBody());
                return response.getBody();
            } else {
                logger.error("Failed to fetch issues from Sonar. Status: {}", LoggerUtil.maskSensitive(String.valueOf(response.getStatusCode())));
                // Return empty JSON if there's an error
                return "{}";
            }
        } catch (Exception e) {
            logger.error("Error fetching issues from SonarQube: ", e);
            return "{}";
        }
    }
    private HttpEntity getAuthHeaders(){
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(sonarUsername, sonarPassword);
        // Disable caching
        headers.setCacheControl(CacheControl.noCache());
        return new HttpEntity<>(headers);
    }

    private List<SonarIssue> parseSonarIssues(String responseBody) throws IOException {
        JsonNode issuesNode = objectMapper.readTree(responseBody).path("issues");
        List<SonarIssue> issues = new ArrayList<>();
        for (JsonNode issueNode : issuesNode) {
            SonarIssue issue = new SonarIssue();
            issue.setKey(issueNode.path("key").asText());
            issue.setType(issueNode.path("type").asText());
            issue.setSeverity(issueNode.path("severity").asText());
            issue.setDescription(issueNode.path("message").asText());
            issue.setCategory(replaceText(issueNode.path("component").asText()));
            // Extract softwareQualities from impacts array
            List<String> softwareQualities = new ArrayList<>();
            for (JsonNode impactNode : issueNode.path("impacts")) {
                softwareQualities.add(impactNode.path("softwareQuality").asText());
            }
            issue.setSoftwareQuality(softwareQualities);
            issues.add(issue);
        }
        return issues;
    }

    private String replaceText(String input) {
        return (input != null) ? (input.replaceFirst("^[^:]+:", "").replace("/", ".")) : input;
    }

    public SonarMetricsModel getSonarMetrics() {
        SonarMetricsModel sonarMetricsModel = new SonarMetricsModel();
        try {
            JsonNode rootNode = getRootNode();
            sonarMetricsModel.setTotalIssuesCount(rootNode.get("total").asInt());
            sonarMetricsModel.setTechDebtTime(convertToHours(rootNode.get("effortTotal").asText())+" hours");
            sonarMetricsModel.setDollarImpact("$ "+roundToTwoDecimalPlaces(rootNode.get("effortTotal").asInt() * dollarValuePerMinute));
        } catch (IOException e) {
            logger.error("Error fetching Sonar metrics: ", e);
        }
        return sonarMetricsModel;
    }

    public JsonNode getRootNode() throws IOException {
        // Returning the expression directly instead of assigning to a temporary variable
        return objectMapper.readTree(fetchIssuesFromSonar());
    }

    private void saveIssuesToMongo(String responseBody) {
        try {
            MongoCollection<Document> collection = mongoTemplate.getCollection("sonarissues");

            List<Document> newDocuments = new ArrayList<>();
            Set<String> existingIssueKeys = new HashSet<>();

            // Step 1: Fetch existing issue keys from MongoDB
            collection.find()
                    .projection(Projections.include("key")) // Fetch only the 'key' field
                    .forEach(document -> existingIssueKeys.add(document.getString("key")));

            // Step 2: Parse response and filter out duplicates
            objectMapper.readTree(responseBody).path("issues").forEach(issueNode -> {
                String issueKey = issueNode.path("key").asText(); // Assuming 'key' is the unique identifier for an issue
                if (!existingIssueKeys.contains(issueKey)) {
                    newDocuments.add(Document.parse(issueNode.toString()));
                    existingIssueKeys.add(issueKey); // Add to local set to avoid duplicates in this batch
                }
            });

            // Step 3: Insert only new issues
            if (!newDocuments.isEmpty()) {
                collection.insertMany(newDocuments);
                logger.info("Inserted {} new issues into MongoDB.", newDocuments.size());
            } else {
                logger.info("No new issues to insert.");
            }
        } catch (Exception e) {
            logger.error("Handling and suppressing the exception", e);
        }
    }
}