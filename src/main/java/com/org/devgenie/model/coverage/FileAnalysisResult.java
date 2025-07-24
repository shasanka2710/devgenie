package com.org.devgenie.model.coverage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileAnalysisResult {
    private String filePath;
    private String complexity;
    private String businessLogicPriority;
    private List<TestableComponent> testableComponents;
    private List<UncoveredCodePath> uncoveredCodePaths;
    private List<String> dependencies;
    private String estimatedEffort;
    private String coverageImpactPotential;
    private double currentCoverage;

    // Additional helper methods required by CoverageAgentService
    public List<String> getUncoveredMethods() {
        return testableComponents.stream()
                .filter(component -> !component.isCurrentlyCovered())
                .map(TestableComponent::getMethodName)
                .collect(java.util.stream.Collectors.toList());
    }

    public List<String> getComplexMethods() {
        return testableComponents.stream()
                .filter(component -> "HIGH".equals(component.getComplexity()) || "VERY_HIGH".equals(component.getComplexity()))
                .map(TestableComponent::getMethodName)
                .collect(java.util.stream.Collectors.toList());
    }

    public double getComplexityScore() {
        // Calculate complexity score based on complexity string and testable components
        if (complexity == null) return 0.0;

        switch (complexity.toUpperCase()) {
            case "LOW": return 2.0;
            case "MEDIUM": return 5.0;
            case "HIGH": return 8.0;
            case "VERY_HIGH": return 10.0;
            default: return 3.0;
        }
    }

    public String getPackageName() {
        if (filePath == null) return "";

        // Extract package name from file path
        // e.g., "src/main/java/com/org/devgenie/service/UserService.java" -> "com.org.devgenie.service"
        String packagePath = filePath.replace("src/main/java/", "")
                                   .replace("src/test/java/", "")
                                   .replace("/", ".");

        // Remove the filename part
        int lastDotIndex = packagePath.lastIndexOf('.');
        if (lastDotIndex > 0) {
            String extension = packagePath.substring(lastDotIndex);
            if (extension.equals(".java")) {
                // Remove .java and the class name
                packagePath = packagePath.substring(0, lastDotIndex);
                lastDotIndex = packagePath.lastIndexOf('.');
                if (lastDotIndex > 0) {
                    return packagePath.substring(0, lastDotIndex);
                }
            }
        }

        return packagePath.contains(".") ? packagePath.substring(0, packagePath.lastIndexOf('.')) : "";
    }
}

