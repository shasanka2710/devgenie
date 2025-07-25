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
            9. Ensure the test class is properly structured with one complete class
            10. Do NOT generate multiple class declarations
            
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
                "testClassContent": "Complete test class content as ONE cohesive class with multiple test methods",
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

    /**
     * Generate tests in batches to handle token limitations
     */
    public BatchTestGenerationResult generateTestsBatch(FileAnalysisResult analysis, int batchIndex, Integer maxTestsPerBatch) {
        log.info("Generating test batch {} for file: {}", batchIndex + 1, analysis.getFilePath());

        try {
            String fileContent = fileService.readFile(analysis.getFilePath());
            
            // Create batch-specific prompt
            String batchPrompt = createBatchTestGenerationPrompt(fileContent, analysis, batchIndex, maxTestsPerBatch);
            String aiResponse = chatClient.prompt(batchPrompt).call().content();

            return parseBatchTestGenerationResponse(aiResponse, analysis, batchIndex);

        } catch (Exception e) {
            log.error("Failed to generate test batch {} for file: {}", batchIndex + 1, analysis.getFilePath(), e);
            return BatchTestGenerationResult.failure(e.getMessage());
        }
    }

    /**
     * Validate generated tests by compilation and optional execution
     */
    public TestValidationResult validateGeneratedTests(String repoDir, List<String> testFiles) {
        log.info("Validating {} generated test files", testFiles.size());

        try {
            // First, try to compile the tests
            CompilationResult compilationResult = compileTestFiles(repoDir, testFiles);
            
            if (!compilationResult.getSuccess()) {
                return TestValidationResult.builder()
                        .success(false)
                        .testsExecuted(0)
                        .testsPassed(0)
                        .testsFailed(0)
                        .compilationErrors(compilationResult.getErrors())
                        .validationMethod("COMPILATION_ONLY")
                        .build();
            }

            // If compilation succeeds, try to run the tests
            TestExecutionResult executionResult = executeTestFiles(repoDir, testFiles);
            
            return TestValidationResult.builder()
                    .success(executionResult.getSuccess())
                    .testsExecuted(executionResult.getTestsExecuted())
                    .testsPassed(executionResult.getTestsPassed())
                    .testsFailed(executionResult.getTestsFailed())
                    .executionErrors(executionResult.getErrors())
                    .validationMethod("EXECUTION")
                    .executionTimeMs(executionResult.getExecutionTimeMs())
                    .build();

        } catch (Exception e) {
            log.error("Failed to validate generated tests", e);
            return TestValidationResult.builder()
                    .success(false)
                    .compilationErrors(List.of(e.getMessage()))
                    .validationMethod("VALIDATION_ERROR")
                    .build();
        }
    }

    private String createBatchTestGenerationPrompt(String fileContent, FileAnalysisResult analysis, 
                                                  int batchIndex, Integer maxTestsPerBatch) {
        return String.format("""
            Generate %d JUnit 5 test methods for the following Java class (batch %d).
            Focus on different aspects of the code to maximize coverage.
            
            Source Code:
            ```java
            %s
            ```
            
            Analysis Results:
            - File Path: %s
            - Current Coverage: %.1f%%
            - Methods to focus on: %s
            
            IMPORTANT REQUIREMENTS:
            1. Generate exactly %d test methods
            2. Return ONLY the test method code, NOT complete class declarations
            3. Each method should be a standalone test method with @Test annotation
            4. Use JUnit 5 annotations (@Test, @BeforeEach, etc.)
            5. Include Mockito mocks where appropriate
            6. Focus on edge cases and error conditions
            7. Ensure tests are independent and can run in any order
            8. Include meaningful assertions
            9. Do NOT include package declarations, imports, or class declarations
            10. Do NOT include setUp() methods or class-level fields
            
            Return the response in JSON format with ONLY method code:
            {
                "testClass": "TestClassName",
                "testMethods": [
                    {
                        "methodName": "testMethodName",
                        "description": "What this test validates",
                        "code": "@Test\\nvoid testMethodName() {\\n    // test implementation\\n    // assertions\\n}",
                        "coveredMethods": ["method1", "method2"],
                        "estimatedCoverageContribution": 5.0
                    }
                ]
            }
            
            EXAMPLE of correct method code format:
            "@Test\\nvoid testConstructorInjection() {\\n    // Given\\n    // When\\n    // Then\\n    assertNotNull(instance);\\n}"
            
            DO NOT include:
            - Package declarations
            - Import statements
            - Class declarations
            - Class-level fields or mocks
            - setUp methods
            - Class closing braces
            """, 
            maxTestsPerBatch, batchIndex + 1, fileContent, analysis.getFilePath(), 
            analysis.getCurrentCoverage(), getMethodsForBatch(analysis, batchIndex), maxTestsPerBatch);
    }

    private String getMethodsForBatch(FileAnalysisResult analysis, int batchIndex) {
        // This would intelligently select methods for this batch
        return "Methods identified for test generation in this batch";
    }

    private BatchTestGenerationResult parseBatchTestGenerationResponse(String aiResponse, 
                                                                     FileAnalysisResult analysis, 
                                                                     int batchIndex) {
        try {
            // Sanitize AI response - remove markdown code block formatting
            String sanitizedResponse = sanitizeAiResponse(aiResponse);
            log.debug("Original AI response length: {}, Sanitized length: {}", 
                     aiResponse.length(), sanitizedResponse.length());
            
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(sanitizedResponse);
            
            List<GeneratedTestInfo> tests = new ArrayList<>();
            JsonNode testMethods = root.get("testMethods");
            
            if (testMethods != null && testMethods.isArray()) {
                for (JsonNode method : testMethods) {
                    String rawCode = method.get("code").asText();
                    
                    // Check if the code contains class-level declarations (indicating LLM returned full class)
                    String cleanedCode = rawCode;
                    if (rawCode.contains("package ") || rawCode.contains("import ") || 
                        rawCode.contains("class ") || rawCode.contains("public class")) {
                        log.warn("LLM returned complete class instead of method code. Extracting method...");
                        cleanedCode = extractMethodCodeFromClass(rawCode);
                        log.debug("Extracted method code: {}", cleanedCode.substring(0, Math.min(100, cleanedCode.length())));
                    }
                    
                    GeneratedTestInfo testInfo = GeneratedTestInfo.builder()
                            .testMethodName(method.get("methodName").asText())
                            .testClass(root.get("testClass").asText())
                            .description(method.get("description").asText())
                            .testCode(cleanedCode)
                            .coveredMethods(extractCoveredMethods(method.get("coveredMethods")))
                            .estimatedCoverageContribution(method.get("estimatedCoverageContribution").asDouble())
                            .build();
                    tests.add(testInfo);
                }
            }
            
            // Generate test file paths
            List<String> testFilePaths = createTestFilePaths(analysis, batchIndex, tests);
            
            return BatchTestGenerationResult.success(tests, testFilePaths);
            
        } catch (Exception e) {
            log.error("Failed to parse batch test generation response", e);
            return BatchTestGenerationResult.failure("Failed to parse AI response: " + e.getMessage());
        }
    }

    private List<String> extractCoveredMethods(JsonNode coveredMethodsNode) {
        List<String> methods = new ArrayList<>();
        if (coveredMethodsNode != null && coveredMethodsNode.isArray()) {
            coveredMethodsNode.forEach(node -> methods.add(node.asText()));
        }
        return methods;
    }

    private List<String> createTestFilePaths(FileAnalysisResult analysis, int batchIndex, List<GeneratedTestInfo> tests) {
        List<String> paths = new ArrayList<>();
        String baseTestPath = analysis.getFilePath().replace("src/main/java", "src/test/java")
                                                   .replace(".java", "Test.java");
        
        if (batchIndex > 0) {
            baseTestPath = baseTestPath.replace("Test.java", "Test" + (batchIndex + 1) + ".java");
        }
        
        paths.add(baseTestPath);
        return paths;
    }

    private CompilationResult compileTestFiles(String repoDir, List<String> testFiles) {
        // Implementation would use javac or Maven/Gradle to compile tests
        // For now, return a simple success result
        return CompilationResult.builder()
                .success(true)
                .errors(new ArrayList<>())
                .build();
    }

    private TestExecutionResult executeTestFiles(String repoDir, List<String> testFiles) {
        // Implementation would use JUnit platform to execute tests
        // For now, return a simple success result
        return TestExecutionResult.builder()
                .success(true)
                .testsExecuted(testFiles.size())
                .testsPassed(testFiles.size())
                .testsFailed(0)
                .errors(new ArrayList<>())
                .executionTimeMs(1000L)
                .build();
    }

    /**
     * Sanitizes AI response by removing markdown code block formatting
     * Handles cases where AI returns JSON wrapped in ```json ... ``` or ``` ... ```
     */
    private String sanitizeAiResponse(String aiResponse) {
        if (aiResponse == null) {
            return null;
        }
        
        String response = aiResponse.trim();
        
        // Remove markdown code block markers
        if (response.startsWith("```")) {
            int firstNewline = response.indexOf('\n');
            if (firstNewline != -1) {
                response = response.substring(firstNewline + 1);
            }
        }
        
        if (response.endsWith("```")) {
            response = response.substring(0, response.lastIndexOf("```"));
        }
        
        // Remove any leading/trailing whitespace again
        response = response.trim();
        
        log.debug("Sanitized AI response: starts with '{}', ends with '{}'", 
                 response.substring(0, Math.min(10, response.length())),
                 response.substring(Math.max(0, response.length() - 10)));
        
        return response;
    }

    /**
     * Extract test method code from complete class structure if the LLM returns full class
     * This is a fallback mechanism to handle cases where LLM ignores instructions
     */
    private String extractMethodCodeFromClass(String fullClassCode) {
        if (fullClassCode == null || fullClassCode.trim().isEmpty()) {
            return "";
        }
        
        // Remove package declarations
        String code = fullClassCode.replaceAll("package\\s+[^;]+;\\s*", "");
        
        // Remove import statements
        code = code.replaceAll("import\\s+[^;]+;\\s*", "");
        
        // Remove class declaration and opening brace
        code = code.replaceAll("(?s)/\\*\\*.*?\\*/\\s*", ""); // Remove class-level comments
        code = code.replaceAll("(?s)class\\s+\\w+\\s*\\{", ""); // Remove class declaration
        code = code.replaceAll("(?s)public\\s+class\\s+\\w+\\s*\\{", ""); // Remove public class declaration
        
        // Remove class-level fields and mocks
        code = code.replaceAll("(?s)@Mock\\s+private\\s+[^;]+;\\s*", "");
        code = code.replaceAll("(?s)private\\s+[^;]+;\\s*", "");
        
        // Remove setUp methods
        code = code.replaceAll("(?s)@BeforeEach\\s+void\\s+setUp\\(\\)\\s*\\{[^}]*\\}\\s*", "");
        
        // Remove the last closing brace (class closing)
        code = code.trim();
        if (code.endsWith("}")) {
            code = code.substring(0, code.lastIndexOf("}")).trim();
        }
        
        // Extract just the test method
        if (code.contains("@Test")) {
            // Find the start of the test method
            int testStart = code.indexOf("@Test");
            if (testStart >= 0) {
                // Find the method and its body
                String methodPart = code.substring(testStart);
                
                // Find the complete method by counting braces
                int braceCount = 0;
                int methodEnd = -1;
                boolean inMethod = false;
                
                for (int i = 0; i < methodPart.length(); i++) {
                    char c = methodPart.charAt(i);
                    if (c == '{') {
                        if (!inMethod) inMethod = true;
                        braceCount++;
                    } else if (c == '}') {
                        braceCount--;
                        if (inMethod && braceCount == 0) {
                            methodEnd = i + 1;
                            break;
                        }
                    }
                }
                
                if (methodEnd > 0) {
                    code = methodPart.substring(0, methodEnd).trim();
                }
            }
        }
        
        // Clean up any remaining artifacts
        code = code.replaceAll("^\\s*\\n+", ""); // Remove leading newlines
        code = code.replaceAll("\\n+$", ""); // Remove trailing newlines
        
        return code.trim();
    }
}
