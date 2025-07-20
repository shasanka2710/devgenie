package com.org.devgenie.service.coverage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.org.devgenie.exception.coverage.CoverageDataNotFoundException;
import com.org.devgenie.model.SonarBaseComponentMetrics;
import com.org.devgenie.model.SonarQubeMetricsResponse;
import com.org.devgenie.model.coverage.*;
import com.org.devgenie.mongo.RepositoryAnalysisMongoUtil;
import com.org.devgenie.service.metadata.MetadataAnalyzer;
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
    private MetadataAnalyzer metadataAnalyzer;

    @Autowired
    private ChatClient chatClient;

    @Autowired
    private RepositoryAnalysisMongoUtil analysisMongoUtil;

    @Autowired
    private FileImprovementOpportunityService fileImprovementOpportunityService;

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

            // Metadata analysis for Java files
            log.info("Starting metadata analysis for {} Java files", javaFiles.size());
            stepStart = System.nanoTime();
            List<MetadataAnalyzer.FileMetadata> fileMetadata = metadataAnalyzer.analyzeJavaFiles(javaFiles,request.getRepositoryUrl(),request.getBranch());
            stepEnd = System.nanoTime();
            log.info("Metadata analysis completed for {} files in {} ms", fileMetadata.size(), (stepEnd - stepStart) / 1_000_000);
            
            // Log metadata analysis summary
            logMetadataAnalysisSummary(fileMetadata);

            // Get existing coverage data if available
            log.info("Retrieving existing coverage data for repository: {}", repoDir);
            stepStart = System.nanoTime();
            SonarQubeMetricsResponse sonarQubeMetricsResponse = null;
            SonarBaseComponentMetrics sonarBaseComponentMetrics = null;
            List<CoverageData> existingCoverage = new ArrayList<>();
            try {
                sonarQubeMetricsResponse = coverageDataService.getCurrentCoverage(request.getRepositoryUrl(), request.getBranch());
                existingCoverage = sonarQubeMetricsResponse.getCoverageDataList();
                sonarBaseComponentMetrics = sonarQubeMetricsResponse.getSonarBaseComponentMetrics();
            } catch (CoverageDataNotFoundException e) {
                log.info("No existing coverage data found, will generate fresh analysis");
            }
            stepEnd = System.nanoTime();
            log.info("Coverage data retrieval completed in {} ms", (stepEnd - stepStart) / 1_000_000);

            // AI-powered repository analysis
            log.info("Starting AI-powered repository analysis for: {}", repoDir);
            stepStart = System.nanoTime();
            RepositoryInsights insights = generateRepositoryInsights(repoDir, javaFiles, projectConfig, fileMetadata, existingCoverage);
            stepEnd = System.nanoTime();
            log.info("AI-powered repository analysis completed in {} ms", (stepEnd - stepStart) / 1_000_000);

            // Generate recommendations
            log.info("Generating coverage recommendations based on analysis");
            stepStart = System.nanoTime();
            List<CoverageRecommendation> recommendations = null;
           /* List<CoverageRecommendation> recommendations = generateCoverageRecommendations(
                    javaFiles, existingCoverage, projectConfig, insights, fileMetadata);*/
            stepEnd = System.nanoTime();
            log.info("Coverage recommendations generation completed in {} ms", (stepEnd - stepStart) / 1_000_000);

            long overallEnd = System.nanoTime();
            log.info("Total analysis completed in {} ms", (overallEnd - overallStart) / 1_000_000);

            RepositoryAnalysis repositoryAnalysis = RepositoryAnalysis.builder()
                    .repositoryUrl(request.getRepositoryUrl())
                    .branch(request.getBranch())
                    .workspaceId(extractWorkspaceId(repoDir))
                    .projectConfiguration(projectConfig)
                    .totalJavaFiles(javaFiles.size())
                    .insights(insights)
                    .recommendations(recommendations)
                    .analysisTimestamp(LocalDateTime.now())
                    .success(true)
                    .build();

            RepositoryAnalysisResponse response = RepositoryAnalysisResponse.builder()
                    .repositoryAnalysis(repositoryAnalysis)
                    .fileMetadata(fileMetadata)
                    .existingCoverage(existingCoverage)
                    .sonarBaseComponentMetrics(sonarBaseComponentMetrics)
                    .success(true)
                    .build();


            // Persist repository summary
            analysisMongoUtil.persistRepositoryAnalysisAsync(repositoryAnalysis);
            // Persist coverage nodes separately
            analysisMongoUtil.persistCoverageDataBatchAsync(existingCoverage, repoDir, request.getBranch());
            //Persist SonarBaseComponentMetrics
            analysisMongoUtil.persistSonarBaseComponentMetricsAsync(repoDir,request.getBranch(), sonarBaseComponentMetrics);
            // Persist file metadata separately
            if (fileMetadata != null) {
                analysisMongoUtil.persistFileMetadataBatchAsync(fileMetadata, repoDir, request.getBranch());
            }

            return response;

        } catch (Exception e) {
            log.error("Failed to analyze repository", e);
            return RepositoryAnalysisResponse.builder()
                    .success(false)
                    .error(e.getMessage())
                    .build();
        }
    }

    private RepositoryInsights generateRepositoryInsights(String repoDir, List<String> javaFiles, 
            ProjectConfiguration config, List<MetadataAnalyzer.FileMetadata> fileMetadata, 
            List<CoverageData> existingCoverage) {
        try {
            // Generate simplified analysis - more focused and reliable
            SimplifiedRepositoryInsights simplifiedInsights = generateSimplifiedRepositoryInsights(
                repoDir, javaFiles, config, fileMetadata, existingCoverage);
            
            // Convert simplified insights to legacy format for compatibility
            return convertToLegacyInsights(simplifiedInsights);

        } catch (Exception e) {
            log.error("Failed to generate repository insights", e);
            return createDefaultInsights();
        }
    }

    /**
     * Generate simplified repository insights using focused prompt
     */
    private SimplifiedRepositoryInsights generateSimplifiedRepositoryInsights(String repoDir, List<String> javaFiles,
            ProjectConfiguration config, List<MetadataAnalyzer.FileMetadata> fileMetadata,
            List<CoverageData> existingCoverage) {
        try {
            String focusedData = generateFocusedAnalysisData(repoDir, javaFiles, config, fileMetadata, existingCoverage);
            
            String simplifiedPrompt = String.format("""
                You are analyzing a Java repository. Provide precise, actionable insights in JSON format.
                
                REPOSITORY DATA:
                %s
                
                RESPOND WITH EXACT JSON FORMAT:
                {
                    "repositorySummary": {
                        "overallRiskLevel": "LOW|MEDIUM|HIGH|CRITICAL",
                        "complexityScore": 1-10,
                        "coverageGrade": "A|B|C|D|F",
                        "primaryConcerns": ["concern1", "concern2", "concern3"]
                    },
                    "criticalFindings": {
                        "highestRiskFiles": [
                            {
                                "fileName": "ClassName.java",
                                "riskScore": 85.0,
                                "reason": "Specific reason for high risk"
                            }
                        ],
                        "coverageGaps": ["Specific area lacking coverage"],
                        "architecturalIssues": ["Specific architectural concern"]
                    },
                    "recommendations": [
                        {
                            "priority": "HIGH|MEDIUM|LOW",
                            "title": "Concise title",
                            "description": "Specific actionable description",
                            "impact": "Expected improvement",
                            "effort": "X hours/days"
                        }
                    ]
                }
                
                Rules:
                1. Base insights on actual data provided
                2. Limit to top 3 recommendations 
                3. Be specific about file names and metrics
                4. Keep descriptions concise but actionable
                """, focusedData);
            
            log.info("Generating simplified AI analysis with prompt: {}", simplifiedPrompt);
            String aiResponse = chatClient.prompt(simplifiedPrompt).call().content();
            log.info("Simplified AI response: {}", aiResponse);
            
            return parseSimplifiedRepositoryInsights(aiResponse);
            
        } catch (Exception e) {
            log.error("Failed to generate simplified repository insights", e);
            return createDefaultSimplifiedInsights();
        }
    }
    
    /**
     * Generate comprehensive analysis data for AI processing
     */
   /* private String generateComprehensiveAnalysisData(String repoDir, List<String> javaFiles,
            ProjectConfiguration config, List<MetadataAnalyzer.FileMetadata> fileMetadata) {
        
        StringBuilder analysisData = new StringBuilder();
        
        // Project Configuration Details
        analysisData.append("PROJECT CONFIGURATION:\n");
        analysisData.append(String.format("- Build Tool: %s\n", config.getBuildTool()));
        analysisData.append(String.format("- Test Framework: %s\n", config.getTestFramework()));
        analysisData.append(String.format("- Java Version: %s\n", config.getJavaVersion()));
        analysisData.append(String.format("- Total Java Files: %d\n", javaFiles.size()));
        analysisData.append(String.format("- Project Type: %s\n", determineProjectType(config)));
        
        // Detailed Metadata Analysis
        analysisData.append("\nCODE QUALITY METRICS:\n");
        if (fileMetadata != null && !fileMetadata.isEmpty()) {
            double avgRiskScore = fileMetadata.stream()
                    .mapToDouble(MetadataAnalyzer.FileMetadata::getRiskScore)
                    .average().orElse(0.0);
            
            double avgCyclomaticComplexity = fileMetadata.stream()
                    .filter(fm -> fm.getCodeComplexity() != null)
                    .mapToDouble(fm -> fm.getCodeComplexity().getCyclomaticComplexity())
                    .average().orElse(0.0);
            
            double avgCognitiveComplexity = fileMetadata.stream()
                    .filter(fm -> fm.getCodeComplexity() != null)
                    .mapToDouble(fm -> fm.getCodeComplexity().getCognitiveComplexity())
                    .average().orElse(0.0);
            
            analysisData.append(String.format("- Average Risk Score: %.1f/100\n", avgRiskScore));
            analysisData.append(String.format("- Average Cyclomatic Complexity: %.1f\n", avgCyclomaticComplexity));
            analysisData.append(String.format("- Average Cognitive Complexity: %.1f\n", avgCognitiveComplexity));
            
            // High-risk files analysis
            List<MetadataAnalyzer.FileMetadata> highRiskFiles = fileMetadata.stream()
                    .filter(fm -> fm.getRiskScore() > 70.0)
                    .sorted((f1, f2) -> Double.compare(f2.getRiskScore(), f1.getRiskScore()))
                    .limit(10)
                    .collect(Collectors.toList());
            
            if (!highRiskFiles.isEmpty()) {
                analysisData.append(String.format("\nHIGH-RISK FILES (%d files with risk score >70):\n", highRiskFiles.size()));
                highRiskFiles.forEach(fm -> 
                    analysisData.append(String.format("- %s: Risk=%.1f, Cyclomatic=%d, Business Criticality=%.1f\n",
                        fm.getClassName(),
                        fm.getRiskScore(),
                        fm.getCodeComplexity() != null ? fm.getCodeComplexity().getCyclomaticComplexity() : 0,
                        fm.getBusinessComplexity() != null ? fm.getBusinessComplexity().getBusinessCriticality() : 0.0
                    )));
            }
            
            // Business critical files
            List<MetadataAnalyzer.FileMetadata> businessCriticalFiles = fileMetadata.stream()
                    .filter(fm -> fm.getBusinessComplexity() != null && 
                            fm.getBusinessComplexity().getBusinessCriticality() > 7.0)
                    .sorted((f1, f2) -> Double.compare(
                            f2.getBusinessComplexity().getBusinessCriticality(),
                            f1.getBusinessComplexity().getBusinessCriticality()))
                    .limit(5)
                    .collect(Collectors.toList());
            
            if (!businessCriticalFiles.isEmpty()) {
                analysisData.append(String.format("\nBUSINESS-CRITICAL FILES (%d files):\n", businessCriticalFiles.size()));
                businessCriticalFiles.forEach(fm -> 
                    analysisData.append(String.format("- %s: Business Criticality=%.1f, Risk=%.1f\n",
                        fm.getClassName(),
                        fm.getBusinessComplexity().getBusinessCriticality(),
                        fm.getRiskScore()
                    )));
            }
        }
        
        // File structure analysis
        analysisData.append("\nFILE STRUCTURE ANALYSIS:\n");
        Map<String, Long> packageDistribution = javaFiles.stream()
                .collect(Collectors.groupingBy(this::extractPackageFromPath, Collectors.counting()));
        
        analysisData.append("Package Distribution:\n");
        packageDistribution.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .forEach(entry -> analysisData.append(String.format("- %s: %d files\n", entry.getKey(), entry.getValue())));
        
        // Sample high-complexity files for detailed analysis
        analysisData.append("\nSAMPLE HIGH-COMPLEXITY FILES:\n");
        if (fileMetadata != null) {
            fileMetadata.stream()
                    .filter(fm -> fm.getCodeComplexity() != null && 
                            fm.getCodeComplexity().getCyclomaticComplexity() > 10)
                    .sorted((f1, f2) -> Integer.compare(
                            f2.getCodeComplexity().getCyclomaticComplexity(),
                            f1.getCodeComplexity().getCyclomaticComplexity()))
                    .limit(3)
                    .forEach(fm -> {
                        try {
                            String content = repositoryService.readFileContent(repoDir, fm.getFilePath());
                            analysisData.append(String.format("\nFile: %s (Complexity: %d)\n",
                                    fm.getClassName(),
                                    fm.getCodeComplexity().getCyclomaticComplexity()));
                            analysisData.append("Sample Content:\n");
                            analysisData.append(content.substring(0, Math.min(content.length(), 800))).append("...\n");
                        } catch (Exception e) {
                            analysisData.append(String.format("- %s: [Error reading file]\n", fm.getClassName()));
                        }
                    });
        }
        
        return analysisData.toString();
    }*/
    
    private String determineProjectType(ProjectConfiguration config) {
        if (config.getDependencies().stream().anyMatch(dep -> dep.contains("spring-boot"))) {
            return "Spring Boot Application";
        } else if (config.getDependencies().stream().anyMatch(dep -> dep.contains("spring"))) {
            return "Spring Application";
        } else if (config.getDependencies().stream().anyMatch(dep -> dep.contains("servlet"))) {
            return "Java Web Application";
        }
        return "Java Application";
    }
    
    private String extractPackageFromPath(String filePath) {
        if (filePath.contains("src/main/java/")) {
            String packagePath = filePath.substring(filePath.indexOf("src/main/java/") + "src/main/java/".length());
            int lastSlash = packagePath.lastIndexOf('/');
            if (lastSlash > 0) {
                return packagePath.substring(0, lastSlash).replace('/', '.');
            }
        }
        return "default";
    }

    private List<CoverageRecommendation> generateCoverageRecommendations(
            List<String> javaFiles, CoverageData existingCoverage, ProjectConfiguration config, 
            RepositoryInsights insights, List<MetadataAnalyzer.FileMetadata> fileMetadata) {

        try {
            // Generate AI-driven coverage recommendations
            List<CoverageRecommendation> aiRecommendations = generateAIDrivenCoverageRecommendations(
                javaFiles, existingCoverage, config, insights, fileMetadata);
            
            if (!aiRecommendations.isEmpty()) {
                log.info("Successfully generated {} AI-driven coverage recommendations", aiRecommendations.size());
                return aiRecommendations;
            }
            
            log.warn("AI recommendations failed or empty, falling back to manual logic");
        } catch (Exception e) {
            log.error("Failed to generate AI-driven recommendations, falling back to manual logic", e);
        }

        // Fallback to manual logic
        return generateFallbackCoverageRecommendations(javaFiles, existingCoverage, config, fileMetadata);
    }

    /**
     * Generate AI-driven coverage recommendations using comprehensive context
     */
    private List<CoverageRecommendation> generateAIDrivenCoverageRecommendations(
            List<String> javaFiles, CoverageData existingCoverage, ProjectConfiguration config, 
            RepositoryInsights insights, List<MetadataAnalyzer.FileMetadata> fileMetadata) {
        String coverageRecommendationPrompt = "";

        /*// Build comprehensive context for AI analysis
        String coverageRecommendationPrompt = buildCoverageRecommendationPrompt(
            javaFiles, existingCoverage, config, insights, fileMetadata);*/

        log.info("Generating AI-driven coverage recommendations with prompt .......: {}", coverageRecommendationPrompt);
        String aiResponse = chatClient.prompt(coverageRecommendationPrompt).call().content();
        log.info("AI coverage recommendations received: {}", aiResponse);

        return parseAICoverageRecommendations(aiResponse);
    }

    /**
     * Build comprehensive prompt for AI coverage recommendation generation
     */
    /*private String buildCoverageRecommendationPrompt(List<String> javaFiles, CoverageData existingCoverage,
            ProjectConfiguration config, RepositoryInsights insights, List<MetadataAnalyzer.FileMetadata> fileMetadata) {
        
        StringBuilder promptBuilder = new StringBuilder();
        
        promptBuilder.append("""
            You are a senior test automation architect analyzing a Java repository to provide specific, actionable test coverage recommendations.
            
            REPOSITORY CONTEXT:
            """);
        
        // Project Configuration
        promptBuilder.append(String.format("""
            
            PROJECT CONFIGURATION:
            - Build Tool: %s
            - Test Framework: %s
            - Java Version: %s
            - Total Java Files: %d
            - Project Type: %s
            """, 
            config.getBuildTool(),
            config.getTestFramework(),
            config.getJavaVersion(),
            javaFiles.size(),
            determineProjectType(config)));
        
        // Repository Insights Summary
        if (insights != null) {
            promptBuilder.append(String.format("""
                
                REPOSITORY INSIGHTS:
                - Executive Summary: %s
                - Repository Complexity: %s
                - Risk Assessment Level: %s
                - Code Quality Grade: %s
                - Technical Debt Level: %s
                """,
                insights.getExecutiveSummary() != null ? insights.getExecutiveSummary() : "Not available",
                insights.getRepositoryComplexity() != null ? insights.getRepositoryComplexity() : "Unknown",
                insights.getRiskAssessment() != null ? insights.getRiskAssessment().getLevel() : "Unknown",
                insights.getCodeQualityAssessment() != null ? insights.getCodeQualityAssessment().getOverallGrade() : "Unknown",
                insights.getTechnicalDebtAssessment() != null ? insights.getTechnicalDebtAssessment().getDebtLevel() : "Unknown"));
        }

        // Existing Coverage Analysis
        if (existingCoverage != null) {
            promptBuilder.append(String.format("""
                
                EXISTING COVERAGE DATA:
                - Overall Coverage: %.1f%%
                - Line Coverage: %.1f%%
                - Branch Coverage: %.1f%%
                """,
                existingCoverage.getOverallCoverage(),
                existingCoverage.getLineCoverage(),
                existingCoverage.getBranchCoverage()));

            // Add directory-level coverage if available
            if (existingCoverage.getRootDirectory() != null &&
                existingCoverage.getRootDirectory().getSubdirectories() != null &&
                !existingCoverage.getRootDirectory().getSubdirectories().isEmpty()) {
                promptBuilder.append("\nDIRECTORY COVERAGE:\n");
                existingCoverage.getRootDirectory().getSubdirectories().stream()
                    .sorted((d1, d2) -> Double.compare(d1.getOverallCoverage(), d2.getOverallCoverage()))
                    .limit(10)
                    .forEach(dir -> promptBuilder.append(String.format("- %s: %.1f%% (Lines: %d/%d)\n",
                        dir.getDirectoryPath(),
                        dir.getOverallCoverage(),
                        dir.getCoveredLines(),
                        dir.getTotalLines())));
            }

            // Add file-level coverage for low coverage files
            if (existingCoverage.getRootDirectory() != null) {
                List<FileCoverageData> lowCoverageFiles = getAllFilesFromDirectory(existingCoverage.getRootDirectory())
                    .stream()
                    .filter(file -> file.getLineCoverage() < 50.0)
                    .sorted((f1, f2) -> Double.compare(f1.getLineCoverage(), f2.getLineCoverage()))
                    .limit(15)
                    .collect(Collectors.toList());

                if (!lowCoverageFiles.isEmpty()) {
                    promptBuilder.append("\nLOW COVERAGE FILES (<50%):\n");
                    lowCoverageFiles.forEach(file -> promptBuilder.append(String.format("- %s: %.1f%% (Lines: %d/%d)\n",
                        file.getFileName(),
                        file.getLineCoverage(),
                        file.getCoveredLines(),
                        file.getTotalLines())));
                }
            }
        } else {
            promptBuilder.append("\nEXISTING COVERAGE DATA: No coverage data available - fresh analysis needed\n");
        }

        // High-Risk and Business Critical Files
        if (fileMetadata != null && !fileMetadata.isEmpty()) {
            // High-risk files
            List<MetadataAnalyzer.FileMetadata> highRiskFiles = fileMetadata.stream()
                .filter(fm -> fm.getRiskScore() > 70.0)
                .sorted((f1, f2) -> Double.compare(f2.getRiskScore(), f1.getRiskScore()))
                .limit(10)
                .collect(Collectors.toList());
            
            if (!highRiskFiles.isEmpty()) {
                promptBuilder.append("\nHIGH-RISK FILES (Risk Score >70):\n");
                highRiskFiles.forEach(fm -> promptBuilder.append(String.format("- %s: Risk=%.1f, Cyclomatic=%d, Cognitive=%d\n",
                    fm.getClassName(),
                    fm.getRiskScore(),
                    fm.getCodeComplexity() != null ? fm.getCodeComplexity().getCyclomaticComplexity() : 0,
                    fm.getCodeComplexity() != null ? fm.getCodeComplexity().getCognitiveComplexity() : 0)));
            }
            
            // Business critical files
            List<MetadataAnalyzer.FileMetadata> businessCriticalFiles = fileMetadata.stream()
                .filter(fm -> fm.getBusinessComplexity() != null && 
                        fm.getBusinessComplexity().getBusinessCriticality() > 7.0)
                .sorted((f1, f2) -> Double.compare(
                        f2.getBusinessComplexity().getBusinessCriticality(),
                        f1.getBusinessComplexity().getBusinessCriticality()))
                .limit(10)
                .collect(Collectors.toList());
            
            if (!businessCriticalFiles.isEmpty()) {
                promptBuilder.append("\nBUSINESS-CRITICAL FILES:\n");
                businessCriticalFiles.forEach(fm -> promptBuilder.append(String.format("- %s: Business Criticality=%.1f, Risk=%.1f\n",
                    fm.getClassName(),
                    fm.getBusinessComplexity().getBusinessCriticality(),
                    fm.getRiskScore())));
            }
        }

        promptBuilder.append("""
            
            ANALYSIS REQUIREMENT:
            Based on the comprehensive data above, provide specific, actionable test coverage recommendations in JSON format.
            Focus on the most impactful recommendations that address the actual risks and gaps identified in this specific codebase.
            
            REQUIRED JSON FORMAT:
            {
                "recommendations": [
                    {
                        "priority": "CRITICAL|HIGH|MEDIUM|LOW",
                        "category": "SETUP|STRATEGY|HIGH_RISK|BUSINESS_LOGIC|COMPLEXITY|FRAMEWORK|COVERAGE|TECHNICAL_DEBT",
                        "title": "Specific, actionable title",
                        "description": "Detailed explanation based on actual findings",
                        "rationale": "Why this recommendation is important for this specific codebase",
                        "actionSteps": [
                            "Specific step 1 with file names if applicable",
                            "Specific step 2 with concrete actions",
                            "Specific step 3 with measurable outcomes"
                        ],
                        "expectedOutcomes": [
                            "Specific measurable outcome 1",
                            "Specific measurable outcome 2"
                        ],
                        "timeframe": "Realistic timeframe for completion",
                        "prerequisites": ["What needs to be done first"],
                        "successMetrics": "How to measure success",
                        "riskIfIgnored": "Specific consequences of not following this recommendation",
                        "relatedFiles": ["Specific file names this affects"],
                        "estimatedImpact": "Quantified impact (e.g., coverage improvement, risk reduction)",
                        "estimatedEffort": "Realistic effort estimate"
                    }
                ]
            }
            
            IMPORTANT GUIDELINES:
            1. Prioritize recommendations based on actual risk scores and business criticality found in the data
            2. Include specific file names where applicable
            3. Provide realistic timeframes and effort estimates
            4. Base all recommendations on the actual data provided, not generic advice
            5. Limit to the top 5-8 most impactful recommendations
            6. Make action steps specific and implementable
            """);

        return promptBuilder.toString();
    }*/

    /**
     * Parse AI coverage recommendations from JSON response
     */
    private List<CoverageRecommendation> parseAICoverageRecommendations(String aiResponse) {
        log.info("Parsing AI coverage recommendations from response: {}", aiResponse);
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonResponse = mapper.readTree(extractJsonFromResponse(aiResponse));
            
            List<CoverageRecommendation> recommendations = new ArrayList<>();
            JsonNode recommendationsArray = jsonResponse.get("recommendations");
            
            if (recommendationsArray != null && recommendationsArray.isArray()) {
                for (JsonNode recNode : recommendationsArray) {
                    CoverageRecommendation recommendation = CoverageRecommendation.builder()
                        .priority(getJsonText(recNode, "priority"))
                        .category(getJsonText(recNode, "category"))
                        .title(getJsonText(recNode, "title"))
                        .description(getJsonText(recNode, "description"))
                        .rationale(getJsonText(recNode, "rationale"))
                        .actionSteps(parseStringArray(recNode.get("actionSteps")))
                        .expectedOutcomes(parseStringArray(recNode.get("expectedOutcomes")))
                        .timeframe(getJsonText(recNode, "timeframe"))
                        .prerequisites(parseStringArray(recNode.get("prerequisites")))
                        .successMetrics(getJsonText(recNode, "successMetrics"))
                        .riskIfIgnored(getJsonText(recNode, "riskIfIgnored"))
                        .relatedFiles(parseStringArray(recNode.get("relatedFiles")))
                        .estimatedImpact(getJsonText(recNode, "estimatedImpact"))
                        .estimatedEffort(getJsonText(recNode, "estimatedEffort"))
                        .build();
                    
                    recommendations.add(recommendation);
                }
            }
            
            return recommendations;
            
        } catch (Exception e) {
            log.error("Failed to parse AI coverage recommendations", e);
            return new ArrayList<>();
        }
    }

    /**
     * Fallback manual coverage recommendations when AI fails
     */
    private List<CoverageRecommendation> generateFallbackCoverageRecommendations(
            List<String> javaFiles, CoverageData existingCoverage, ProjectConfiguration config,
            List<MetadataAnalyzer.FileMetadata> fileMetadata) {

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

        // Metadata-based recommendations
        if (fileMetadata != null && !fileMetadata.isEmpty()) {
            // Find high-risk files based on metadata analysis
            List<MetadataAnalyzer.FileMetadata> highRiskFiles = fileMetadata.stream()
                    .filter(fm -> fm.getRiskScore() > 70.0)
                    .sorted((f1, f2) -> Double.compare(f2.getRiskScore(), f1.getRiskScore()))
                    .limit(5)
                    .collect(Collectors.toList());

            if (!highRiskFiles.isEmpty()) {
                recommendations.add(CoverageRecommendation.builder()
                        .priority("HIGH")
                        .category("HIGH_RISK")
                        .title("High-Risk Files Detected")
                        .description(String.format("Found %d high-risk files with complexity scores above 70. " +
                                "Priority files: %s", 
                                highRiskFiles.size(),
                                highRiskFiles.stream()
                                        .map(MetadataAnalyzer.FileMetadata::getClassName)
                                        .collect(Collectors.joining(", "))))
                        .estimatedImpact("25-35% risk reduction")
                        .estimatedEffort("2-4 days")
                        .build());
            }

            // Business logic recommendations
            long businessCriticalFiles = fileMetadata.stream()
                    .filter(fm -> fm.getBusinessComplexity() != null && 
                            fm.getBusinessComplexity().getBusinessCriticality() > 7.0)
                    .count();

            if (businessCriticalFiles > 0) {
                recommendations.add(CoverageRecommendation.builder()
                        .priority("HIGH")
                        .category("BUSINESS_LOGIC")
                        .title("Business-Critical Files Identified")
                        .description(String.format("%d files contain critical business logic. " +
                                "These should be prioritized for comprehensive test coverage", businessCriticalFiles))
                        .estimatedImpact("40-50% business risk reduction")
                        .estimatedEffort("3-5 days")
                        .build());
            }
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

    /**
     * Parse enhanced repository insights from AI response
     */
    private RepositoryInsights parseEnhancedRepositoryInsights(String aiResponse) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonResponse = mapper.readTree(extractJsonFromResponse(aiResponse));

            return RepositoryInsights.builder()
                    .executiveSummary(getJsonText(jsonResponse, "executiveSummary"))
                    .repositoryComplexity(getJsonText(jsonResponse, "repositoryComplexity"))
                    .dominantPatterns(parseStringArray(jsonResponse.get("dominantPatterns")))
                    .keyFindings(parseStringArray(jsonResponse.get("keyFindings")))
                    .criticalActions(parseStringArray(jsonResponse.get("criticalActions")))
                    .testingGaps(parseTestingGaps(jsonResponse.get("testingGaps")))
                    .architecturalInsights(parseStringArray(jsonResponse.get("architecturalInsights")))
                    .codeQualityAssessment(parseCodeQualityAssessment(jsonResponse.get("codeQualityAssessment")))
                    .businessImpactAnalysis(parseBusinessImpactAnalysis(jsonResponse.get("businessImpactAnalysis")))
                    .technicalDebtAssessment(parseTechnicalDebtAssessment(jsonResponse.get("technicalDebtAssessment")))
                    .coverageStrategy(parseCoverageStrategy(jsonResponse.get("coverageStrategy")))
                    .riskAssessment(parseRiskAssessment(jsonResponse.get("riskAssessment")))
                    .build();

        } catch (Exception e) {
            log.error("Failed to parse enhanced repository insights", e);
            return createDefaultInsights();
        }
    }
    
    /**
     * Parse simplified repository insights from AI response
     */
    private SimplifiedRepositoryInsights parseSimplifiedRepositoryInsights(String aiResponse) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonResponse = mapper.readTree(extractJsonFromResponse(aiResponse));

            // Parse repository summary
            JsonNode summaryNode = jsonResponse.get("repositorySummary");
            SimplifiedRepositoryInsights.RepositorySummary summary = SimplifiedRepositoryInsights.RepositorySummary.builder()
                    .overallRiskLevel(getJsonText(summaryNode, "overallRiskLevel"))
                    .complexityScore(summaryNode.get("complexityScore") != null ? summaryNode.get("complexityScore").asInt() : 5)
                    .coverageGrade(getJsonText(summaryNode, "coverageGrade"))
                    .primaryConcerns(parseStringArray(summaryNode.get("primaryConcerns")))
                    .build();

            // Parse critical findings
            JsonNode findingsNode = jsonResponse.get("criticalFindings");
            List<SimplifiedRepositoryInsights.HighRiskFile> highRiskFiles = new ArrayList<>();
            JsonNode riskFilesArray = findingsNode.get("highestRiskFiles");
            if (riskFilesArray != null && riskFilesArray.isArray()) {
                for (JsonNode fileNode : riskFilesArray) {
                    highRiskFiles.add(SimplifiedRepositoryInsights.HighRiskFile.builder()
                            .fileName(getJsonText(fileNode, "fileName"))
                            .riskScore(fileNode.get("riskScore") != null ? fileNode.get("riskScore").asDouble() : 0.0)
                            .reason(getJsonText(fileNode, "reason"))
                            .build());
                }
            }

            SimplifiedRepositoryInsights.CriticalFindings findings = SimplifiedRepositoryInsights.CriticalFindings.builder()
                    .highestRiskFiles(highRiskFiles)
                    .coverageGaps(parseStringArray(findingsNode.get("coverageGaps")))
                    .architecturalIssues(parseStringArray(findingsNode.get("architecturalIssues")))
                    .build();

            // Parse recommendations
            List<SimplifiedRepositoryInsights.Recommendation> recommendations = new ArrayList<>();
            JsonNode recArray = jsonResponse.get("recommendations");
            if (recArray != null && recArray.isArray()) {
                for (JsonNode recNode : recArray) {
                    recommendations.add(SimplifiedRepositoryInsights.Recommendation.builder()
                            .priority(getJsonText(recNode, "priority"))
                            .title(getJsonText(recNode, "title"))
                            .description(getJsonText(recNode, "description"))
                            .impact(getJsonText(recNode, "impact"))
                            .effort(getJsonText(recNode, "effort"))
                            .build());
                }
            }

            return SimplifiedRepositoryInsights.builder()
                    .repositorySummary(summary)
                    .criticalFindings(findings)
                    .recommendations(recommendations)
                    .build();

        } catch (Exception e) {
            log.error("Failed to parse simplified repository insights", e);
            return createDefaultSimplifiedInsights();
        }
    }
    
    /**
     * Create default insights when AI analysis fails
     */
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

    /**
     * Create default simplified insights when AI analysis fails
     */
    private SimplifiedRepositoryInsights createDefaultSimplifiedInsights() {
        return SimplifiedRepositoryInsights.builder()
                .repositorySummary(SimplifiedRepositoryInsights.RepositorySummary.builder()
                        .overallRiskLevel("MEDIUM")
                        .complexityScore(5)
                        .coverageGrade("C")
                        .primaryConcerns(List.of("Unknown complexity", "No coverage data", "Analysis needed"))
                        .build())
                .criticalFindings(SimplifiedRepositoryInsights.CriticalFindings.builder()
                        .highestRiskFiles(List.of())
                        .coverageGaps(List.of("No coverage analysis available"))
                        .architecturalIssues(List.of("Analysis required"))
                        .build())
                .recommendations(List.of(
                        SimplifiedRepositoryInsights.Recommendation.builder()
                                .priority("HIGH")
                                .title("Run Initial Analysis")
                                .description("Complete repository analysis to identify specific improvement areas")
                                .impact("Establish baseline for improvements")
                                .effort("1-2 hours")
                                .build()))
                .build();
    }

    /**
     * Generate focused analysis data for simplified AI processing
     */
    private String generateFocusedAnalysisData(String repoDir, List<String> javaFiles,
            ProjectConfiguration config, List<MetadataAnalyzer.FileMetadata> fileMetadata,
            List<CoverageData> existingCoverage) {
        
        StringBuilder data = new StringBuilder();
        
        // PROJECT BASICS
        data.append("PROJECT INFO:\n");
        data.append(String.format("Build Tool: %s | Test Framework: %s | Java: %s | Files: %d\n", 
            config.getBuildTool(), config.getTestFramework(), config.getJavaVersion(), javaFiles.size()));
        
        // COVERAGE SUMMARY
        data.append("\nCOVERAGE:\n");
        if (existingCoverage != null && !existingCoverage.isEmpty()) {
            CoverageData coverage = existingCoverage.get(0);
            data.append(String.format("Overall: %.1f%% | Line: %.1f%% | Branch: %.1f%%\n",
                coverage.getOverallCoverage(), coverage.getLineCoverage(), coverage.getBranchCoverage()));
        } else {
            data.append("No existing coverage data available\n");
        }
        
        // RISK FILES (Top 5)
        if (fileMetadata != null && !fileMetadata.isEmpty()) {
            List<MetadataAnalyzer.FileMetadata> topRiskFiles = fileMetadata.stream()
                .sorted((f1, f2) -> Double.compare(f2.getRiskScore(), f1.getRiskScore()))
                .limit(5)
                .collect(Collectors.toList());
            
            data.append("\nTOP RISK FILES:\n");
            topRiskFiles.forEach(fm -> 
                data.append(String.format("%s: Risk=%.0f, Complexity=%d\n",
                    fm.getClassName(),
                    fm.getRiskScore(),
                    fm.getCodeComplexity() != null ? fm.getCodeComplexity().getCyclomaticComplexity() : 0
                )));
            
            // AVERAGES
            double avgRisk = fileMetadata.stream().mapToDouble(MetadataAnalyzer.FileMetadata::getRiskScore).average().orElse(0.0);
            double avgComplexity = fileMetadata.stream()
                .filter(fm -> fm.getCodeComplexity() != null)
                .mapToDouble(fm -> fm.getCodeComplexity().getCyclomaticComplexity())
                .average().orElse(0.0);
            
            data.append(String.format("\nAVERAGES: Risk=%.0f, Complexity=%.0f\n", avgRisk, avgComplexity));
        }
        
        return data.toString();
    }

    public RepositoryAnalysisResponse getAnalysisFromMongo(String htmlUrl, String main) {
        return analysisMongoUtil.getRepositoryAnalysisResponse(htmlUrl,main);
    }

    private String generateMetadataSummary(List<MetadataAnalyzer.FileMetadata> fileMetadata) {
        if (fileMetadata == null || fileMetadata.isEmpty()) {
            return "No metadata analysis available.";
        }

        StringBuilder summary = new StringBuilder();
        
        // Overall statistics
        double avgRiskScore = fileMetadata.stream()
                .mapToDouble(MetadataAnalyzer.FileMetadata::getRiskScore)
                .average()
                .orElse(0.0);
        
        int avgCyclomaticComplexity = (int) fileMetadata.stream()
                .filter(fm -> fm.getCodeComplexity() != null)
                .mapToInt(fm -> fm.getCodeComplexity().getCyclomaticComplexity())
                .average()
                .orElse(0.0);
        
        int avgCognitiveComplexity = (int) fileMetadata.stream()
                .filter(fm -> fm.getCodeComplexity() != null)
                .mapToInt(fm -> fm.getCodeComplexity().getCognitiveComplexity())
                .average()
                .orElse(0.0);
        
        long highRiskFiles = fileMetadata.stream()
                .filter(fm -> fm.getRiskScore() > 70.0)
                .count();
        
        long businessCriticalFiles = fileMetadata.stream()
                .filter(fm -> fm.getBusinessComplexity() != null && 
                        fm.getBusinessComplexity().getBusinessCriticality() > 7.0)
                .count();

        summary.append(String.format("Code Quality Metrics:\n"));
        summary.append(String.format("- Average Risk Score: %.1f/100\n", avgRiskScore));
        summary.append(String.format("- Average Cyclomatic Complexity: %d\n", avgCyclomaticComplexity));
        summary.append(String.format("- Average Cognitive Complexity: %d\n", avgCognitiveComplexity));
        summary.append(String.format("- High-Risk Files (>70 risk score): %d\n", highRiskFiles));
        summary.append(String.format("- Business-Critical Files: %d\n", businessCriticalFiles));
        
        if (highRiskFiles > 0) {
            summary.append("\nTop High-Risk Files:\n");
            fileMetadata.stream()
                    .filter(fm -> fm.getRiskScore() > 70.0)
                    .sorted((f1, f2) -> Double.compare(f2.getRiskScore(), f1.getRiskScore()))
                    .limit(3)
                    .forEach(fm -> summary.append(String.format("- %s (Risk: %.1f, Cyclomatic: %d)\n", 
                            fm.getClassName(), 
                            fm.getRiskScore(),
                            fm.getCodeComplexity() != null ? fm.getCodeComplexity().getCyclomaticComplexity() : 0)));
        }

        return summary.toString();
    }

    /**
     * Logs a summary of the metadata analysis results
     */
    private void logMetadataAnalysisSummary(List<MetadataAnalyzer.FileMetadata> fileMetadata) {
        if (fileMetadata == null || fileMetadata.isEmpty()) {
            log.info("No metadata analysis results available");
            return;
        }

        log.info("=== METADATA ANALYSIS SUMMARY ===");
        log.info("Total files analyzed: {}", fileMetadata.size());
        
        // Risk distribution
        long highRisk = fileMetadata.stream().filter(fm -> fm.getRiskScore() > 70.0).count();
        long mediumRisk = fileMetadata.stream().filter(fm -> fm.getRiskScore() > 40.0 && fm.getRiskScore() <= 70.0).count();
        long lowRisk = fileMetadata.stream().filter(fm -> fm.getRiskScore() <= 40.0).count();
        
        log.info("Risk Distribution - High: {}, Medium: {}, Low: {}", highRisk, mediumRisk, lowRisk);
        
        // Complexity statistics
        double avgRisk = fileMetadata.stream().mapToDouble(MetadataAnalyzer.FileMetadata::getRiskScore).average().orElse(0.0);
        log.info("Average risk score: {:.1f}", avgRisk);
        
        if (highRisk > 0) {
            log.info("High-risk files requiring immediate attention:");
            fileMetadata.stream()
                    .filter(fm -> fm.getRiskScore() > 70.0)
                    .sorted((f1, f2) -> Double.compare(f2.getRiskScore(), f1.getRiskScore()))
                    .limit(5)
                    .forEach(fm -> log.info("  - {} (Risk: {:.1f})", fm.getClassName(), fm.getRiskScore()));
        }
        
        log.info("=== END METADATA ANALYSIS SUMMARY ===");
    }

    /**
     * Extract workspace ID from repo directory path
     */
    private String extractWorkspaceId(String repoDir) {
        if (repoDir == null || repoDir.isEmpty()) return "";
        Path path = Paths.get(repoDir);
        Path parent = path.getParent();
        if (parent != null) {
            return parent.getFileName().toString();
        }
        return path.getFileName().toString();
    }

    /**
     * Generate a unique workspace ID
     */
    private String generateWorkspaceId() {
        return "workspace-" + System.currentTimeMillis() + "-" + ThreadLocalRandom.current().nextInt(1000, 9999);
    }

    /**
     * Get default exclude patterns for Java file search
     */
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

    /**
     * Convert simplified insights to legacy format for compatibility
     */
    private RepositoryInsights convertToLegacyInsights(SimplifiedRepositoryInsights simplified) {
        // Extract high-priority recommendation as critical actions
        List<String> criticalActions = simplified.getRecommendations().stream()
                .filter(rec -> "HIGH".equals(rec.getPriority()))
                .map(rec -> rec.getTitle() + ": " + rec.getDescription())
                .limit(3)
                .collect(Collectors.toList());

        // Convert high-risk files to testing gaps
        List<TestingGap> testingGaps = simplified.getCriticalFindings().getHighestRiskFiles().stream()
                .map(file -> TestingGap.builder()
                        .category("High-Risk File")
                        .description("Critical file requiring test coverage: " + file.getReason())
                        .priority("HIGH")
                        .estimatedEffort("2-4 hours")
                        .build())
                .collect(Collectors.toList());

        return RepositoryInsights.builder()
                .executiveSummary("Repository risk level: " + simplified.getRepositorySummary().getOverallRiskLevel() + 
                                ", Complexity: " + simplified.getRepositorySummary().getComplexityScore() + "/10")
                .repositoryComplexity(mapComplexityScoreToLevel(simplified.getRepositorySummary().getComplexityScore()))
                .dominantPatterns(List.of("Spring Boot Application", "Layered Architecture"))
                .keyFindings(simplified.getRepositorySummary().getPrimaryConcerns())
                .criticalActions(criticalActions)
                .testingGaps(testingGaps)
                .architecturalInsights(simplified.getCriticalFindings().getArchitecturalIssues())
                .codeQualityAssessment(RepositoryInsights.CodeQualityAssessment.builder()
                        .overallGrade(simplified.getRepositorySummary().getCoverageGrade())
                        .complexityLevel(mapComplexityScoreToLevel(simplified.getRepositorySummary().getComplexityScore()))
                        .maintainabilityScore("75%") // Default estimation
                        .qualityIssues(simplified.getCriticalFindings().getCoverageGaps())
                        .strengthAreas(List.of("Structured codebase", "Modern Java version"))
                        .build())
                .businessImpactAnalysis(RepositoryInsights.BusinessImpactAnalysis.builder()
                        .riskLevel(simplified.getRepositorySummary().getOverallRiskLevel())
                        .businessCriticalityLevel("Medium impact on business operations")
                        .highRiskComponents(simplified.getCriticalFindings().getHighestRiskFiles().stream()
                                .map(SimplifiedRepositoryInsights.HighRiskFile::getFileName)
                                .collect(Collectors.toList()))
                        .estimatedBusinessImpact("Moderate risk to business continuity")
                        .complianceConsiderations(List.of("Code quality standards", "Testing requirements"))
                        .build())
                .technicalDebtAssessment(RepositoryInsights.TechnicalDebtAssessment.builder()
                        .debtLevel(mapRiskToDebtLevel(simplified.getRepositorySummary().getOverallRiskLevel()))
                        .estimatedRefactoringEffort("2-4 weeks for major improvements")
                        .debtHotspots(simplified.getCriticalFindings().getHighestRiskFiles().stream()
                                .map(SimplifiedRepositoryInsights.HighRiskFile::getFileName)
                                .collect(Collectors.toList()))
                        .refactoringPriorities(simplified.getRecommendations().stream()
                                .map(SimplifiedRepositoryInsights.Recommendation::getTitle)
                                .collect(Collectors.toList()))
                        .maintainabilityTrend("STABLE")
                        .build())
                .coverageStrategy(CoverageStrategy.builder()
                        .recommendedApproach("Focus on high-risk files first, then expand systematically")
                        .priorityAreas(simplified.getCriticalFindings().getHighestRiskFiles().stream()
                                .map(SimplifiedRepositoryInsights.HighRiskFile::getFileName)
                                .collect(Collectors.toList()))
                        .estimatedTimeToImprove("1-2 weeks for significant improvement")
                        .build())
                .riskAssessment(RiskAssessment.builder()
                        .level(simplified.getRepositorySummary().getOverallRiskLevel())
                        .factors(simplified.getRepositorySummary().getPrimaryConcerns())
                        .build())
                .build();
    }

    private String mapComplexityScoreToLevel(Integer score) {
        if (score == null) return "MEDIUM";
        if (score <= 3) return "LOW";
        if (score <= 7) return "MEDIUM";
        return "HIGH";
    }

    private String mapRiskToDebtLevel(String riskLevel) {
        switch (riskLevel.toUpperCase()) {
            case "LOW": return "LOW";
            case "HIGH": 
            case "CRITICAL": return "HIGH";
            default: return "MEDIUM";
        }
    }

    /**
     * Get simplified repository insights (new focused format)
     */
    public SimplifiedRepositoryInsights getSimplifiedRepositoryInsights(String repoDir, List<String> javaFiles,
            ProjectConfiguration config, List<MetadataAnalyzer.FileMetadata> fileMetadata,
            List<CoverageData> existingCoverage) {
        return generateSimplifiedRepositoryInsights(repoDir, javaFiles, config, fileMetadata, existingCoverage);
    }

    /**
     * Extract JSON content from AI response
     */
    private String extractJsonFromResponse(String response) {
        int startIndex = response.indexOf('{');
        int endIndex = response.lastIndexOf('}') + 1;

        if (startIndex >= 0 && endIndex > startIndex) {
            return response.substring(startIndex, endIndex);
        }

        throw new IllegalArgumentException("No valid JSON found in AI response");
    }

    /**
     * Parse string array from JSON node
     */
    private List<String> parseStringArray(JsonNode arrayNode) {
        List<String> result = new ArrayList<>();
        if (arrayNode != null && arrayNode.isArray()) {
            for (JsonNode node : arrayNode) {
                result.add(node.asText());
            }
        }
        return result;
    }

    /**
     * Get JSON text value safely
     */
    private String getJsonText(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        return field != null ? field.asText() : "";
    }

    /**
     * Parse testing gaps from JSON node
     */
    private List<TestingGap> parseTestingGaps(JsonNode gapsNode) {
        List<TestingGap> gaps = new ArrayList<>();
        if (gapsNode != null && gapsNode.isArray()) {
            for (JsonNode gapNode : gapsNode) {
                gaps.add(TestingGap.builder()
                        .category(gapNode.get("category") != null ? gapNode.get("category").asText() : "")
                        .description(gapNode.get("description") != null ? gapNode.get("description").asText() : "")
                        .priority(gapNode.get("priority") != null ? gapNode.get("priority").asText() : "")
                        .estimatedEffort(gapNode.get("estimatedEffort") != null ? gapNode.get("estimatedEffort").asText() : "")
                        .build());
            }
        }
        return gaps;
    }

    /**
     * Parse coverage strategy from JSON node
     */
    private CoverageStrategy parseCoverageStrategy(JsonNode strategyNode) {
        if (strategyNode == null) return null;

        return CoverageStrategy.builder()
                .recommendedApproach(strategyNode.get("recommendedApproach") != null ? strategyNode.get("recommendedApproach").asText() : "")
                .priorityAreas(parseStringArray(strategyNode.get("priorityAreas")))
                .estimatedTimeToImprove(strategyNode.get("estimatedTimeToImprove") != null ? strategyNode.get("estimatedTimeToImprove").asText() : "")
                .build();
    }

    /**
     * Parse risk assessment from JSON node
     */
    private RiskAssessment parseRiskAssessment(JsonNode riskNode) {
        if (riskNode == null) return null;

        return RiskAssessment.builder()
                .level(riskNode.get("level") != null ? riskNode.get("level").asText() : "")
                .factors(parseStringArray(riskNode.get("factors")))
                .build();
    }

    /**
     * Parse code quality assessment from JSON node
     */
    private RepositoryInsights.CodeQualityAssessment parseCodeQualityAssessment(JsonNode node) {
        if (node == null) return null;
        
        return RepositoryInsights.CodeQualityAssessment.builder()
                .overallGrade(getJsonText(node, "overallGrade"))
                .complexityLevel(getJsonText(node, "complexityLevel"))
                .maintainabilityScore(getJsonText(node, "maintainabilityScore"))
                .qualityIssues(parseStringArray(node.get("qualityIssues")))
                .strengthAreas(parseStringArray(node.get("strengthAreas")))
                .build();
    }

    /**
     * Parse business impact analysis from JSON node
     */
    private RepositoryInsights.BusinessImpactAnalysis parseBusinessImpactAnalysis(JsonNode node) {
        if (node == null) return null;
        
        return RepositoryInsights.BusinessImpactAnalysis.builder()
                .riskLevel(getJsonText(node, "riskLevel"))
                .businessCriticalityLevel(getJsonText(node, "businessCriticalityLevel"))
                .highRiskComponents(parseStringArray(node.get("highRiskComponents")))
                .estimatedBusinessImpact(getJsonText(node, "estimatedBusinessImpact"))
                .complianceConsiderations(parseStringArray(node.get("complianceConsiderations")))
                .build();
    }

    /**
     * Parse technical debt assessment from JSON node
     */
    private RepositoryInsights.TechnicalDebtAssessment parseTechnicalDebtAssessment(JsonNode node) {
        if (node == null) return null;
        
        return RepositoryInsights.TechnicalDebtAssessment.builder()
                .debtLevel(getJsonText(node, "debtLevel"))
                .estimatedRefactoringEffort(getJsonText(node, "estimatedRefactoringEffort"))
                .debtHotspots(parseStringArray(node.get("debtHotspots")))
                .refactoringPriorities(parseStringArray(node.get("refactoringPriorities")))
                .maintainabilityTrend(getJsonText(node, "maintainabilityTrend"))
                .build();
    }
}
