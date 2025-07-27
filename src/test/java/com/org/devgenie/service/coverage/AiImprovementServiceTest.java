package com.org.devgenie.service.coverage;

import com.org.devgenie.model.coverage.CoverageData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the {@link AiImprovementService} class.
 * These tests cover the generation of improvement opportunities, including AI-powered
 * suggestions and rule-based fallbacks, as well as AI response parsing and various
 * coverage data scenarios.
 */
@ExtendWith(MockitoExtension.class)
class AiImprovementServiceTest {

    private AiImprovementService aiImprovementService;

    @Mock
    private ChatClient chatClient;
    @Mock
    private ChatClient.PromptSpec promptSpec;
    @Mock
    private ChatClient.CallSpec callSpec;
    @Mock
    private ChatResponse chatResponse;
    @Mock
    private Generation generation;

    @BeforeEach
    void setUp() {
        aiImprovementService = new AiImprovementService();
        // Use reflection to inject the mock chatClient into the private field.
        // This is necessary for unit testing classes with @Autowired private fields
        // without a constructor for injection in a pure JUnit context (no Spring Boot test harness).
        try {
            java.lang.reflect.Field chatClientField = AiImprovementService.class.getDeclaredField("chatClient");
            chatClientField.setAccessible(true);
            chatClientField.set(aiImprovementService, chatClient);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Failed to inject mock ChatClient: " + e.getMessage());
        }

        // Common mock setup for ChatClient chain
        when(chatClient.prompt()).thenReturn(promptSpec);
        when(promptSpec.user(anyString())).thenReturn(callSpec);
        when(callSpec.call()).thenReturn(chatResponse);
        when(chatResponse.getGenerations()).thenReturn(Collections.singletonList(generation));
    }

    /**
     * Tests the main scenario where the AI service successfully returns valid improvement suggestions.
     * Verifies that AI opportunities are parsed correctly and rule-based opportunities are also included.
     */
    @Test
    void generateImprovementOpportunities_successfulAiResponse() {
        // Arrange
        CoverageData coverageData = createTestCoverageData(
                "TestFile.java", "TestClass", "com.example",
                50, 100, 50.0, // Line coverage 50%
                10, 20, 50.0,  // Branch coverage 50%
                5, 10, 50.0    // Method coverage 50%
        );

        String aiResponseContent = """
            OPPORTUNITY: UNCOVERED_CODE - Test the new feature module - Priority: HIGH - Estimated Impact: +10%
            OPPORTUNITY: EDGE_CASES - Add tests for null inputs - Priority: MEDIUM - Estimated Impact: +5%
            Some random text here.
            OPPORTUNITY: PERFORMANCE - Optimize loop for large datasets - Priority: LOW - Estimated Impact: +2%
            """;
        when(generation.getContent()).thenReturn(aiResponseContent);

        // Act
        List<AiImprovementService.ImprovementOpportunity> opportunities =
                aiImprovementService.generateImprovementOpportunities(coverageData);

        // Assert
        assertNotNull(opportunities);
        // Expect 3 AI opportunities + 4 rule-based opportunities (low line, low branch, uncovered methods, complex file)
        assertEquals(7, opportunities.size());

        // Verify AI opportunities
        assertTrue(opportunities.stream().anyMatch(o -> "UNCOVERED_CODE".equals(o.getType()) && "Test the new feature module".equals(o.getDescription())));
        assertTrue(opportunities.stream().anyMatch(o -> "EDGE_CASES".equals(o.getType()) && "Add tests for null inputs".equals(o.getDescription())));
        assertTrue(opportunities.stream().anyMatch(o -> "PERFORMANCE".equals(o.getType()) && "Optimize loop for large datasets".equals(o.getDescription())));

        // Verify that chatClient was called with the correct prompt
        verify(promptSpec).user(argThat(prompt ->
                prompt.contains("File: TestFile.java") &&
                prompt.contains("Line Coverage: 50.0%") &&
                prompt.contains("Branch Coverage: 50.0%") &&
                prompt.contains("Method Coverage: 50.0%")
        ));
    }

