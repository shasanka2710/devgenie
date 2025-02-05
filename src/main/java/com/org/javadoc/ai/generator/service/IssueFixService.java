package com.org.javadoc.ai.generator.service;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.org.javadoc.ai.generator.github.GitHubUtility;
import com.org.javadoc.ai.generator.model.ClassDescription;
import com.org.javadoc.ai.generator.mongo.PullRequestMetrics;
import com.org.javadoc.ai.generator.mongo.PullRequestMetricsRepository;
import com.org.javadoc.ai.generator.parser.JavaCodeParser;
import com.org.javadoc.ai.generator.util.PathConverter;
import lombok.extern.slf4j.Slf4j;
import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.bson.Document;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import static com.mongodb.client.model.Filters.eq;
import static com.org.javadoc.ai.generator.util.GroupByKeys.groupByKeys;
import static com.org.javadoc.ai.generator.util.StringUtil.getclassDisplayName;

@Slf4j
@Service
public class IssueFixService {

    private final Map<String, List<String>> operationProgress = new ConcurrentHashMap<>();

    private final JavaCodeParser javaCodeParser;

    private final GitHubUtility gitHubUtility;

    private final PullRequestMetricsRepository pullRequestMetricsRepository;

    private final MongoTemplate mongoTemplate;

    private final double dollarValuePerMinute;

    public IssueFixService(JavaCodeParser javaCodeParser, GitHubUtility gitHubUtility, PullRequestMetricsRepository pullRequestMetricsRepository, MongoTemplate mongoTemplate, @Value("${developer.dollarValuePerMinute}") double dollarValuePerMinute) {
        this.javaCodeParser = javaCodeParser;
        this.gitHubUtility = gitHubUtility;
        this.pullRequestMetricsRepository = pullRequestMetricsRepository;
        this.mongoTemplate = mongoTemplate;
        this.dollarValuePerMinute = dollarValuePerMinute;
    }

    @Async
    public CompletableFuture<String> startFix(String operationId, List<ClassDescription> classDescriptions) {
        Map<String, Set<String>> sonarIssues = groupByKeys(classDescriptions);
        int count = 0;
        operationProgress.put(operationId, new ArrayList<>(List.of("\uD83D\uDD04 Analyzing the request for " + sonarIssues.size() + " classes!")));
        try {
            for (Map.Entry<String, Set<String>> entry : sonarIssues.entrySet()) {
                String className = entry.getKey();
                Set<String> description = entry.getValue();
                operationProgress.get(operationId).add("⚙\uFE0F Identifying the Fix with LLM model for " + sonarIssues.size() + " class(es)!");
                String fixedCode = identifyFix(className, description);
                operationProgress.get(operationId).add("✅ Identifying the Fix with LLM model for " + getclassDisplayName(className) + " ...DONE!");
                boolean isValidCode = javaCodeParser.isValidJavaCode(fixedCode);
                operationProgress.get(operationId).add("✅ Validating the Fix for class " + getclassDisplayName(className) + "...DONE!");
                if (!isValidCode) {
                    operationProgress.get(operationId).add("Please check logs for " + getclassDisplayName(className) + ", Skipping the fix.");
                    continue;
                }
                operationProgress.get(operationId).add("⚙\uFE0F Applying the Fix for class " + getclassDisplayName(className));
                applyFix(className, fixedCode);
                operationProgress.get(operationId).add("✅ Applying the Fix for class " + getclassDisplayName(className) + " ...DONE!");
                count++;
            }
            String pullRequest = gitHubUtility.createPullRequest(sonarIssues.entrySet().stream().map(Map.Entry::getKey).toList(), "Automated fixing issues");
            // Calculate and save PR metrics
            calculateAndSavePullRequestMetrics(count, classDescriptions);
            int skippedCount = sonarIssues.size() - count;
            operationProgress.get(operationId).add("✅ Creating a Pull Request for " + count + " files and " + skippedCount + " file(s) Skipped!");
            operationProgress.get(operationId).add("✅ Pull Request: <a href=\"" + pullRequest + "\">" + pullRequest + "</a>");
            operationProgress.get(operationId).add("✅ Completed");
        } catch (Exception e) {
            operationProgress.get(operationId).add("❌ Failed");
        }
        return CompletableFuture.completedFuture(operationId);
    }

    private String identifyFix(String className, Set<String> description) throws FileNotFoundException {
        return javaCodeParser.identifyFixUsingLLModel(className, description);
    }

    private void applyFix(String className, String fixedCode) throws IOException {
        String filePath = PathConverter.toSlashedPath(className);
        Files.write(Paths.get(filePath), fixedCode.getBytes());
    }

    public List<String> getStatus(String operationId) {
        return operationProgress.getOrDefault(operationId, List.of("unknown"));
    }

    private void calculateAndSavePullRequestMetrics(int resolvedIssues, List<ClassDescription> classDescriptions) {
        int prCreatedCount = 1;
        int issuesResolved = resolvedIssues;
        MongoCollection<Document> collection = mongoTemplate.getCollection("sonarissues");
        // Iterate over the classDescriptions to fetch the issue key for each class
        for (ClassDescription classDescription : classDescriptions) {
            String issueKey = classDescription.getKey();
            // Get the issue key from ClassDescription
            Bson filter = eq("key", issueKey);
            MongoCursor<Document> cursor = collection.find(filter).iterator();
            if (cursor.hasNext()) {
                Document sonarIssue = cursor.next();
                // Save Pull Request metrics with data retrieved from SonarIssues
                PullRequestMetrics metrics = new PullRequestMetrics();
                metrics.setGitRepoName(sonarIssue.get("project").toString());
                metrics.setPrCreatedCount(prCreatedCount);
                metrics.setIssuesResolved(issuesResolved);
                metrics.setEngineeringTimeSaved(extractMinutes(sonarIssue.get("effort").toString()));
                metrics.setCostSavings(extractAndMultiplyEffort(sonarIssue.get("effort").toString()));
                metrics.setCreatedDateTime(new Date().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
                // Save the metrics in the repository
                pullRequestMetricsRepository.save(metrics);
                log.info("Pull Request Metrics saved for issue: " + issueKey);
            }
        }
    }

    public int extractMinutes(String effort) {
        // Extract the number from the string (assuming the format is always a number followed by "min")
        // Removes any non-numeric characters using concise character class syntax.
        String numberString = effort.replaceAll("\\D", "");
        // Convert the number to an integer
        return Integer.parseInt(numberString);
    }

    public double extractAndMultiplyEffort(String effort) {
        // Extract the number from the string (assuming the format is always a number followed by "min")
        // Multiply by dollarValuePerMinute to get the cost savings
        // Return the result as a string prefixed with '$'
        return extractMinutes(effort) * dollarValuePerMinute;
    }
}