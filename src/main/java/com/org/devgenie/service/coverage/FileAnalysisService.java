package com.org.devgenie.service.coverage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.org.devgenie.exception.coverage.FileAnalysisException;
import com.org.devgenie.model.coverage.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class FileAnalysisService {

    @Autowired
    private ChatClient chatClient;

    @Autowired
    private CoverageDataService coverageDataService;

    public FileAnalysisResult analyzeFile(String filePath) {
        log.info("Analyzing file: {}", filePath);

        try {
            // Read file content
            String fileContent = readFileContent(filePath);

            // Get current coverage data
            FileCoverageData coverageData = coverageDataService.getFileCoverage(filePath);

            // Analyze with AI
            String analysisPrompt = createFileAnalysisPrompt(fileContent, coverageData);
            String aiAnalysis = chatClient.prompt(analysisPrompt).call().content();

            // Parse AI response
            return parseFileAnalysisResponse(aiAnalysis, filePath, coverageData);

        } catch (Exception e) {
            log.error("Failed to analyze file: {}", filePath, e);
            throw new FileAnalysisException("Failed to analyze file: " + e.getMessage(), e);
        }
    }

    public List<FilePriority> prioritizeFiles(CoverageData coverageData, double targetCoverage) {
        log.info("Prioritizing files for coverage improvement");

        try {
            String prioritizationPrompt = createPrioritizationPrompt(coverageData, targetCoverage);
            String aiResponse = chatClient.prompt(prioritizationPrompt).call().content();

            return parsePrioritizationResponse(aiResponse, coverageData);

        } catch (Exception e) {
            log.error("Failed to prioritize files", e);
            throw new FileAnalysisException("Failed to prioritize files: " + e.getMessage(), e);
        }
    }

    private String createFileAnalysisPrompt(String fileContent, FileCoverageData coverageData) {
        return String.format("""
            Analyze the following Java file for test coverage improvement opportunities.
            
            File Content:
            ```java
            %s
            ```
            
            Current Coverage Data:
            - Line Coverage: %.2f%%
            - Branch Coverage: %.2f%%
            - Method Coverage: %.2f%%
            - Uncovered Lines: %s
            - Uncovered Branches: %s
            
            Please provide analysis in the following JSON format:
            {
                "complexity": "LOW|MEDIUM|HIGH",
                "businessLogicPriority": "LOW|MEDIUM|HIGH",
                "testableComponents": [
                    {
                        "methodName": "methodName",
                        "complexity": "LOW|MEDIUM|HIGH",
                        "currentlyCovered": true/false,
                        "riskLevel": "LOW|MEDIUM|HIGH",
                        "description": "Brief description of what this method does"
                    }
                ],
                "uncoveredCodePaths": [
                    {
                        "location": "line numbers or method name",
                        "description": "Description of uncovered code",
                        "priority": "LOW|MEDIUM|HIGH",
                        "suggestedTestType": "UNIT|INTEGRATION|EDGE_CASE"
                    }
                ],
                "dependencies": ["List of key dependencies that need mocking"],
                "estimatedEffort": "LOW|MEDIUM|HIGH",
                "coverageImpactPotential": "LOW|MEDIUM|HIGH"
            }
            """,
                fileContent,
                coverageData.getLineCoverage(),
                coverageData.getBranchCoverage(),
                coverageData.getMethodCoverage(),
                String.join(", ", coverageData.getUncoveredLines()),
                String.join(", ", coverageData.getUncoveredBranches())
        );
    }

    private String createPrioritizationPrompt(CoverageData coverageData, double targetCoverage) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Prioritize the following files for test coverage improvement to reach ").append(targetCoverage).append("% overall coverage.\n\n");
        prompt.append("Current Overall Coverage: ").append(coverageData.getOverallCoverage()).append("%\n\n");
        prompt.append("Files and their coverage data:\n");

        for (FileCoverageData fileData : coverageData.getFiles()) {
            prompt.append(String.format("- %s: %.2f%% line coverage, %.2f%% branch coverage, %d uncovered lines\n",
                    fileData.getFilePath(),
                    fileData.getLineCoverage(),
                    fileData.getBranchCoverage(),
                    fileData.getUncoveredLines().size()));
        }

        prompt.append("""
            
            Please provide prioritization in the following JSON format:
            {
                "prioritizedFiles": [
                    {
                        "filePath": "path/to/file.java",
                        "priority": 1,
                        "impactScore": 85,
                        "effortScore": 45,
                        "impactEffortRatio": 1.89,
                        "reasoning": "High business logic concentration with low current coverage",
                        "estimatedCoverageGain": 15.5
                    }
                ],
                "strategy": "Description of overall prioritization strategy",
                "estimatedFilesToProcess": 5
            }
            """);

        return prompt.toString();
    }

    private FileAnalysisResult parseFileAnalysisResponse(String aiResponse, String filePath, FileCoverageData coverageData) {
        try {
            // Parse JSON response from AI
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonResponse = mapper.readTree(extractJsonFromResponse(aiResponse));

            return FileAnalysisResult.builder()
                    .filePath(filePath)
                    .complexity(jsonResponse.get("complexity").asText())
                    .businessLogicPriority(jsonResponse.get("businessLogicPriority").asText())
                    .testableComponents(parseTestableComponents(jsonResponse.get("testableComponents")))
                    .uncoveredCodePaths(parseUncoveredCodePaths(jsonResponse.get("uncoveredCodePaths")))
                    .dependencies(parseDependencies(jsonResponse.get("dependencies")))
                    .estimatedEffort(jsonResponse.get("estimatedEffort").asText())
                    .coverageImpactPotential(jsonResponse.get("coverageImpactPotential").asText())
                    .currentCoverage(coverageData.getLineCoverage())
                    .build();

        } catch (Exception e) {
            log.error("Failed to parse AI analysis response", e);
            throw new FileAnalysisException("Failed to parse AI response", e);
        }
    }

    private List<FilePriority> parsePrioritizationResponse(String aiResponse, CoverageData coverageData) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonResponse = mapper.readTree(extractJsonFromResponse(aiResponse));

            List<FilePriority> priorities = new ArrayList<>();
            JsonNode prioritizedFiles = jsonResponse.get("prioritizedFiles");

            for (JsonNode fileNode : prioritizedFiles) {
                priorities.add(FilePriority.builder()
                        .filePath(fileNode.get("filePath").asText())
                        .priority(fileNode.get("priority").asInt())
                        .impactScore(fileNode.get("impactScore").asDouble())
                        .effortScore(fileNode.get("effortScore").asDouble())
                        .impactEffortRatio(fileNode.get("impactEffortRatio").asDouble())
                        .reasoning(fileNode.get("reasoning").asText())
                        .estimatedCoverageGain(fileNode.get("estimatedCoverageGain").asDouble())
                        .build());
            }

            return priorities;

        } catch (Exception e) {
            log.error("Failed to parse prioritization response", e);
            throw new FileAnalysisException("Failed to parse prioritization response", e);
        }
    }

    private String extractJsonFromResponse(String response) {
        // Extract JSON from AI response (handles cases where AI adds explanation text)
        int startIndex = response.indexOf('{');
        int endIndex = response.lastIndexOf('}') + 1;

        if (startIndex >= 0 && endIndex > startIndex) {
            return response.substring(startIndex, endIndex);
        }

        throw new IllegalArgumentException("No valid JSON found in AI response");
    }

    private List<TestableComponent> parseTestableComponents(JsonNode componentsNode) {
        List<TestableComponent> components = new ArrayList<>();
        if (componentsNode != null && componentsNode.isArray()) {
            for (JsonNode componentNode : componentsNode) {
                components.add(TestableComponent.builder()
                        .methodName(componentNode.get("methodName").asText())
                        .complexity(componentNode.get("complexity").asText())
                        .currentlyCovered(componentNode.get("currentlyCovered").asBoolean())
                        .riskLevel(componentNode.get("riskLevel").asText())
                        .description(componentNode.get("description").asText())
                        .build());
            }
        }
        return components;
    }

    private List<UncoveredCodePath> parseUncoveredCodePaths(JsonNode pathsNode) {
        List<UncoveredCodePath> paths = new ArrayList<>();
        if (pathsNode != null && pathsNode.isArray()) {
            for (JsonNode pathNode : pathsNode) {
                paths.add(UncoveredCodePath.builder()
                        .location(pathNode.get("location").asText())
                        .description(pathNode.get("description").asText())
                        .priority(pathNode.get("priority").asText())
                        .suggestedTestType(pathNode.get("suggestedTestType").asText())
                        .build());
            }
        }
        return paths;
    }

    private List<String> parseDependencies(JsonNode dependenciesNode) {
        List<String> dependencies = new ArrayList<>();
        if (dependenciesNode != null && dependenciesNode.isArray()) {
            for (JsonNode depNode : dependenciesNode) {
                dependencies.add(depNode.asText());
            }
        }
        return dependencies;
    }

    private String readFileContent(String filePath) throws IOException {
        return Files.readString(Paths.get(filePath));
    }
}