    /**
     * Tests the scenario where the AI response contains malformed lines or lines missing optional parts.
     * Verifies that only correctly formatted AI opportunities are parsed and rule-based opportunities are included.
     * Also covers default priority/impact values when not provided by AI.
     */
    @Test
    void generateImprovementOpportunities_aiResponseParsingError() {
        // Arrange
        CoverageData coverageData = createTestCoverageData(
                "TestFile.java", "TestClass", "com.example",
                50, 100, 50.0, // Line coverage 50%
                10, 20, 50.0,  // Branch coverage 50%
                5, 10, 50.0    // Method coverage 50%
        );

        String aiResponseContent = """
            OPPORTUNITY: Valid - Full - Priority: HIGH - Estimated Impact: +5%
            OPPORTUNITY: OnlyPriority - Description - Priority: MEDIUM
            OPPORTUNITY: OnlyTypeAndDescription - Simple Description
            OPPORTUNITY: Malformed - Too Few
            Random text not starting with OPPORTUNITY.
            """;
        when(generation.getContent()).thenReturn(aiResponseContent);

        // Act
        List<AiImprovementService.ImprovementOpportunity> opportunities =
                aiImprovementService.generateImprovementOpportunities(coverageData);

        // Assert
        assertNotNull(opportunities);
        // Expect 2 valid AI opportunities (Valid - Full, OnlyPriority) + 4 rule-based opportunities
        // "OnlyTypeAndDescription" and "Malformed" will be skipped by parseOpportunityLine as they have < 3 parts.
        assertEquals(6, opportunities.size());

        // Verify the fully parsed AI opportunity
        assertTrue(opportunities.stream().anyMatch(o ->
                "Valid".equals(o.getType()) &&
                "Full".equals(o.getDescription()) &&
                "HIGH".equals(o.getPriority()) &&
                "+5%".equals(o.getEstimatedImpact())));
        
        // Verify the AI opportunity with default estimated impact
        assertTrue(opportunities.stream().anyMatch(o ->
                "OnlyPriority".equals(o.getType()) &&
                "Description".equals(o.getDescription()) &&
                "MEDIUM".equals(o.getPriority()) &&
                "+5%".equals(o.getEstimatedImpact()))); // Default impact
        
        // Verify that malformed or incomplete AI opportunities were NOT added
        assertFalse(opportunities.stream().anyMatch(o -> "OnlyTypeAndDescription".equals(o.getType())));
        assertFalse(opportunities.stream().anyMatch(o -> "Malformed".equals(o.getType())));

        verify(promptSpec, atLeastOnce()).user(anyString());
    }

    /**
     * Tests the scenario where the AI service throws an exception during the call.
     * Verifies that the service logs a warning and gracefully falls back to generating
     * only rule-based opportunities.
     */
    @Test
    void generateImprovementOpportunities_aiExceptionFallbackToRules() {
        // Arrange
        CoverageData coverageData = createTestCoverageData(
                "TestFile.java", "TestClass", "com.example",
                50, 100, 50.0, // Line coverage 50%
                10, 20, 50.0,  // Branch coverage 50%
                5, 10, 50.0    // Method coverage 50%
        );

        // Simulate an exception from the AI call
        when(callSpec.call()).thenThrow(new RuntimeException("AI service unavailable"));

        // Act
        List<AiImprovementService.ImprovementOpportunity> opportunities =
                aiImprovementService.generateImprovementOpportunities(coverageData);

        // Assert
        assertNotNull(opportunities);
        // Expect only rule-based opportunities (4 rules triggered by this coverage data)
        assertEquals(4, opportunities.size());
        assertTrue(opportunities.stream().anyMatch(o -> "UNCOVERED_METHODS".equals(o.getType())));
        assertTrue(opportunities.stream().anyMatch(o -> "EDGE_CASES".equals(o.getType())));
        assertTrue(opportunities.stream().anyMatch(o -> "OVERALL_IMPROVEMENT".equals(o.getType())));
        assertTrue(opportunities.stream().anyMatch(o -> "COMPLEX_FILE".equals(o.getType())));

        verify(promptSpec).user(anyString()); // Prompt was still attempted
        // Note: Verifying log.warn requires a specific log appender setup, which is out of scope for a simple unit test.
    }

    /**
     * Tests the scenario where the AI response content is an empty string.
     * Verifies that the service falls back to generating only rule-based opportunities.
     */
    @Test
    void generateImprovementOpportunities_emptyAiResponseFallbackToRules() {
        // Arrange
        CoverageData coverageData = createTestCoverageData(
                "TestFile.java", "TestClass", "com.example",
                50, 100, 50.0, // Line coverage 50%
                10, 20, 50.0,  // Branch coverage 50%
                5, 10, 50.0    // Method coverage 50%
        );
        when(generation.getContent()).thenReturn(""); // Empty AI response

        // Act
        List<AiImprovementService.ImprovementOpportunity> opportunities =
                aiImprovementService.generateImprovementOpportunities(coverageData);

        // Assert
        assertNotNull(opportunities);
        // Expect only rule-based opportunities
        assertEquals(4, opportunities.size());
        assertTrue(opportunities.stream().anyMatch(o -> "UNCOVERED_METHODS".equals(o.getType())));
        assertTrue(opportunities.stream().anyMatch(o -> "EDGE_CASES".equals(o.getType())));
        assertTrue(opportunities.stream().anyMatch(o -> "OVERALL_IMPROVEMENT".equals(o.getType())));
        assertTrue(opportunities.stream().anyMatch(o -> "COMPLEX_FILE".equals(o.getType())));

        verify(promptSpec).user(anyString());
    }

