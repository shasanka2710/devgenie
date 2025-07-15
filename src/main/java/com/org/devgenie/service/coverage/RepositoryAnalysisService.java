package com.org.devgenie.service.coverage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.org.devgenie.exception.coverage.CoverageDataNotFoundException;
import com.org.devgenie.model.coverage.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RepositoryAnalysisService {

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private ProjectConfigDetectionService projectConfigService;

    @Autowired
    private CoverageDataService coverageDataService;

    @Autowired
    private ChatClient chatClient;

    public RepositoryAnalysisResponse analyzeRepository(RepositoryAnalysisRequest request) {
        log.info("Analyzing repository: {}", request.getRepositoryUrl());
        long overallStart = System.nanoTime();
        try {
            long stepStart, stepEnd;

            log.info("Starting repository analysis for URL: {}, Branch: {}, Workspace ID: {}", request.getRepositoryUrl(), request.getBranch(), request.getWorkspaceId());
            // Setup repository in workspace
            stepStart = System.nanoTime();
            String repoDir = repositoryService.setupRepository(
                    request.getRepositoryUrl(),
                    request.getBranch(),
                    request.getWorkspaceId() != null ? request.getWorkspaceId() : generateWorkspaceId(),
                    request.getGithubToken()
            );
            stepEnd = System.nanoTime();
            log.info("Repository setup completed in {} ms", (stepEnd - stepStart) / 1_000_000);


            // Detect project configuration
            log.info("Detecting project configuration for repository: {}", repoDir);
            stepStart = System.nanoTime();
            ProjectConfiguration projectConfig = projectConfigService.detectProjectConfiguration(repoDir);
            stepEnd = System.nanoTime();
            log.info("Project configuration detection completed in {} ms", (stepEnd - stepStart) / 1_000_000);

            // Find Java files
            log.info("Discovering Java files in repository: {}", repoDir);
            stepStart = System.nanoTime();
            List<String> javaFiles = repositoryService.findJavaFiles(repoDir, getDefaultExcludePatterns());
            stepEnd = System.nanoTime();
            log.info(javaFiles.size()+" Java file(s) discovery completed in {} ms", (stepEnd - stepStart) / 1_000_000);

            // Get existing coverage data if available
            log.info("Retrieving existing coverage data for repository: {}", repoDir);
            stepStart = System.nanoTime();
            CoverageData existingCoverage = null;
            try {
                existingCoverage = coverageDataService.getCurrentCoverage(repoDir);
            } catch (CoverageDataNotFoundException e) {
                log.info("No existing coverage data found, will generate fresh analysis");
            }
            stepEnd = System.nanoTime();
            log.info("Coverage data retrieval completed in {} ms", (stepEnd - stepStart) / 1_000_000);

            // AI-powered repository analysis
            log.info("Starting AI-powered repository analysis for: {}", repoDir);
            stepStart = System.nanoTime();
            RepositoryInsights insights = generateRepositoryInsights(repoDir, javaFiles, projectConfig);
            stepEnd = System.nanoTime();
            log.info("AI-powered repository analysis completed in {} ms", (stepEnd - stepStart) / 1_000_000);

            // Generate recommendations
            log.info("Generating coverage recommendations based on analysis");
            stepStart = System.nanoTime();
            List<CoverageRecommendation> recommendations = generateCoverageRecommendations(
                    javaFiles, existingCoverage, projectConfig, insights
            );
            stepEnd = System.nanoTime();
            log.info("Coverage recommendations generation completed in {} ms", (stepEnd - stepStart) / 1_000_000);

            long overallEnd = System.nanoTime();
            log.info("Total analysis completed in {} ms", (overallEnd - overallStart) / 1_000_000);

            return RepositoryAnalysisResponse.builder()
                    .repositoryUrl(request.getRepositoryUrl())
                    .branch(request.getBranch())
                    .workspaceId(extractWorkspaceId(repoDir))
                    .projectConfiguration(projectConfig)
                    .totalJavaFiles(javaFiles.size())
                    .javaFiles(javaFiles.stream().limit(20).collect(Collectors.toList())) // Limit for response size
                    .existingCoverage(existingCoverage)
                    .insights(insights)
                    .recommendations(recommendations)
                    .analysisTimestamp(LocalDateTime.now())
                    .success(true)
                    .build();

        } catch (Exception e) {
            log.error("Failed to analyze repository", e);
            return RepositoryAnalysisResponse.builder()
                    .success(false)
                    .error(e.getMessage())
                    .build();
        }
    }

    private RepositoryInsights generateRepositoryInsights(String repoDir, List<String> javaFiles, ProjectConfiguration config) {
        try {
            // Sample a few files for AI analysis
            List<String> sampleFiles = javaFiles.stream()
                    .limit(5)
                    .map(filePath -> {
                        try {
                            String content = repositoryService.readFileContent(repoDir, filePath);
                            return filePath + ":\n" + content.substring(0, Math.min(content.length(), 1000)) + "...";
                        } catch (Exception e) {
                            return filePath + ": [Error reading file]";
                        }
                    })
                    .collect(Collectors.toList());

            String analysisPrompt = String.format("""
                Analyze this Java repository for test coverage improvement opportunities.
                
                Project Configuration:
                - Build Tool: %s
                - Test Framework: %s
                - Java Version: %s
                - Total Java Files: %d
                
                Sample Files:
                %s
                
                Please provide analysis in JSON format:
                {
                    "repositoryComplexity": "LOW|MEDIUM|HIGH",
                    "dominantPatterns": ["MVC", "Service Layer", "Repository Pattern"],
                    "testingGaps": [
                        {
                            "category": "Service Layer",
                            "description": "Business logic in services lacks comprehensive testing",
                            "priority": "HIGH",
                            "estimatedEffort": "MEDIUM"
                        }
                    ],
                    "architecturalInsights": [
                        "Spring Boot application with layered architecture",
                        "Heavy use of dependency injection"
                    ],
                    "coverageStrategy": {
                        "recommendedApproach": "Bottom-up: Start with service layer, then controllers",
                        "priorityAreas": ["Business logic", "Error handling", "Edge cases"],
                        "estimatedTimeToImprove": "2-3 days"
                    },
                    "riskAssessment": {
                        "level": "LOW|MEDIUM|HIGH", 
                        "factors": ["Complex business logic", "External dependencies"]
                    }
                }
                """,
                    config.getBuildTool(),
                    config.getTestFramework(),
                    config.getJavaVersion(),
                    javaFiles.size(),
                    String.join("\n\n", sampleFiles)
            );
            log.info("Generated AI analysis prompt: {}", analysisPrompt);
            String aiResponse = chatClient.prompt(analysisPrompt).call().content();
            log.info("AI response received: {}", aiResponse);
            return parseRepositoryInsights(aiResponse);

        } catch (Exception e) {
            log.error("Failed to generate repository insights", e);
            return createDefaultInsights();
        }
    }

    private List<CoverageRecommendation> generateCoverageRecommendations(
            List<String> javaFiles, CoverageData existingCoverage, ProjectConfiguration config, RepositoryInsights insights) {

        List<CoverageRecommendation> recommendations = new ArrayList<>();

        // Basic recommendations based on project structure
        if (javaFiles.size() > 50) {
            recommendations.add(CoverageRecommendation.builder()
                    .priority("HIGH")
                    .category("STRATEGY")
                    .title("Large Codebase - Incremental Approach")
                    .description("With " + javaFiles.size() + " Java files, recommend processing in batches of 10-15 files")
                    .estimatedImpact("20-30% coverage improvement")
                    .estimatedEffort("3-5 days")
                    .build());
        }

        // Framework-specific recommendations
        if ("junit4".equals(config.getTestFramework())) {
            recommendations.add(CoverageRecommendation.builder()
                    .priority("MEDIUM")
                    .category("FRAMEWORK")
                    .title("Consider JUnit 5 Migration")
                    .description("Current project uses JUnit 4. Consider migrating to JUnit 5 for better test features")
                    .estimatedImpact("Improved test maintainability")
                    .estimatedEffort("1-2 days")
                    .build());
        }

        // Coverage-based recommendations
        if (existingCoverage != null) {
            if (existingCoverage.getOverallCoverage() < 50) {
                recommendations.add(CoverageRecommendation.builder()
                        .priority("HIGH")
                        .category("COVERAGE")
                        .title("Low Overall Coverage Detected")
                        .description(String.format("Current coverage is %.1f%%. Recommend targeting service layer first",
                                existingCoverage.getOverallCoverage()))
                        .estimatedImpact("40-50% coverage improvement possible")
                        .estimatedEffort("2-3 days")
                        .build());
            }
        } else {
            recommendations.add(CoverageRecommendation.builder()
                    .priority("HIGH")
                    .category("SETUP")
                    .title("No Coverage Data Available")
                    .description("No existing coverage data found. Will run fresh Jacoco analysis")
                    .estimatedImpact("Baseline establishment")
                    .estimatedEffort("30 minutes")
                    .build());
        }

        return recommendations;
    }

    private RepositoryInsights parseRepositoryInsights(String aiResponse) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonResponse = mapper.readTree(extractJsonFromResponse(aiResponse));

            return RepositoryInsights.builder()
                    .repositoryComplexity(jsonResponse.get("repositoryComplexity").asText())
                    .dominantPatterns(parseStringArray(jsonResponse.get("dominantPatterns")))
                    .testingGaps(parseTestingGaps(jsonResponse.get("testingGaps")))
                    .architecturalInsights(parseStringArray(jsonResponse.get("architecturalInsights")))
                    .coverageStrategy(parseCoverageStrategy(jsonResponse.get("coverageStrategy")))
                    .riskAssessment(parseRiskAssessment(jsonResponse.get("riskAssessment")))
                    .build();

        } catch (Exception e) {
            log.error("Failed to parse repository insights", e);
            return createDefaultInsights();
        }
    }

    private RepositoryInsights createDefaultInsights() {
        return RepositoryInsights.builder()
                .repositoryComplexity("MEDIUM")
                .dominantPatterns(List.of("Spring Boot", "Layered Architecture"))
                .testingGaps(List.of())
                .architecturalInsights(List.of("Standard Java application structure"))
                .coverageStrategy(CoverageStrategy.builder()
                        .recommendedApproach("Incremental improvement starting with service layer")
                        .priorityAreas(List.of("Business logic", "Error handling"))
                        .estimatedTimeToImprove("2-3 days")
                        .build())
                .riskAssessment(RiskAssessment.builder()
                        .level("MEDIUM")
                        .factors(List.of("Unknown project complexity"))
                        .build())
                .build();
    }

    private String generateWorkspaceId() {
        return "workspace-" + System.currentTimeMillis() + "-" +
                ThreadLocalRandom.current().nextInt(1000, 9999);
    }

    private String extractWorkspaceId(String repoDir) {
        // Extract workspace ID from repo directory path
        Path path = Paths.get(repoDir);
        return path.getParent().getFileName().toString();
    }

    private List<String> getDefaultExcludePatterns() {
        return Arrays.asList(
                "**/test/**",
                "**/tests/**",
                "**/*Test.java",
                "**/*Tests.java",
                "**/*IT.java",
                "**/target/**",
                "**/build/**",
                "**/generated/**"
        );
    }

    // Helper methods for parsing AI responses
    private List<String> parseStringArray(JsonNode arrayNode) {
        List<String> result = new ArrayList<>();
        if (arrayNode != null && arrayNode.isArray()) {
            for (JsonNode node : arrayNode) {
                result.add(node.asText());
            }
        }
        return result;
    }

    private List<TestingGap> parseTestingGaps(JsonNode gapsNode) {
        List<TestingGap> gaps = new ArrayList<>();
        if (gapsNode != null && gapsNode.isArray()) {
            for (JsonNode gapNode : gapsNode) {
                gaps.add(TestingGap.builder()
                        .category(gapNode.get("category").asText())
                        .description(gapNode.get("description").asText())
                        .priority(gapNode.get("priority").asText())
                        .estimatedEffort(gapNode.get("estimatedEffort").asText())
                        .build());
            }
        }
        return gaps;
    }

    private CoverageStrategy parseCoverageStrategy(JsonNode strategyNode) {
        if (strategyNode == null) return null;

        return CoverageStrategy.builder()
                .recommendedApproach(strategyNode.get("recommendedApproach").asText())
                .priorityAreas(parseStringArray(strategyNode.get("priorityAreas")))
                .estimatedTimeToImprove(strategyNode.get("estimatedTimeToImprove").asText())
                .build();
    }

    private RiskAssessment parseRiskAssessment(JsonNode riskNode) {
        if (riskNode == null) return null;

        return RiskAssessment.builder()
                .level(riskNode.get("level").asText())
                .factors(parseStringArray(riskNode.get("factors")))
                .build();
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