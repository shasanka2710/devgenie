package com.org.javadoc.ai.generator.service;

import com.org.javadoc.ai.generator.github.GitHubUtility;
import com.org.javadoc.ai.generator.model.ClassDescription;
import com.org.javadoc.ai.generator.mongo.PullRequestMetrics;
import com.org.javadoc.ai.generator.mongo.PullRequestMetricsRepository;
import com.org.javadoc.ai.generator.parser.JavaCodeParser;
import com.org.javadoc.ai.generator.util.PathConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import static com.org.javadoc.ai.generator.util.GroupByKeys.groupByKeys;
import static com.org.javadoc.ai.generator.util.StringUtil.getclassDisplayName;

@Slf4j
@Service
public class IssueFixService {

    private final Map<String, List<String>> operationProgress = new ConcurrentHashMap<>();

    private final JavaCodeParser javaCodeParser;

    private final GitHubUtility gitHubUtility;

    private final PullRequestMetricsRepository pullRequestMetricsRepository;

    public IssueFixService(JavaCodeParser javaCodeParser, GitHubUtility gitHubUtility, PullRequestMetricsRepository pullRequestMetricsRepository) {
        this.javaCodeParser = javaCodeParser;
        this.gitHubUtility = gitHubUtility;
        this.pullRequestMetricsRepository = pullRequestMetricsRepository;
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
                operationProgress.get(operationId).add("⚙\uFE0F Identifying the Fix with LLM model for " + sonarIssues.size() + " classes ...");
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
            calculateAndSavePullRequestMetrics(sonarIssues.size(), count);
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

    // Removed unused parameters "pullRequest", "totalIssues"
    private void calculateAndSavePullRequestMetrics(int totalIssues, int resolvedIssues) {
        // Metrics calculation (simplified as examples)
        int prCreatedCount = 1;
        int issuesResolved = resolvedIssues;
        String engineeringTimeSaved = "Approx. " + (resolvedIssues * 30) + " minutes";
        String costSavings = "$" + (resolvedIssues * 10);
        PullRequestMetrics metrics = new PullRequestMetrics();
        metrics.setGitRepoName("YourGitRepoName");
        metrics.setPrCreatedCount(prCreatedCount);
        metrics.setIssuesResolved(issuesResolved);
        metrics.setEngineeringTimeSaved(engineeringTimeSaved);
        metrics.setCostSavings(costSavings);
        pullRequestMetricsRepository.save(metrics);
        log.info("Pull Request Metrics saved successfully!");
    }
}