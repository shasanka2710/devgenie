package com.org.devgenie.service.coverage;

import com.org.devgenie.model.coverage.FileImprovementOpportunity;
import com.org.devgenie.model.coverage.FileCoverageData;
import com.org.devgenie.service.metadata.MetadataAnalyzer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class FileImprovementOpportunityService {

    @Autowired
    private MetadataAnalyzer metadataAnalyzer;

    /**
     * Generate file-level improvement opportunities based on coverage data and metadata analysis
     */
    public List<FileImprovementOpportunity> generateImprovementOpportunities(
            FileCoverageData fileCoverageData,
            MetadataAnalyzer.FileMetadata fileMetadata) {
        
        List<FileImprovementOpportunity> opportunities = new ArrayList<>();
        
        // Opportunity 1: Uncovered Methods
        if (fileCoverageData.getTotalMethods() > fileCoverageData.getCoveredMethods()) {
            opportunities.add(createUncoveredMethodsOpportunity(fileCoverageData, fileMetadata));
        }
        
        // Opportunity 2: Low Branch Coverage
        if (fileCoverageData.getBranchCoverage() < 70.0 && fileCoverageData.getTotalBranches() > 0) {
            opportunities.add(createBranchCoverageOpportunity(fileCoverageData, fileMetadata));
        }
        
        // Opportunity 3: High Risk Code (based on metadata)
        if (fileMetadata != null && fileMetadata.getRiskScore() > 70.0) {
            opportunities.add(createHighRiskCodeOpportunity(fileCoverageData, fileMetadata));
        }
        
        // Opportunity 4: Business Critical Code
        if (fileMetadata != null && fileMetadata.getBusinessComplexity() != null &&
            fileMetadata.getBusinessComplexity().getBusinessCriticality() > 7.0) {
            opportunities.add(createBusinessCriticalOpportunity(fileCoverageData, fileMetadata));
        }
        
        // Opportunity 5: Complex Logic
        if (fileMetadata != null && fileMetadata.getCodeComplexity() != null &&
            fileMetadata.getCodeComplexity().getCognitiveComplexity() > 15) {
            opportunities.add(createComplexLogicOpportunity(fileCoverageData, fileMetadata));
        }
        
        return opportunities;
    }
    
    private FileImprovementOpportunity createUncoveredMethodsOpportunity(
            FileCoverageData fileCoverageData, MetadataAnalyzer.FileMetadata fileMetadata) {
        
        int uncoveredMethods = fileCoverageData.getTotalMethods() - fileCoverageData.getCoveredMethods();
        double estimatedIncrease = (double) uncoveredMethods / fileCoverageData.getTotalMethods() * 100;
        
        List<String> actions = List.of(
            "Write unit tests for uncovered methods",
            "Focus on methods with high complexity first",
            "Use mocking for external dependencies"
        );
        
        return FileImprovementOpportunity.builder()
                .filePath(fileCoverageData.getFilePath())
                .fileName(fileCoverageData.getFileName())
                .type(FileImprovementOpportunity.OpportunityType.UNCOVERED_METHODS)
                .title(uncoveredMethods + " uncovered methods detected")
                .description(String.format("File has %d methods without test coverage out of %d total methods",
                        uncoveredMethods, fileCoverageData.getTotalMethods()))
                .priority(uncoveredMethods > 5 ? "HIGH" : "MEDIUM")
                .estimatedCoverageIncrease(estimatedIncrease)
                .estimatedEffort(estimateEffort(uncoveredMethods))
                .recommendedActions(actions)
                .complexityMetrics(buildComplexityMetrics(fileMetadata))
                .build();
    }
    
    private FileImprovementOpportunity createBranchCoverageOpportunity(
            FileCoverageData fileCoverageData, MetadataAnalyzer.FileMetadata fileMetadata) {
        
        int uncoveredBranches = fileCoverageData.getTotalBranches() - fileCoverageData.getCoveredBranches();
        double estimatedIncrease = 20.0; // Estimated increase from branch coverage improvement
        
        List<String> actions = List.of(
            "Add test cases for edge cases",
            "Test both true and false conditions",
            "Cover exception handling paths",
            "Test loop boundary conditions"
        );
        
        return FileImprovementOpportunity.builder()
                .filePath(fileCoverageData.getFilePath())
                .fileName(fileCoverageData.getFileName())
                .type(FileImprovementOpportunity.OpportunityType.EDGE_CASES)
                .title("Edge cases need test coverage")
                .description(String.format("Branch coverage is %.1f%% with %d uncovered branches", 
                        fileCoverageData.getBranchCoverage(), uncoveredBranches))
                .priority("MEDIUM")
                .estimatedCoverageIncrease(estimatedIncrease)
                .estimatedEffort("2-4 hours")
                .recommendedActions(actions)
                .complexityMetrics(buildComplexityMetrics(fileMetadata))
                .build();
    }
    
    private FileImprovementOpportunity createHighRiskCodeOpportunity(
            FileCoverageData fileCoverageData, MetadataAnalyzer.FileMetadata fileMetadata) {
        
        List<String> actions = List.of(
            "Prioritize testing for this high-risk file",
            "Consider refactoring before adding tests",
            "Use comprehensive integration tests",
            "Add error scenario testing"
        );
        
        return FileImprovementOpportunity.builder()
                .filePath(fileCoverageData.getFilePath())
                .fileName(fileCoverageData.getFileName())
                .type(FileImprovementOpportunity.OpportunityType.HIGH_RISK_CODE)
                .title("High-risk code requires immediate attention")
                .description(String.format("File has risk score of %.1f (>70 is high risk)", 
                        fileMetadata.getRiskScore()))
                .priority("HIGH")
                .estimatedCoverageIncrease(25.0)
                .estimatedEffort("4-8 hours")
                .recommendedActions(actions)
                .complexityMetrics(buildComplexityMetrics(fileMetadata))
                .build();
    }
    
    private FileImprovementOpportunity createBusinessCriticalOpportunity(
            FileCoverageData fileCoverageData, MetadataAnalyzer.FileMetadata fileMetadata) {
        
        List<String> actions = List.of(
            "Implement comprehensive business logic tests",
            "Add validation and error handling tests",
            "Test business rule edge cases",
            "Consider property-based testing"
        );
        
        return FileImprovementOpportunity.builder()
                .filePath(fileCoverageData.getFilePath())
                .fileName(fileCoverageData.getFileName())
                .type(FileImprovementOpportunity.OpportunityType.BUSINESS_CRITICAL)
                .title("Business-critical logic needs comprehensive testing")
                .description(String.format("File contains critical business logic (criticality: %.1f)", 
                        fileMetadata.getBusinessComplexity().getBusinessCriticality()))
                .priority("HIGH")
                .estimatedCoverageIncrease(30.0)
                .estimatedEffort("6-12 hours")
                .recommendedActions(actions)
                .complexityMetrics(buildComplexityMetrics(fileMetadata))
                .build();
    }
    
    private FileImprovementOpportunity createComplexLogicOpportunity(
            FileCoverageData fileCoverageData, MetadataAnalyzer.FileMetadata fileMetadata) {
        
        List<String> actions = List.of(
            "Break down complex methods for easier testing",
            "Test each logical path separately",
            "Use parameterized tests for complex scenarios",
            "Consider refactoring to reduce complexity"
        );
        
        return FileImprovementOpportunity.builder()
                .filePath(fileCoverageData.getFilePath())
                .fileName(fileCoverageData.getFileName())
                .type(FileImprovementOpportunity.OpportunityType.COMPLEX_LOGIC)
                .title("Complex logic requires careful testing")
                .description(String.format("File has high cognitive complexity (%d > 15)", 
                        fileMetadata.getCodeComplexity().getCognitiveComplexity()))
                .priority("MEDIUM")
                .estimatedCoverageIncrease(20.0)
                .estimatedEffort("4-6 hours")
                .recommendedActions(actions)
                .complexityMetrics(buildComplexityMetrics(fileMetadata))
                .build();
    }
    
    private FileImprovementOpportunity.ComplexityMetrics buildComplexityMetrics(
            MetadataAnalyzer.FileMetadata fileMetadata) {
        
        if (fileMetadata == null) {
            return FileImprovementOpportunity.ComplexityMetrics.builder()
                    .cyclomaticComplexity(0)
                    .cognitiveComplexity(0)
                    .businessCriticality(0.0)
                    .riskScore(0.0)
                    .build();
        }
        
        return FileImprovementOpportunity.ComplexityMetrics.builder()
                .cyclomaticComplexity(fileMetadata.getCodeComplexity() != null ? 
                        fileMetadata.getCodeComplexity().getCyclomaticComplexity() : 0)
                .cognitiveComplexity(fileMetadata.getCodeComplexity() != null ? 
                        fileMetadata.getCodeComplexity().getCognitiveComplexity() : 0)
                .businessCriticality(fileMetadata.getBusinessComplexity() != null ? 
                        fileMetadata.getBusinessComplexity().getBusinessCriticality() : 0.0)
                .riskScore(fileMetadata.getRiskScore())
                .build();
    }
    
    private String estimateEffort(int uncoveredMethods) {
        if (uncoveredMethods <= 3) return "1-2 hours";
        if (uncoveredMethods <= 6) return "2-4 hours";
        if (uncoveredMethods <= 10) return "4-6 hours";
        return "6-12 hours";
    }
}