    /**
     * Tests the scenario where the AI response contains no generations (effectively null content).
     * Verifies that the service falls back to generating only rule-based opportunities.
     */
    @Test
    void generateImprovementOpportunities_nullAiResponseFallbackToRules() {
        // Arrange
        CoverageData coverageData = createTestCoverageData(
                "TestFile.java", "TestClass", "com.example",
                50, 100, 50.0, // Line coverage 50%
                10, 20, 50.0,  // Branch coverage 50%
                5, 10, 50.0    // Method coverage 50%
        );
        when(chatResponse.getGenerations()).thenReturn(Collections.emptyList()); // No generations means no content to parse

        // Act
        List<AiImprovementService.ImprovementOpportunity> opportunities =
                aiImprovementService.generateImprovementOpportunities(coverageData);

        // Assert
        assertNotNull(opportunities);
        // Expect only rule-based opportunities
        assertEquals(4, opportunities.size());
        assertTrue(opportunities.stream().anyMatch(o -> "UNCOVERED_METHODS".equals(o.getType())));
        assertTrue(opportunities.stream().anyMatch(o -> "EDGE_CASES".equals(o.getType())));
        assertTrue(opportunities.stream().anyMatch(o -> "OVERALL_IMPROVEMENT".equals(o.getType())));
        assertTrue(opportunities.stream().anyMatch(o -> "COMPLEX_FILE".equals(o.getType())));

        verify(promptSpec).user(anyString());
    }

    /**
     * Tests the `generateRuleBasedOpportunities` logic when all possible rules are triggered.
     * This is tested by providing coverage data that meets all criteria and ensuring no AI response.
     */
    @Test
    void generateImprovementOpportunities_ruleBased_allRulesTriggered() {
        // Arrange
        // Low coverage across the board, complex file size
        CoverageData coverageData = createTestCoverageData(
                "TestFile.java", "TestClass", "com.example",
                50, 100, 50.0, // Line coverage: 50% (triggers Rule 3 & 4)
                10, 20, 50.0,  // Branch coverage: 50% (triggers Rule 2)
                5, 10, 50.0    // Method coverage: 50% (triggers Rule 1, priority MEDIUM)
        );
        when(generation.getContent()).thenReturn(""); // No AI response to isolate rule-based

        // Act
        List<AiImprovementService.ImprovementOpportunity> opportunities =
                aiImprovementService.generateImprovementOpportunities(coverageData);

        // Assert
        assertEquals(4, opportunities.size());
        assertTrue(opportunities.stream().anyMatch(o -> "UNCOVERED_METHODS".equals(o.getType()) && o.getDescription().contains("5 uncovered methods")));
        assertTrue(opportunities.stream().anyMatch(o -> "EDGE_CASES".equals(o.getType())));
        assertTrue(opportunities.stream().anyMatch(o -> "OVERALL_IMPROVEMENT".equals(o.getType())));
        assertTrue(opportunities.stream().anyMatch(o -> "COMPLEX_FILE".equals(o.getType())));
    }

    /**
     * Tests the `generateRuleBasedOpportunities` logic when no rules are triggered.
     * This is achieved by providing high coverage data and ensuring no AI response.
     */
    @Test
    void generateImprovementOpportunities_ruleBased_noRulesTriggered() {
        // Arrange
        // High coverage, not complex file
        CoverageData coverageData = createTestCoverageData(
                "SimpleFile.java", "SimpleClass", "com.example",
                95, 100, 95.0, // Line coverage: 95%
                19, 20, 95.0,  // Branch coverage: 95%
                9, 10, 90.0    // Method coverage: 90%
        );
        when(generation.getContent()).thenReturn(""); // No AI response

        // Act
        List<AiImprovementService.ImprovementOpportunity> opportunities =
                aiImprovementService.generateImprovementOpportunities(coverageData);

        // Assert
        assertTrue(opportunities.isEmpty()); // No rule-based opportunities should be generated
    }

