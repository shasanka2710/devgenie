package com.org.devgenie.service.coverage;

import com.org.devgenie.model.coverage.CoverageData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class AiImprovementService {

    @Autowired
    private ChatClient chatClient;

    public List<ImprovementOpportunity> generateImprovementOpportunities(CoverageData coverageData) {
        List<ImprovementOpportunity> opportunities = new ArrayList<>();
        
        try {
            // Generate AI-powered improvement suggestions
            String prompt = createImprovementPrompt(coverageData);
            String aiResponse = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
            
            // Parse AI response and create opportunities
            opportunities.addAll(parseAiResponse(aiResponse, coverageData));
            
        } catch (Exception e) {
            log.warn("Failed to generate AI improvement opportunities for {}: {}", 
                    coverageData.getPath(), e.getMessage());
        }
        
        // Fallback to rule-based opportunities if AI fails
        opportunities.addAll(generateRuleBasedOpportunities(coverageData));
        
        return opportunities;
    }

    private String createImprovementPrompt(CoverageData coverageData) {
        return String.format("""
            Analyze the following Java file coverage metrics and provide specific improvement opportunities:
            
            File: %s
            Class: %s
            Package: %s
            
            Coverage Metrics:
            - Line Coverage: %.1f%% (%d/%d lines covered)
            - Branch Coverage: %.1f%% (%d/%d branches covered)  
            - Method Coverage: %.1f%% (%d/%d methods covered)
            
            Please provide 2-3 specific, actionable improvement opportunities focusing on:
            1. Uncovered methods that should be tested
            2. Missing edge cases and boundary conditions
            3. Complex logic that needs better branch coverage
            4. Realistic coverage improvement estimates
            
            Format your response as:
            OPPORTUNITY: [Type] - [Description] - [Priority: HIGH/MEDIUM/LOW] - [Estimated Impact: +X%%]
            """, 
            coverageData.getFileName(),
            coverageData.getClassName(),
            coverageData.getPackageName(),
            coverageData.getLineCoverage(),
            coverageData.getCoveredLines(),
            coverageData.getTotalLines(),
            coverageData.getBranchCoverage(),
            coverageData.getCoveredBranches(), 
            coverageData.getTotalBranches(),
            coverageData.getMethodCoverage(),
            coverageData.getCoveredMethods(),
            coverageData.getTotalMethods()
        );
    }

    private List<ImprovementOpportunity> parseAiResponse(String aiResponse, CoverageData coverageData) {
        List<ImprovementOpportunity> opportunities = new ArrayList<>();
        
        if (aiResponse != null && !aiResponse.trim().isEmpty()) {
            String[] lines = aiResponse.split("\n");
            for (String line : lines) {
                if (line.trim().startsWith("OPPORTUNITY:")) {
                    try {
                        ImprovementOpportunity opportunity = parseOpportunityLine(line);
                        if (opportunity != null) {
                            opportunities.add(opportunity);
                        }
                    } catch (Exception e) {
                        log.debug("Failed to parse opportunity line: {}", line);
                    }
                }
            }
        }
        
        return opportunities;
    }

    private ImprovementOpportunity parseOpportunityLine(String line) {
        // Parse format: OPPORTUNITY: [Type] - [Description] - [Priority] - [Impact]
        String content = line.substring("OPPORTUNITY:".length()).trim();
        String[] parts = content.split(" - ");
        
        if (parts.length >= 3) {
            String type = parts[0].trim();
            String description = parts[1].trim();
            String priority = parts.length > 2 ? parts[2].replace("Priority:", "").trim() : "MEDIUM";
            String impact = parts.length > 3 ? parts[3].replace("Estimated Impact:", "").trim() : "+5%";
            
            return ImprovementOpportunity.builder()
                    .type(type)
                    .description(description)
                    .priority(priority)
                    .estimatedImpact(impact)
                    .build();
        }
        
        return null;
    }

    private List<ImprovementOpportunity> generateRuleBasedOpportunities(CoverageData coverageData) {
        List<ImprovementOpportunity> opportunities = new ArrayList<>();
        
        // Rule 1: Uncovered methods
        int uncoveredMethods = coverageData.getTotalMethods() - coverageData.getCoveredMethods();
        if (uncoveredMethods > 0) {
            opportunities.add(ImprovementOpportunity.builder()
                    .type("UNCOVERED_METHODS")
                    .description(uncoveredMethods + " uncovered methods detected")
                    .priority(uncoveredMethods > 5 ? "HIGH" : uncoveredMethods > 2 ? "MEDIUM" : "LOW")
                    .estimatedImpact("+" + Math.min(15, uncoveredMethods * 2) + "%")
                    .build());
        }
        
        // Rule 2: Low branch coverage
        if (coverageData.getBranchCoverage() < 70 && coverageData.getTotalBranches() > 0) {
            int uncoveredBranches = coverageData.getTotalBranches() - coverageData.getCoveredBranches();
            opportunities.add(ImprovementOpportunity.builder()
                    .type("EDGE_CASES")
                    .description("Edge cases and conditional logic need test coverage")
                    .priority("MEDIUM")
                    .estimatedImpact("+" + Math.min(10, uncoveredBranches) + "%")
                    .build());
        }
        
        // Rule 3: Low line coverage
        if (coverageData.getLineCoverage() < 80) {
            double potentialImprovement = Math.min(20.0, (90 - coverageData.getLineCoverage()) * 0.7);
            opportunities.add(ImprovementOpportunity.builder()
                    .type("OVERALL_IMPROVEMENT")
                    .description("Estimated +" + String.format("%.0f", potentialImprovement) + "% coverage possible")
                    .priority("INFO")
                    .estimatedImpact("+" + String.format("%.0f", potentialImprovement) + "%")
                    .build());
        }
        
        // Rule 4: Complex files with low coverage
        if (coverageData.getTotalLines() > 100 && coverageData.getLineCoverage() < 60) {
            opportunities.add(ImprovementOpportunity.builder()
                    .type("COMPLEX_FILE")
                    .description("Large file with low coverage - consider refactoring and focused testing")
                    .priority("HIGH")
                    .estimatedImpact("+12%")
                    .build());
        }
        
        return opportunities;
    }

    @lombok.Data
    @lombok.Builder
    public static class ImprovementOpportunity {
        private String type;
        private String description;
        private String priority;
        private String estimatedImpact;
    }
}
