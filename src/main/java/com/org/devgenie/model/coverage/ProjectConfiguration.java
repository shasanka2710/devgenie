package com.org.devgenie.model.coverage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectConfiguration {
    private String buildTool; // maven, gradle, sbt
    private String testFramework; // junit4, junit5, testng, spock
    private String javaVersion;
    private List<String> dependencies;
    private List<String> testDirectories;
    private boolean isSpringBoot;
    private ProjectCharacteristics projectCharacteristics;
    private BuildCommands buildCommands;
    private LocalDateTime detectedAt;

    // Additional methods for convenience
    public boolean isModernJava() {
        try {
            return Integer.parseInt(javaVersion) >= 11;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public boolean isJUnit5() {
        return "junit5".equals(testFramework);
    }

    public boolean isGradleProject() {
        return "gradle".equals(buildTool);
    }

    public boolean isMavenProject() {
        return "maven".equals(buildTool);
    }

    public boolean hasSpringBootTest() {
        return dependencies.contains("spring-test") || dependencies.contains("spring-boot-starter-test");
    }

    public boolean hasMockito() {
        return dependencies.contains("mockito");
    }

    public boolean hasTestContainers() {
        return dependencies.contains("testcontainers");
    }

    public String getMainTestDirectory() {
        return testDirectories.isEmpty() ? "src/test/java" : testDirectories.get(0);
    }

    public String getProjectComplexity() {
        if (projectCharacteristics != null) {
            long fileCount = projectCharacteristics.getJavaFileCount();
            if (fileCount > 100) return "HIGH";
            if (fileCount > 20) return "MEDIUM";
            return "LOW";
        }
        return "UNKNOWN";
    }
}
