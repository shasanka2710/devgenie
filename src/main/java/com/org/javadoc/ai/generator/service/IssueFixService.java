package com.org.javadoc.ai.generator.service;

import com.org.javadoc.ai.generator.github.GitHubUtility;
import com.org.javadoc.ai.generator.parser.JavaCodeParser;
import com.org.javadoc.ai.generator.util.PathConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class IssueFixService {

    private final Map<String, String> operationProgress = new ConcurrentHashMap<>();
    @Autowired
    private JavaCodeParser javaCodeParser;
    @Autowired
    GitHubUtility gitHubUtility;

    public String startFix(String className, String description) {
        String operationId = UUID.randomUUID().toString();
        String pullRequestUrl ="";
        operationProgress.put(operationId, "started");

        try {
            // Check with LLM to identify the fix
            operationProgress.put(operationId, "Analyzing the issue with LLM model");
            String fixedCode = identifyFix(className,description);
            //Validate the fix by checking if the fixed code is a valid Java code.
            operationProgress.put(operationId, "Validating the fix provided by Model");
            boolean isValidCode = javaCodeParser.isValidJavaCode(fixedCode);
            if(!isValidCode) {
                operationProgress.put(operationId, "failed");
                return "Invalid fix code";
            }
            // Apply the fix to the file
            operationProgress.put(operationId, "Applying the fix to the file");
            applyFix(className, fixedCode);
            //Get the name of the class
            //String name = javaCodeParser.getCompilationUnit(className).getPrimaryTypeName().get().toString();
            // Create a pull request
            operationProgress.put(operationId, "Creating a pull request");
            pullRequestUrl = gitHubUtility.createPullRequest(className, "Automated fixing issue: " + description);
            operationProgress.put(operationId, "Completed and pull request created");
            operationProgress.put(operationId + "_url", pullRequestUrl);
        } catch (Exception e) {
            operationProgress.put(operationId, "failed");
            log.error("Error occurred while fixing the issue", e);
        }
        return operationId;
    }

    private String identifyFix(String className, String description) throws FileNotFoundException {
        return javaCodeParser.identifyFixUsingLLModel(className, description);
    }

    private void applyFix(String className, String fixedCode) throws IOException {
        String filePath = PathConverter.toSlashedPath(className);
        Files.write(Paths.get(filePath), fixedCode.getBytes());
    }
    public String getStatus(String operationId) {
        return operationProgress.getOrDefault(operationId, "unknown");
    }

    public String getPullRequestUrl(String operationId) {
        return operationProgress.getOrDefault(operationId + "_url", null);
    }
}