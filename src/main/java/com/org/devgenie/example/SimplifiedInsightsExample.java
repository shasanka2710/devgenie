package com.org.devgenie.example;

import com.org.devgenie.model.coverage.SimplifiedRepositoryInsights;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Example demonstrating the new simplified repository insights format
 */
public class SimplifiedInsightsExample {
    
    public static void main(String[] args) throws Exception {
        // Example of the new simplified format
        SimplifiedRepositoryInsights example = SimplifiedRepositoryInsights.builder()
                .repositorySummary(SimplifiedRepositoryInsights.RepositorySummary.builder()
                        .overallRiskLevel("HIGH")
                        .complexityScore(8)
                        .coverageGrade("C")
                        .primaryConcerns(java.util.List.of(
                                "High complexity in business logic",
                                "Low test coverage in critical components", 
                                "Several high-risk files identified"))
                        .build())
                .criticalFindings(SimplifiedRepositoryInsights.CriticalFindings.builder()
                        .highestRiskFiles(java.util.List.of(
                                SimplifiedRepositoryInsights.HighRiskFile.builder()
                                        .fileName("PaymentProcessor.java")
                                        .riskScore(95.0)
                                        .reason("Complex financial logic with no tests")
                                        .build(),
                                SimplifiedRepositoryInsights.HighRiskFile.builder()
                                        .fileName("UserAuthService.java")
                                        .riskScore(87.0)
                                        .reason("Security-critical component with high complexity")
                                        .build()))
                        .coverageGaps(java.util.List.of(
                                "Business logic layer has minimal coverage",
                                "Error handling scenarios not tested"))
                        .architecturalIssues(java.util.List.of(
                                "Tight coupling between service layers",
                                "Missing separation of concerns in controllers"))
                        .build())
                .recommendations(java.util.List.of(
                        SimplifiedRepositoryInsights.Recommendation.builder()
                                .priority("HIGH")
                                .title("Add Unit Tests for Payment Processing")
                                .description("Create comprehensive unit tests for PaymentProcessor.java focusing on edge cases and error scenarios")
                                .impact("Reduce financial risk by 60%")
                                .effort("2-3 days")
                                .build(),
                        SimplifiedRepositoryInsights.Recommendation.builder()
                                .priority("HIGH")
                                .title("Security Testing for Authentication")
                                .description("Implement security tests for UserAuthService including penetration testing scenarios")
                                .impact("Improve security posture significantly")
                                .effort("1-2 days")
                                .build(),
                        SimplifiedRepositoryInsights.Recommendation.builder()
                                .priority("MEDIUM")
                                .title("Refactor Service Layer Coupling")
                                .description("Introduce interfaces and dependency injection to reduce tight coupling")
                                .impact("Improve maintainability and testability")
                                .effort("1 week")
                                .build()))
                .build();
        
        // Serialize to pretty JSON
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        String json = mapper.writeValueAsString(example);
        
        System.out.println("=== SIMPLIFIED REPOSITORY INSIGHTS FORMAT ===");
        System.out.println(json);
        System.out.println("\n=== KEY BENEFITS ===");
        System.out.println("1. Concise and focused insights");
        System.out.println("2. Clear priority-based recommendations");
        System.out.println("3. Actionable with specific effort estimates");
        System.out.println("4. Easy to parse and process programmatically");
        System.out.println("5. Reduced complexity compared to previous format");
    }
}