    /**
     * Tests the rule for uncovered methods to ensure HIGH priority is assigned correctly.
     */
    @Test
    void generateImprovementOpportunities_ruleBased_uncoveredMethodsPriorityHigh() {
        // Arrange
        CoverageData coverageData = createTestCoverageData(
                "TestFile.java", "TestClass", "com.example",
                90, 100, 90.0,
                90, 100, 90.0,
                1, 10, 10.0 // 9 uncovered methods (>= 5 -> HIGH priority)
        );
        when(generation.getContent()).thenReturn("");

        // Act
        List<AiImprovementService.ImprovementOpportunity> opportunities =
                aiImprovementService.generateImprovementOpportunities(coverageData);

        // Assert
        assertEquals(1, opportunities.size());
        AiImprovementService.ImprovementOpportunity op = opportunities.get(0);
        assertEquals("UNCOVERED_METHODS", op.getType());
        assertEquals("HIGH", op.getPriority());
        assertEquals("+15%", op.getEstimatedImpact()); // Min(15, 9*2=18)
    }

    /**
     * Tests the rule for uncovered methods to ensure MEDIUM priority is assigned correctly.
     */
    @Test
    void generateImprovementOpportunities_ruleBased_uncoveredMethodsPriorityMedium() {
        // Arrange
        CoverageData coverageData = createTestCoverageData(
                "TestFile.java", "TestClass", "com.example",
                90, 100, 90.0,
                90, 100, 90.0,
                7, 10, 70.0 // 3 uncovered methods (2 < 3 <= 5 -> MEDIUM priority)
        );
        when(generation.getContent()).thenReturn("");

        // Act
        List<AiImprovementService.ImprovementOpportunity> opportunities =
                aiImprovementService.generateImprovementOpportunities(coverageData);

        // Assert
        assertEquals(1, opportunities.size());
        AiImprovementService.ImprovementOpportunity op = opportunities.get(0);
        assertEquals("UNCOVERED_METHODS", op.getType());
        assertEquals("MEDIUM", op.getPriority());
        assertEquals("+6%", op.getEstimatedImpact()); // Min(15, 3*2=6)
    }

    /**
     * Tests the rule for uncovered methods to ensure LOW priority is assigned correctly.
     */
    @Test
    void generateImprovementOpportunities_ruleBased_uncoveredMethodsPriorityLow() {
        // Arrange
        CoverageData coverageData = createTestCoverageData(
                "TestFile.java", "TestClass", "com.example",
                90, 100, 90.0,
                90, 100, 90.0,
                9, 10, 90.0 // 1 uncovered method (<= 2 -> LOW priority)
        );
        when(generation.getContent()).thenReturn("");

        // Act
        List<AiImprovementService.ImprovementOpportunity> opportunities =
                aiImprovementService.generateImprovementOpportunities(coverageData);

        // Assert
        assertEquals(1, opportunities.size());
        AiImprovementService.ImprovementOpportunity op = opportunities.get(0);
        assertEquals("UNCOVERED_METHODS", op.getType());
        assertEquals("LOW", op.getPriority());
        assertEquals("+2%", op.getEstimatedImpact()); // Min(15, 1*2=2)
    }

    /**
     * Tests the branch coverage rule's condition where total branches is zero.
     * Verifies that the EDGE_CASES rule is not triggered in this scenario.
     */
    @Test
    void generateImprovementOpportunities_ruleBased_branchCoverageTotalBranchesZero() {
        // Arrange
        CoverageData coverageData = createTestCoverageData(
                "TestFile.java", "TestClass", "com.example",
                50, 100, 50.0,
                0, 0, 0.0, // Branch coverage 0%, but total branches 0
                5, 10, 50.0
        );
        when(generation.getContent()).thenReturn("");

        // Act
        List<AiImprovementService.ImprovementOpportunity> opportunities =
                aiImprovementService.generateImprovementOpportunities(coverageData);

        // Assert
        // Should not trigger EDGE_CASES rule because totalBranches is 0
        assertFalse(opportunities.stream().anyMatch(o -> "EDGE_CASES".equals(o.getType())));
        assertEquals(3, opportunities.size()); // Rule 1, 3, 4 should still trigger
    }

    /**
     * Helper method to create a CoverageData instance for testing.
     */
    private CoverageData createTestCoverageData(String fileName, String className, String packageName,
                                                int coveredLines, int totalLines, double lineCoverage,
                                                int coveredBranches, int totalBranches, double branchCoverage,
                                                int coveredMethods, int totalMethods, double methodCoverage) {
        return CoverageData.builder()
                .fileName(fileName)
                .className(className)
                .packageName(packageName)
                .path("src/main/java/" + packageName.replace('.', '/') + "/" + fileName)
                .coveredLines(coveredLines)
                .totalLines(totalLines)
                .lineCoverage(lineCoverage)
                .coveredBranches(coveredBranches)
                .totalBranches(totalBranches)
                .branchCoverage(branchCoverage)
                .coveredMethods(coveredMethods)
                .totalMethods(totalMethods)
                .methodCoverage(methodCoverage)
                .build();
    }
}