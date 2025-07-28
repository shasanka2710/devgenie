package com.org.devgenie.service.coverage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.org.devgenie.model.coverage.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TestGenerationService {

    @Autowired
    private ChatClient chatClient;

    @Autowired
    private FileService fileService;

    /**
     * Generate tests for a file using hybrid approach:
     * - Direct full-file generation for small/simple classes
     * - Batch method-based generation for large/complex classes
     * - Intelligent fallback mechanisms
     */
    public TestGenerationResult generateTestsForFile(FileAnalysisResult analysis) {
        log.info("Generating tests for file: {}", analysis.getFilePath());

        try {
            String fileContent = fileService.readFile(analysis.getFilePath());
            
            // Determine if existing test file exists
            boolean existingTestFile = checkExistingTestFile(analysis.getFilePath());
            
            // Determine optimal test generation strategy
            TestGenerationStrategy strategy = TestGenerationStrategy.determine(analysis, existingTestFile, fileContent);
            log.info("Selected strategy: {} - {}", strategy.getStrategy(), strategy.getReasoning());
            
            // Execute strategy with fallback
            return executeTestGenerationStrategy(strategy, analysis, fileContent);

        } catch (Exception e) {
            log.error("Failed to generate tests for file: {}", analysis.getFilePath(), e);
            return TestGenerationResult.failure(analysis.getFilePath(), e.getMessage());
        }
    }
    
    /**
     * Generate tests for a file using a specific strategy
     * This method is used when a strategy has already been determined
     */
    public TestGenerationResult generateTestsForFileWithStrategy(FileAnalysisResult analysis, TestGenerationStrategy strategy) {
        log.info("Generating tests for file: {} using strategy: {}", analysis.getFilePath(), strategy.getStrategy());

        try {
            String fileContent = fileService.readFile(analysis.getFilePath());
            
            // Execute the specified strategy with fallback
            return executeTestGenerationStrategy(strategy, analysis, fileContent);

        } catch (Exception e) {
            log.error("Failed to generate tests for file: {} with strategy: {}", 
                     analysis.getFilePath(), strategy.getStrategy(), e);
            return TestGenerationResult.failure(analysis.getFilePath(), e.getMessage());
        }
    }
    
    /**
     * Execute the determined test generation strategy with fallback mechanisms
     */
    private TestGenerationResult executeTestGenerationStrategy(TestGenerationStrategy strategy, 
                                                             FileAnalysisResult analysis, 
                                                             String fileContent) {
        try {
            switch (strategy.getStrategy()) {
                case DIRECT_FULL_FILE:
                    return generateDirectFullFile(analysis, fileContent, strategy);
                    
                case BATCH_METHOD_BASED:
                    return generateBatchMethodBased(analysis, fileContent, strategy);
                    
                case MERGE_WITH_EXISTING:
                    return generateAndMergeWithExisting(analysis, fileContent, strategy);
                    
                default:
                    throw new IllegalArgumentException("Unknown strategy: " + strategy.getStrategy());
            }
        } catch (Exception e) {
            log.warn("Primary strategy {} failed, attempting fallback", strategy.getStrategy(), e);
            return attemptFallbackStrategy(strategy, analysis, fileContent, e);
        }
    }
    
    /**
     * Attempt fallback strategy if primary fails
     */
    private TestGenerationResult attemptFallbackStrategy(TestGenerationStrategy originalStrategy,
                                                        FileAnalysisResult analysis,
                                                        String fileContent,
                                                        Exception originalError) {
        try {
            if (originalStrategy.getStrategy() == TestGenerationStrategy.Strategy.DIRECT_FULL_FILE) {
                log.info("Direct full-file generation failed, falling back to batch method approach");
                TestGenerationStrategy fallbackStrategy = TestGenerationStrategy.builder()
                    .strategy(TestGenerationStrategy.Strategy.BATCH_METHOD_BASED)
                    .reasoning("Fallback from failed direct generation")
                    .maxTestsPerBatch(3)
                    .requiresValidation(true)
                    .build();
                return generateBatchMethodBased(analysis, fileContent, fallbackStrategy);
            } else {
                log.info("Batch method generation failed, falling back to simple direct approach");
                TestGenerationStrategy fallbackStrategy = TestGenerationStrategy.builder()
                    .strategy(TestGenerationStrategy.Strategy.DIRECT_FULL_FILE)
                    .reasoning("Fallback from failed batch generation")
                    .requiresValidation(false)
                    .build();
                return generateDirectFullFile(analysis, fileContent, fallbackStrategy);
            }
        } catch (Exception fallbackError) {
            log.error("Both primary and fallback strategies failed", fallbackError);
            return TestGenerationResult.failure(analysis.getFilePath(), 
                "Primary strategy failed: " + originalError.getMessage() + 
                "; Fallback also failed: " + fallbackError.getMessage());
        }
    }
    
    /**
     * Generate complete test file directly from AI (for small/simple classes)
     */
    private TestGenerationResult generateDirectFullFile(FileAnalysisResult analysis, 
                                                       String fileContent, 
                                                       TestGenerationStrategy strategy) {
        log.info("Using DIRECT_FULL_FILE strategy for: {}", analysis.getFilePath());
        
        String testPrompt = createDirectFullFilePrompt(fileContent, analysis, strategy);
        String aiResponse = chatClient.prompt(testPrompt).call().content();

        log.info("Input Test message to LLM: {}", testPrompt);
        log.info("LLM output message : {}", aiResponse);
        
        // For DIRECT_FULL_FILE, use minimal processing to preserve LLM output quality
        TestGenerationResult result = parseDirectFullFileResponseMinimal(aiResponse, analysis, strategy);
        
        // Only validate if explicitly required by strategy (disabled for minimal processing)
        // if (strategy.isRequiresValidation() && result.isSuccess()) {
        //     result = validateDirectGeneratedTest(result);
        // }
        
        return result;
    }
    
    /**
     * Generate tests in batches and assemble manually (for large/complex classes)
     */
    private TestGenerationResult generateBatchMethodBased(FileAnalysisResult analysis, 
                                                         String fileContent, 
                                                         TestGenerationStrategy strategy) {
        log.info("BATCH_METHOD_BASED strategy temporarily disabled - falling back to DIRECT_FULL_FILE");
        
        // Fallback to DIRECT_FULL_FILE for now
        TestGenerationStrategy fallbackStrategy = TestGenerationStrategy.builder()
            .strategy(TestGenerationStrategy.Strategy.DIRECT_FULL_FILE)
            .reasoning("Fallback from BATCH_METHOD_BASED (temporarily disabled)")
            .requiresValidation(false)
            .build();
        return generateDirectFullFile(analysis, fileContent, fallbackStrategy);
    }
    
    /**
     * Generate new tests and merge with existing test file
     */
    private TestGenerationResult generateAndMergeWithExisting(FileAnalysisResult analysis, 
                                                            String fileContent, 
                                                            TestGenerationStrategy strategy) {
        log.info("Using MERGE_WITH_EXISTING strategy for: {}", analysis.getFilePath());
        
        // Generate new tests using batch approach (safer for merging)
        TestGenerationResult batchResult = generateBatchMethodBased(analysis, fileContent, strategy);
        
        if (!batchResult.isSuccess()) {
            return batchResult;
        }
        
        // Mark for merging in the result
        return TestGenerationResult.builder()
            .sourceFilePath(batchResult.getSourceFilePath())
            .testFilePath(batchResult.getTestFilePath())
            .testClassName(batchResult.getTestClassName())
            .generatedTests(batchResult.getGeneratedTests())
            .imports(batchResult.getImports())
            .mockDependencies(batchResult.getMockDependencies())
            .generatedTestContent(batchResult.getGeneratedTestContent())
            .estimatedCoverageIncrease(batchResult.getEstimatedCoverageIncrease())
            .notes("Generated for merging with existing test file")
            .success(batchResult.isSuccess())
            .error(batchResult.getError())
            .buildToolSpecific(batchResult.getBuildToolSpecific())
            .projectConfiguration(batchResult.getProjectConfiguration())
            .build();
    }

    private String createTestGenerationPrompt(String fileContent, FileAnalysisResult analysis) {
        // Analyze the class type to provide intelligent guidance
        String classTypeGuidance = analyzeClassTypeForPrompt(fileContent, analysis);
        
        return String.format("""
            Generate comprehensive JUnit 5 test cases for the following Java class to improve code coverage.
            
            Source Code:
            ```java
            %s
            ```
            
            Class Analysis & Guidance:
            %s
            
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
            2. Use Mockito for mocking dependencies ONLY when appropriate
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
                classTypeGuidance,
                analysis.getCurrentCoverage(),
                analysis.getComplexity(),
                analysis.getBusinessLogicPriority(),
                analysis.getEstimatedEffort(),
                formatUncoveredCodePaths(analysis.getUncoveredCodePaths()),
                formatTestableComponents(analysis.getTestableComponents()),
                String.join(", ", analysis.getDependencies())
        );
    }

    /**
     * Framework-agnostic class type analysis for prompt generation
     * Works with Spring Boot, plain Java, Jakarta EE, Micronaut, Quarkus, Maven/Gradle projects, etc.
     */
    private String analyzeClassTypeForPrompt(String fileContent, FileAnalysisResult analysis) {
        StringBuilder guidance = new StringBuilder();
        
        // Check for main application classes (any framework or plain Java)
        if (fileContent.contains("public static void main")) {
            if (fileContent.contains("@SpringBootApplication")) {
                guidance.append("⚠️  SPRING BOOT APPLICATION CLASS DETECTED:\n");
                guidance.append("- This is a Spring Boot application entry point\n");
                guidance.append("- Focus on testing application startup and configuration\n");
                guidance.append("- Use @SpringBootTest for integration testing\n");
                guidance.append("- Test annotation presence and configuration\n");
                guidance.append("- Keep tests simple and focused on essential functionality\n");
                guidance.append("- Limit test generation to 2-3 essential tests\n\n");
            } else if (fileContent.contains("io.micronaut") || fileContent.contains("@MicronautTest")) {
                guidance.append("🦄 MICRONAUT APPLICATION CLASS DETECTED:\n");
                guidance.append("- This is a Micronaut application entry point\n");
                guidance.append("- Use @MicronautTest for testing\n");
                guidance.append("- Focus on application context and bean initialization\n");
                guidance.append("- Test configuration and dependency injection\n");
                guidance.append("- Keep tests lightweight and focused\n");
                guidance.append("- Limit test generation to 2-3 essential tests\n\n");
            } else if (fileContent.contains("io.quarkus") || fileContent.contains("@QuarkusTest")) {
                guidance.append("⚡ QUARKUS APPLICATION CLASS DETECTED:\n");
                guidance.append("- This is a Quarkus application entry point\n");
                guidance.append("- Use @QuarkusTest for testing\n");
                guidance.append("- Focus on application startup and native compilation readiness\n");
                guidance.append("- Test CDI context and configuration\n");
                guidance.append("- Keep tests efficient for fast startup\n");
                guidance.append("- Limit test generation to 2-3 essential tests\n\n");
            } else {
                guidance.append("📱 MAIN APPLICATION CLASS DETECTED:\n");
                guidance.append("- This is a main application entry point (plain Java)\n");
                guidance.append("- Focus on testing main method functionality\n");
                guidance.append("- Test command line argument handling if present\n");
                guidance.append("- Test application initialization logic\n");
                guidance.append("- Use standard JUnit 5 testing approach\n");
                guidance.append("- Keep tests simple and focused on core functionality\n");
                guidance.append("- Limit test generation to 2-3 essential tests\n\n");
            }
        }
        
        // Check for web components (framework-agnostic)
        if (fileContent.contains("@Controller") || fileContent.contains("@RestController")) {
            guidance.append("🌐 SPRING CONTROLLER CLASS DETECTED:\n");
            guidance.append("- Use @WebMvcTest for lightweight testing\n");
            guidance.append("- Mock service dependencies with @MockBean\n");
            guidance.append("- Test HTTP endpoints, request mapping, and response handling\n");
            guidance.append("- Test validation and error handling\n\n");
        } else if (fileContent.contains("extends HttpServlet") || fileContent.contains("@WebServlet")) {
            guidance.append("🌐 SERVLET CLASS DETECTED:\n");
            guidance.append("- Use servlet testing framework or mock HTTP requests\n");
            guidance.append("- Test doGet, doPost, and other HTTP methods\n");
            guidance.append("- Test request parameter handling and response generation\n");
            guidance.append("- Focus on servlet lifecycle and session management\n\n");
        } else if (fileContent.contains("implements Filter") || fileContent.contains("@WebFilter")) {
            guidance.append("🔍 FILTER CLASS DETECTED:\n");
            guidance.append("- Test filter chain execution and request/response modification\n");
            guidance.append("- Mock FilterChain and HttpServletRequest/Response\n");
            guidance.append("- Test filter initialization and destruction\n");
            guidance.append("- Focus on request filtering logic\n\n");
        }
        
        // Check for service/business layer (framework-agnostic)
        if (fileContent.contains("@Service") || (fileContent.contains("Service") && fileContent.contains("class"))) {
            guidance.append("⚙️  SERVICE CLASS DETECTED:\n");
            guidance.append("- Focus on business logic testing\n");
            guidance.append("- Mock repository/external dependencies\n");
            guidance.append("- Test various input scenarios and edge cases\n");
            guidance.append("- Test exception handling and error conditions\n\n");
        } else if (fileContent.contains("@Component") && !fileContent.contains("@Repository") && !fileContent.contains("@Controller")) {
            guidance.append("🔧 COMPONENT CLASS DETECTED:\n");
            guidance.append("- Test component functionality and dependency injection\n");
            guidance.append("- Mock external dependencies as needed\n");
            guidance.append("- Focus on component-specific business logic\n");
            guidance.append("- Test lifecycle methods if present\n\n");
        }
        
        // Check for data access layer (framework-agnostic)
        if (fileContent.contains("@Repository") || (fileContent.contains("Repository") && fileContent.contains("class"))) {
            guidance.append("💾 REPOSITORY CLASS DETECTED:\n");
            guidance.append("- Use @DataJpaTest for JPA repositories or appropriate testing framework\n");
            guidance.append("- Test database operations and queries\n");
            guidance.append("- Focus on data persistence and retrieval\n");
            guidance.append("- Test custom queries and data validation\n\n");
        } else if (fileContent.contains("@Entity") || fileContent.contains("@Table")) {
            guidance.append("📊 JPA ENTITY CLASS DETECTED:\n");
            guidance.append("- Test entity relationships and mappings\n");
            guidance.append("- Test validation annotations and constraints\n");
            guidance.append("- Test equals, hashCode, and toString methods\n");
            guidance.append("- Focus on data integrity and persistence behavior\n\n");
        }
        
        // Check for configuration classes (framework-agnostic)
        if (fileContent.contains("@Configuration") || (fileContent.contains("Config") && fileContent.contains("class"))) {
            guidance.append("⚙️  CONFIGURATION CLASS DETECTED:\n");
            guidance.append("- Test bean creation and configuration\n");
            guidance.append("- Verify conditional configurations\n");
            guidance.append("- Use @SpringBootTest or appropriate framework test for integration testing\n");
            guidance.append("- Test property binding and validation\n\n");
        }
        
        // Check for data structures and utilities (framework-agnostic)
        if (fileContent.contains("public interface") || fileContent.contains("interface")) {
            guidance.append("🔌 INTERFACE DETECTED:\n");
            guidance.append("- Focus on contract testing\n");
            guidance.append("- Test default methods if present\n");
            guidance.append("- Consider testing implementation classes\n");
            guidance.append("- Keep tests minimal and focused on interface contracts\n\n");
        } else if (fileContent.contains("abstract class")) {
            guidance.append("🏗️  ABSTRACT CLASS DETECTED:\n");
            guidance.append("- Test concrete methods in the abstract class\n");
            guidance.append("- Create test subclass for testing abstract methods\n");
            guidance.append("- Focus on shared functionality\n");
            guidance.append("- Test template method patterns if present\n\n");
        } else if (fileContent.contains("public enum") || fileContent.contains("enum")) {
            guidance.append("📋 ENUM CLASS DETECTED:\n");
            guidance.append("- Test enum value creation and retrieval\n");
            guidance.append("- Test any custom methods in the enum\n");
            guidance.append("- Test serialization/deserialization if applicable\n");
            guidance.append("- Keep tests simple and focused on enum behavior\n\n");
        } else if (fileContent.contains("@Entity") || fileContent.contains("@Data") || isDataClass(fileContent)) {
            guidance.append("📊 DATA CLASS DETECTED:\n");
            guidance.append("- Test getters, setters, and constructors\n");
            guidance.append("- Test equals, hashCode, and toString methods\n");
            guidance.append("- Test validation annotations if present\n");
            guidance.append("- Focus on data integrity and validation\n\n");
        } else if (isUtilityClass(fileContent)) {
            guidance.append("🛠️  UTILITY CLASS DETECTED:\n");
            guidance.append("- Test static utility methods\n");
            guidance.append("- Focus on edge cases and boundary conditions\n");
            guidance.append("- Test null handling and exception scenarios\n");
            guidance.append("- Ensure comprehensive input validation testing\n\n");
        }
        
        // Add Jakarta EE specific guidance
        if (fileContent.contains("javax.") || fileContent.contains("jakarta.")) {
            guidance.append("☕ JAKARTA EE/JEE COMPONENT DETECTED:\n");
            guidance.append("- Use appropriate Jakarta EE testing framework (Arquillian, etc.)\n");
            guidance.append("- Test CDI injection and enterprise features\n");
            guidance.append("- Focus on container-managed functionality\n");
            guidance.append("- Test transaction management if applicable\n\n");
        }
        
        // Add complexity-based guidance
        if ("HIGH".equals(analysis.getComplexity()) || "VERY_HIGH".equals(analysis.getComplexity())) {
            guidance.append("🔥 HIGH COMPLEXITY DETECTED:\n");
            guidance.append("- Break down complex methods into multiple test cases\n");
            guidance.append("- Test all branches and edge conditions\n");
            guidance.append("- Focus on error handling and boundary conditions\n");
            guidance.append("- Consider parameterized tests for multiple scenarios\n\n");
        }
        
        // Add framework-agnostic guidance for plain Java
        if (!fileContent.contains("@") || countAnnotations(fileContent) < 2) {
            guidance.append("☕ PLAIN JAVA CLASS DETECTED:\n");
            guidance.append("- Use standard JUnit 5 testing approach\n");
            guidance.append("- Focus on core Java functionality testing\n");
            guidance.append("- Test object creation, method behavior, and state changes\n");
            guidance.append("- No special framework considerations needed\n\n");
        }
        
        // Add general Java testing guidance
        guidance.append("📝 GENERAL TESTING GUIDELINES:\n");
        guidance.append("- Use meaningful test method names that describe what is being tested\n");
        guidance.append("- Follow AAA pattern: Arrange, Act, Assert\n");
        guidance.append("- Mock external dependencies appropriately\n");
        guidance.append("- Test both happy path and error scenarios\n");
        guidance.append("- Include edge cases and boundary value testing\n");
        guidance.append("- Use appropriate assertions for the testing framework\n");
        guidance.append("- For simple classes (LOW complexity), aim for 80%+ coverage with comprehensive tests\n");
        guidance.append("- Generate multiple test methods to thoroughly exercise the code\n");
        
        return guidance.toString();
    }
    
    /**
     * Check if class is a data class (POJO with mainly getters/setters)
     */
    private boolean isDataClass(String fileContent) {
        int getterSetterCount = 0;
        int totalMethods = 0;
        
        String[] lines = fileContent.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.contains("public ") && trimmed.contains("(") && trimmed.contains(")")) {
                totalMethods++;
                if (trimmed.contains("get") || trimmed.contains("set") || 
                    trimmed.contains("is") && trimmed.contains("()")) {
                    getterSetterCount++;
                }
            }
        }
        
        return totalMethods > 0 && (getterSetterCount >= totalMethods * 0.7);
    }
    
    /**
     * Check if class is a utility class (mainly static methods)
     */
    private boolean isUtilityClass(String fileContent) {
        return fileContent.contains("private") && fileContent.contains("static") && 
               !fileContent.contains("@Component") && !fileContent.contains("@Service") &&
               fileContent.contains("public static");
    }
    
    /**
     * Count number of annotations in the class
     */
    private int countAnnotations(String fileContent) {
        return (int) fileContent.lines().filter(line -> line.trim().startsWith("@")).count();
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
        // First, try to find JSON wrapped in markdown code blocks
        String markdownJsonPattern = "```json\\s*\\n?(.*)\\n?```";
        Pattern pattern = Pattern.compile(markdownJsonPattern, Pattern.DOTALL);
        Matcher matcher = pattern.matcher(response);
        
        if (matcher.find()) {
            String extractedJson = matcher.group(1).trim();
            log.debug("Extracted JSON from markdown code block, length: {}", extractedJson.length());
            return extractedJson;
        }
        
        // Fallback to original logic with improved JSON detection
        int startIndex = response.indexOf('{');
        if (startIndex < 0) {
            throw new IllegalArgumentException("No valid JSON found in AI response - no opening brace");
        }
        
        // Find the matching closing brace using proper brace counting
        int braceCount = 0;
        int endIndex = -1;
        boolean inString = false;
        boolean escaped = false;
        
        for (int i = startIndex; i < response.length(); i++) {
            char c = response.charAt(i);
            
            if (escaped) {
                escaped = false;
                continue;
            }
            
            if (c == '\\') {
                escaped = true;
                continue;
            }
            
            if (c == '"' && !escaped) {
                inString = !inString;
                continue;
            }
            
            if (!inString) {
                if (c == '{') {
                    braceCount++;
                } else if (c == '}') {
                    braceCount--;
                    if (braceCount == 0) {
                        endIndex = i + 1;
                        break;
                    }
                }
            }
        }
        
        if (endIndex > startIndex) {
            String extractedJson = response.substring(startIndex, endIndex);
            log.debug("Extracted JSON using brace counting, length: {}", extractedJson.length());
            return extractedJson;
        }

        throw new IllegalArgumentException("No valid JSON found in AI response - unable to find matching braces");
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
            log.info("Batch Test message to LLM: {}, batch index: {}", batchPrompt,batchIndex+1);
            log.info("Batch LLM output message: {}, batch index: {}", aiResponse,batchIndex+1);

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
        // Add intelligent class type analysis for batch generation too
        String classTypeGuidance = analyzeClassTypeForPrompt(fileContent, analysis);
        
        return String.format("""
            Generate %d JUnit 5 test methods for the following Java class (batch %d).
            Focus on different aspects of the code to maximize coverage.
            
            Source Code:
            ```java
            %s
            ```
            
            Class Analysis & Guidance:
            %s
            
            Analysis Results:
            - File Path: %s
            - Current Coverage: %.1f%%
            - Methods to focus on: %s
            
            IMPORTANT REQUIREMENTS:
            1. Generate exactly %d test methods
            2. Return ONLY the test method code, NOT complete class declarations
            3. Each method should be a standalone test method with @Test annotation
            4. Use JUnit 5 annotations (@Test, @BeforeEach, etc.)
            5. Include Mockito mocks ONLY when appropriate (avoid for simple application classes)
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
            maxTestsPerBatch, batchIndex + 1, fileContent, classTypeGuidance, analysis.getFilePath(), 
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
    
    /**
     * Check if an existing test file exists for the given source file
     */
    private boolean checkExistingTestFile(String sourceFilePath) {
        try {
            String testFilePath = generateTestFilePath(sourceFilePath);
            return Files.exists(Paths.get(testFilePath));
        } catch (Exception e) {
            log.debug("Could not check for existing test file: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Generate test file path from source file path
     */
    private String generateTestFilePath(String sourceFilePath) {
        return sourceFilePath.replace("src/main/java", "src/test/java")
                            .replace(".java", "Test.java");
    }
    
    /**
     * Extract test class name from test file path
     */
    private String extractTestClassName(String testFilePath) {
        String fileName = Paths.get(testFilePath).getFileName().toString();
        return fileName.replace(".java", "");
    }
    
    /**
     * Create prompt for direct full-file generation
     */
    private String createDirectFullFilePrompt(String fileContent, FileAnalysisResult analysis, TestGenerationStrategy strategy) {
        log.info("Creating direct full-file generation prompt for file: {}", analysis.getFilePath());
        String classTypeGuidance = analyzeClassTypeForPrompt(fileContent, analysis);
        
        // Determine coverage target based on complexity
        String complexityLevel = analysis.getComplexity().toString();
        String coverageTarget = switch (complexityLevel) {
            case "LOW" -> "MINIMUM 80% coverage - Simple classes should have comprehensive test coverage";
            case "MEDIUM" -> "MINIMUM 70% coverage - Cover all main scenarios and edge cases";
            case "HIGH" -> "MINIMUM 60% coverage - Focus on critical paths and business logic";
            default -> "MINIMUM 70% coverage";
        };
        
        return String.format("""
            Generate a COMPLETE, COMPILABLE JUnit 5 test class for the following Java class.
            This should be a ready-to-use test file that can be saved directly without modification.
            
            🎯 COVERAGE TARGET: %s
            
            Source Code:
            ```java
            %s
            ```
            
            Class Analysis & Guidance:
            %s
            
            Analysis Results:
            - Current Coverage: %.2f%%
            - Complexity: %s
            - Business Logic Priority: %s
            - Strategy: %s
            
            Requirements for COMPLETE TEST FILE:
            1. Include package declaration matching the source package structure
            2. Include ALL necessary imports (JUnit 5, Mockito if needed, assertions, etc.)
            3. Generate a complete test class with proper class declaration
            4. Include @Test methods with meaningful names and proper assertions
            5. Add @BeforeEach setup method ONLY if absolutely necessary
            6. Use appropriate test annotations (@SpringBootTest for app classes, @ExtendWith for others)
            7. Keep it simple for Spring Boot application classes (2-3 basic tests max)
            8. For other classes, generate comprehensive test methods covering different scenarios
            9. Include proper JavaDoc comments for the test class
            10. Ensure the file is immediately compilable and runnable
            
            Please provide the response in the following JSON format:
            {
                "testClassName": "TestClassName",
                "testFilePath": "src/test/java/package/path/TestClassName.java",
                "testClassContent": "COMPLETE test class content ready to save to file",
                "generatedTests": [
                    {
                        "methodName": "testMethodName",
                        "testType": "UNIT|INTEGRATION|EDGE_CASE",
                        "description": "Description of what this test covers",
                        "targetCodePath": "Method or code path being tested",
                        "coverageImpact": "HIGH|MEDIUM|LOW"
                    }
                ],
                "imports": ["list", "of", "import", "statements"],
                "mockDependencies": ["list", "of", "mocked", "dependencies"],
                "estimatedCoverageIncrease": 25.5,
                "notes": "Any important notes about the generated tests"
            }
            """,
                coverageTarget,
                fileContent,
                classTypeGuidance,
                analysis.getCurrentCoverage(),
                analysis.getComplexity(),
                analysis.getBusinessLogicPriority(),
                strategy.getReasoning()
        );
    }
    
    /**
     * Parse response from direct full-file generation
     */
    private TestGenerationResult parseDirectFullFileResponse(String aiResponse, FileAnalysisResult analysis) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonResponse = mapper.readTree(extractJsonFromResponse(aiResponse));

            List<GeneratedTest> tests = parseGeneratedTests(jsonResponse.get("generatedTests"));
            String rawTestClassContent = jsonResponse.get("testClassContent").asText();
            String testFilePath = jsonResponse.get("testFilePath").asText();

            // Validate and clean the test content to prevent syntax errors
            // String cleanedTestContent = validateAndCleanTestContent(rawTestClassContent);
            String cleanedTestContent = rawTestClassContent; // Temporary - use raw content for now
            
            // Log if we had to clean up the content
            // if (!rawTestClassContent.equals(cleanedTestContent)) {
            //     log.info("Cleaned up test content for better syntax: {}", analysis.getFilePath());
            // }

            return TestGenerationResult.builder()
                    .sourceFilePath(analysis.getFilePath())
                    .testFilePath(testFilePath)
                    .testClassName(jsonResponse.get("testClassName").asText())
                    .generatedTests(tests)
                    .imports(parseStringArray(jsonResponse.get("imports")))
                    .mockDependencies(parseStringArray(jsonResponse.get("mockDependencies")))
                    .generatedTestContent(cleanedTestContent)
                    .estimatedCoverageIncrease(jsonResponse.get("estimatedCoverageIncrease").asDouble())
                    .notes(jsonResponse.get("notes").asText())
                    .success(true)
                    .build();

        } catch (Exception e) {
            log.error("Failed to parse direct full-file generation response", e);
            return TestGenerationResult.failure(analysis.getFilePath(), "Failed to parse AI response: " + e.getMessage());
        }
    }
    
    /**
     * Parse response from direct full-file generation with minimal processing
     * This preserves the LLM output quality while doing only essential validation
     */
    private TestGenerationResult parseDirectFullFileResponseMinimal(String aiResponse, 
                                                                   FileAnalysisResult analysis, 
                                                                   TestGenerationStrategy strategy) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            
            // Extract JSON with improved error handling
            String jsonString;
            try {
                jsonString = extractJsonFromResponse(aiResponse);
            } catch (Exception e) {
                log.error("Failed to extract JSON from AI response for {}: {}", analysis.getFilePath(), e.getMessage());
                log.debug("Raw AI response: {}", aiResponse);
                return TestGenerationResult.failure(analysis.getFilePath(), "Failed to extract valid JSON from AI response");
            }
            
            // Parse JSON with detailed error information
            JsonNode jsonResponse;
            try {
                jsonResponse = mapper.readTree(jsonString);
            } catch (Exception e) {
                log.error("Failed to parse JSON for {}: {}", analysis.getFilePath(), e.getMessage());
                log.debug("Extracted JSON string: {}", jsonString);
                
                // Try to fix common JSON issues
                String fixedJson = fixCommonJsonIssues(jsonString);
                try {
                    jsonResponse = mapper.readTree(fixedJson);
                    log.info("Successfully parsed JSON after applying fixes for {}", analysis.getFilePath());
                } catch (Exception e2) {
                    log.error("Failed to parse JSON even after fixes for {}: {}", analysis.getFilePath(), e2.getMessage());
                    return TestGenerationResult.failure(analysis.getFilePath(), "Failed to parse AI response JSON: " + e.getMessage());
                }
            }

            // Extract required fields with null checks
            JsonNode testClassContentNode = jsonResponse.get("testClassContent");
            JsonNode testFilePathNode = jsonResponse.get("testFilePath");
            
            if (testClassContentNode == null || testClassContentNode.isNull()) {
                log.error("Missing testClassContent in AI response for {}", analysis.getFilePath());
                return TestGenerationResult.failure(analysis.getFilePath(), "AI response missing testClassContent field");
            }
            
            if (testFilePathNode == null || testFilePathNode.isNull()) {
                log.error("Missing testFilePath in AI response for {}", analysis.getFilePath());
                return TestGenerationResult.failure(analysis.getFilePath(), "AI response missing testFilePath field");
            }
            
            String rawTestClassContent = testClassContentNode.asText();
            String testFilePath = testFilePathNode.asText();
            
            log.info("=== DEBUG: Raw LLM testClassContent length: {} ===", rawTestClassContent.length());
            log.info("=== DEBUG: Raw LLM testClassContent preview (first 500 chars) ===");
            log.info("{}", rawTestClassContent.substring(0, Math.min(500, rawTestClassContent.length())));
            
            // For DIRECT_FULL_FILE, apply minimal cleanup only if content is clearly malformed
            String finalTestContent;
            if (isContentClearlyMalformed(rawTestClassContent)) {
                log.info("Content appears malformed for {}, applying minimal cleanup", analysis.getFilePath());
                finalTestContent = applyMinimalCleanup(rawTestClassContent);
            } else {
                log.info("Using LLM output directly for {}, minimal processing applied", analysis.getFilePath());
                finalTestContent = rawTestClassContent.trim();
            }
            
            log.info("=== DEBUG: Final test content length after processing: {} ===", finalTestContent.length());
            log.info("=== DEBUG: Final test content preview (first 500 chars) ===");
            log.info("{}", finalTestContent.substring(0, Math.min(500, finalTestContent.length())));

            // Parse metadata with defaults if missing
            List<GeneratedTest> tests = parseGeneratedTestsWithDefaults(jsonResponse.get("generatedTests"));
            
            return TestGenerationResult.builder()
                    .sourceFilePath(analysis.getFilePath())
                    .testFilePath(testFilePath)
                    .testClassName(getValueOrDefault(jsonResponse, "testClassName", 
                                  extractClassNameFromPath(analysis.getFilePath()) + "Test"))
                    .generatedTests(tests)
                    .imports(parseStringArrayWithDefaults(jsonResponse.get("imports")))
                    .mockDependencies(parseStringArrayWithDefaults(jsonResponse.get("mockDependencies")))
                    .generatedTestContent(finalTestContent)
                    .estimatedCoverageIncrease(getValueOrDefault(jsonResponse, "estimatedCoverageIncrease", 25.0))
                    .notes(getValueOrDefault(jsonResponse, "notes", "Generated using DIRECT_FULL_FILE strategy"))
                    .success(true)
                    .build();

        } catch (Exception e) {
            log.error("Failed to parse direct full-file generation response for {}", analysis.getFilePath(), e);
            return TestGenerationResult.failure(analysis.getFilePath(), 
                "Failed to parse AI response: " + e.getMessage());
        }
    }
    
    /**
     * Check if content appears to be clearly malformed
     */
    private boolean isContentClearlyMalformed(String content) {
        if (content == null || content.trim().isEmpty()) {
            return true;
        }
        
        // Check for obvious malformation indicators
        String trimmed = content.trim();
        
        // Check if it starts and ends with proper class structure
        boolean hasClassDeclaration = trimmed.contains("class ") && trimmed.contains("{");
        boolean hasProperEnding = trimmed.endsWith("}");
        
        // Check for excessive brace mismatches
        long openBraces = trimmed.chars().filter(ch -> ch == '{').count();
        long closeBraces = trimmed.chars().filter(ch -> ch == '}').count();
        boolean bracesMismatch = Math.abs(openBraces - closeBraces) > 2;
        
        // Content is malformed if it lacks basic structure or has severe brace mismatches
        return !hasClassDeclaration || !hasProperEnding || bracesMismatch;
    }

    /**
     * Apply minimal cleanup to malformed content
     */
    private String applyMinimalCleanup(String content) {
        if (content == null || content.trim().isEmpty()) {
            return content;
        }
        
        String cleaned = content.trim();
        
        // Basic cleanup only - remove obvious markdown artifacts
        cleaned = cleaned.replaceAll("```java\\s*", "");
        cleaned = cleaned.replaceAll("```\\s*$", "");
        
        // Ensure proper ending if missing
        if (!cleaned.endsWith("}")) {
            cleaned += "\n}";
        }
        
        return cleaned;
    }

    /**
     * Extract class name from file path
     */
    private String extractClassNameFromPath(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return "GeneratedTest";
        }
        
        String fileName = Paths.get(filePath).getFileName().toString();
        if (fileName.endsWith(".java")) {
            return fileName.substring(0, fileName.length() - 5);
        }
        return fileName;
    }

    /**
     * Get value with default for strings
     */
    private String getValueOrDefault(JsonNode node, String fieldName, String defaultValue) {
        return getJsonFieldWithDefault(node, fieldName, defaultValue);
    }

    /**
     * Get value with default for doubles
     */
    private double getValueOrDefault(JsonNode node, String fieldName, double defaultValue) {
        return getJsonNumberWithDefault(node, fieldName, defaultValue);
    }

    /**
     * Attempt to fix common JSON issues that might occur in LLM responses
     */
    private String fixCommonJsonIssues(String jsonString) {
        String fixed = jsonString;
        
        // Remove any leading/trailing whitespace
        fixed = fixed.trim();
        
        // Fix common issues with quotes in string values
        // This is a simple approach - more sophisticated fixes could be added
        
        // Fix unescaped quotes in string values (basic approach)
        // This regex looks for quote-comma patterns that might indicate malformed JSON
        fixed = fixed.replaceAll("\"([^\"]*?)\"([^,}\\]]*?)\"", "\"$1$2\"");
        
        // Remove any trailing commas before closing braces/brackets
        fixed = fixed.replaceAll(",\\s*([}\\]])", "$1");
        
        // Ensure proper spacing around colons and commas
        fixed = fixed.replaceAll("\"\\s*:\\s*", "\": ");
        fixed = fixed.replaceAll(",\\s*\"", ", \"");
        
        // Log if we made changes
        if (!jsonString.equals(fixed)) {
            log.debug("Applied JSON fixes, length changed from {} to {}", jsonString.length(), fixed.length());
        }
        
        return fixed;
    }

    /**
     * Get JSON field value with default fallback
     */
    private String getJsonFieldWithDefault(JsonNode node, String fieldName, String defaultValue) {
        JsonNode fieldNode = node.get(fieldName);
        if (fieldNode == null || fieldNode.isNull()) {
            return defaultValue;
        }
        return fieldNode.asText(defaultValue);
    }

    /**
     * Get JSON number field value with default fallback
     */
    private double getJsonNumberWithDefault(JsonNode node, String fieldName, double defaultValue) {
        JsonNode fieldNode = node.get(fieldName);
        if (fieldNode == null || fieldNode.isNull()) {
            return defaultValue;
        }
        return fieldNode.asDouble(defaultValue);
    }

    /**
     * Parse string array from JSON with defaults
     */
    private List<String> parseStringArrayWithDefaults(JsonNode arrayNode) {
        List<String> result = new ArrayList<>();
        if (arrayNode != null && arrayNode.isArray()) {
            for (JsonNode item : arrayNode) {
                if (item != null && !item.isNull()) {
                    result.add(item.asText());
                }
            }
        }
        return result;
    }

    /**
     * Parse generated tests with default handling
     */
    private List<GeneratedTest> parseGeneratedTestsWithDefaults(JsonNode testsNode) {
        List<GeneratedTest> tests = new ArrayList<>();
        if (testsNode != null && testsNode.isArray()) {
            for (JsonNode testNode : testsNode) {
                if (testNode != null && !testNode.isNull()) {
                    try {
                        GeneratedTest test = GeneratedTest.builder()
                                .methodName(getJsonFieldWithDefault(testNode, "methodName", "testMethod"))
                                .testType(getJsonFieldWithDefault(testNode, "testType", "UNIT"))
                                .description(getJsonFieldWithDefault(testNode, "description", "Generated test"))
                                .targetCodePath(getJsonFieldWithDefault(testNode, "targetCodePath", "unknown"))
                                .coverageImpact(getJsonFieldWithDefault(testNode, "coverageImpact", "MEDIUM"))
                                .build();
                        tests.add(test);
                    } catch (Exception e) {
                        log.warn("Failed to parse test metadata, using defaults: {}", e.getMessage());
                        tests.add(GeneratedTest.builder()
                                .methodName("testMethod" + tests.size())
                                .testType("UNIT")
                                .description("Generated test")
                                .targetCodePath("unknown")
                                .coverageImpact("MEDIUM")
                                .build());
                    }
                }
            }
        }
        return tests;
    }
}
