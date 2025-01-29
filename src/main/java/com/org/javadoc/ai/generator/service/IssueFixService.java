package com.org.javadoc.ai.generator.service;

import com.org.javadoc.ai.generator.github.GitHubUtility;
import com.org.javadoc.ai.generator.parser.JavaCodeParser;
import com.org.javadoc.ai.generator.util.GroupByKeys;
import com.org.javadoc.ai.generator.util.PathConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.org.javadoc.ai.generator.util.GroupByKeys.groupByKeys;

@Slf4j
@Service
public class IssueFixService {

    private final Map<String, List<String>> operationProgress = new ConcurrentHashMap<>();
    @Autowired
    private JavaCodeParser javaCodeParser;
    @Autowired
    GitHubUtility gitHubUtility;

    @Async
    public CompletableFuture<String> startFix(String operationId, List<Map<String, String>> classDescriptions) {
        String pullRequest = "";
        Map<String, Set<String>> sonarIssues = GroupByKeys.groupByKeys(classDescriptions);
        operationProgress.put(operationId, new ArrayList<>(List.of("Analyzing the request...DONE!")));

        try {
            for (Map.Entry<String, Set<String>> entry : sonarIssues.entrySet()) {
                String className = entry.getKey();
                Set<String> description = entry.getValue();

                // Check with LLM to identify the fix
                operationProgress.get(operationId).add("Identifying the Fix with LLM model ...");
                String fixedCode = identifyFix(className, description);
                operationProgress.get(operationId).add("Identifying the Fix with LLM model ...DONE!");

                // Validate the fix by checking if the fixed code is a valid Java code.
                operationProgress.get(operationId).add("Validating the Fix ...");
                boolean isValidCode = javaCodeParser.isValidJavaCode(fixedCode);
                operationProgress.get(operationId).add("Validating the Fix ...DONE!");
                if (!isValidCode) {
                    operationProgress.get(operationId).add("Fix is not valid Java code.for"+ className+" Skipping the fix.");
                    operationProgress.get(operationId).add("Failed");
                    return CompletableFuture.completedFuture(operationId);
                }

                // Apply the fix to the file
                operationProgress.get(operationId).add("Applying the Fix ...");
                applyFix(className, fixedCode);
                operationProgress.get(operationId).add("Applying the Fix ...DONE!");
            }

            // Create a pull request
            pullRequest = gitHubUtility.createPullRequest(
                    classDescriptions.stream().map(cd -> cd.get("className")).collect(Collectors.toList()),
                    "Automated fixing issues"
            );
            operationProgress.get(operationId).add("Creating a Pull Request...DONE!");
            operationProgress.get(operationId).add("Pull Request: " + pullRequest);
            operationProgress.get(operationId).add("Completed");

        } catch (Exception e) {
            operationProgress.get(operationId).add("Failed");
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
}