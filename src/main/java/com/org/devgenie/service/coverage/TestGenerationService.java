package com.org.devgenie.service.coverage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.org.devgenie.model.coverage.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TestGenerationService {

    @Autowired
    private ChatClient chatClient;

    @Autowired
    private FileService fileService;

    public TestGenerationResult generateTestsForFile(FileAnalysisResult analysis) {
        log.info("Generating tests for file: {}", analysis.getFilePath());

        try {
            String fileContent = fileService.readFile(analysis.getFilePath());
            String testPrompt = createTestGenerationPrompt(fileContent, analysis);
            String aiResponse = chatClient.prompt(testPrompt).call().content();

            return parseTestGenerationResponse(aiResponse, analysis);

        } catch (Exception e) {
            log.error("Failed to generate tests for file: {}", analysis.getFilePath(), e);
            return TestGenerationResult.failure(analysis.getFilePath(), e.getMessage());
        }
    }

    private String createTestGenerationPrompt(String fileContent, FileAnalysisResult analysis) {
        return String.format("""
            Generate comprehensive JUnit 5 test cases for the following Java class to improve code coverage.
            
            Source Code:
            ```java
            %s
            ```
            
            Analysis Results:
            - Current Coverage: %.2f%%
            - Complexity: %s
            - Business Logic Priority: %s
            - Estimated Effort: %s
            
            Uncovered Code Paths:
            %s
            
            Testable Components:
            %s
            
            Dependencies to Mock:
            %s
            
            Requirements:
            1. Generate complete, runnable JUnit 5 test class
            2. Use Mockito for mocking dependencies
            3. Include tests for all uncovered code paths
            4. Add edge case tests for high-risk methods
            5. Use proper test naming conventions
            6. Include setup and teardown methods if needed
            7. Add meaningful assertions
            8. Include both positive and negative test cases
            
            Please provide the response in the following JSON format:
            {
                "testClassName": "GeneratedTestClassName",
                "testFilePath": "src/test/java/path/to/TestClass.java",
                "generatedTests": [
                    {
                        "methodName": "testMethodName",
                        "testType": "UNIT|INTEGRATION|EDGE_CASE",
                        "description": "Description of what this test covers",
                        "targetCodePath": "Method or code path being tested",
                        "coverageImpact": "HIGH|MEDIUM|LOW"
                    }
                ],
                "imports": [
                    "import statements needed"
                ],
                "mockDependencies": [
                    "Dependencies that need to be mocked"
                ],
                "testClassContent": "Complete test class content here",
                "estimatedCoverageIncrease": 25.5,
                "notes": "Any important notes about the generated tests"
            }
            """,
                fileContent,
                analysis.getCurrentCoverage(),
                analysis.getComplexity(),
                analysis.getBusinessLogicPriority(),
                analysis.getEstimatedEffort(),
                formatUncoveredCodePaths(analysis.getUncoveredCodePaths()),
                formatTestableComponents(analysis.getTestableComponents()),
                String.join(", ", analysis.getDependencies())
        );
    }

    private TestGenerationResult parseTestGenerationResponse(String aiResponse, FileAnalysisResult analysis) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonResponse = mapper.readTree(extractJsonFromResponse(aiResponse));

            List<GeneratedTest> tests = parseGeneratedTests(jsonResponse.get("generatedTests"));
            String testClassContent = jsonResponse.get("testClassContent").asText();
            String testFilePath = jsonResponse.get("testFilePath").asText();

            return TestGenerationResult.builder()
                    .sourceFilePath(analysis.getFilePath())
                    .testFilePath(testFilePath)
                    .testClassName(jsonResponse.get("testClassName").asText())
                    .generatedTests(tests)
                    .imports(parseStringArray(jsonResponse.get("imports")))
                    .mockDependencies(parseStringArray(jsonResponse.get("mockDependencies")))
                    .generatedTestContent(testClassContent)
                    .estimatedCoverageIncrease(jsonResponse.get("estimatedCoverageIncrease").asDouble())
                    .notes(jsonResponse.get("notes").asText())
                    .success(true)
                    .build();

        } catch (Exception e) {
            log.error("Failed to parse test generation response", e);
            return TestGenerationResult.failure(analysis.getFilePath(), "Failed to parse AI response: " + e.getMessage());
        }
    }

    private List<GeneratedTest> parseGeneratedTests(JsonNode testsNode) {
        List<GeneratedTest> tests = new ArrayList<>();
        if (testsNode != null && testsNode.isArray()) {
            for (JsonNode testNode : testsNode) {
                tests.add(GeneratedTest.builder()
                        .methodName(testNode.get("methodName").asText())
                        .testType(testNode.get("testType").asText())
                        .description(testNode.get("description").asText())
                        .targetCodePath(testNode.get("targetCodePath").asText())
                        .coverageImpact(testNode.get("coverageImpact").asText())
                        .build());
            }
        }
        return tests;
    }

    private List<String> parseStringArray(JsonNode arrayNode) {
        List<String> result = new ArrayList<>();
        if (arrayNode != null && arrayNode.isArray()) {
            for (JsonNode node : arrayNode) {
                result.add(node.asText());
            }
        }
        return result;
    }

    private String formatUncoveredCodePaths(List<UncoveredCodePath> paths) {
        return paths.stream()
                .map(path -> String.format("- %s: %s (Priority: %s, Type: %s)",
                        path.getLocation(), path.getDescription(), path.getPriority(), path.getSuggestedTestType()))
                .collect(Collectors.joining("\n"));
    }

    private String formatTestableComponents(List<TestableComponent> components) {
        return components.stream()
                .map(comp -> String.format("- %s: %s (Complexity: %s, Covered: %s, Risk: %s)",
                        comp.getMethodName(), comp.getDescription(), comp.getComplexity(),
                        comp.isCurrentlyCovered(), comp.getRiskLevel()))
                .collect(Collectors.joining("\n"));
    }

    private String extractJsonFromResponse(String response) {
        int startIndex = response.indexOf('{');
        int endIndex = response.lastIndexOf('}') + 1;

        if (startIndex >= 0 && endIndex > startIndex) {
            return response.substring(startIndex, endIndex);
        }

        throw new IllegalArgumentException("No valid JSON found in AI response");
    }
}
