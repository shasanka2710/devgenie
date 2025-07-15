package com.org.devgenie.config;

import com.org.devgenie.model.coverage.ProjectConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

@Service
@Slf4j
public class JacocoConfigurationService {

    @Autowired
    private ChatClient chatClient;

    @Value("${jacoco.auto-configure.enabled:true}")
    private boolean autoConfigureEnabled;

    @Value("${jacoco.auto-configure.backup-original:true}")
    private boolean backupOriginalFiles;

    /**
     * Check if Jacoco is already configured in the project
     */
    public boolean isJacocoConfigured(String repoDir, ProjectConfiguration projectConfig) {
        String buildTool = projectConfig.getBuildTool().toLowerCase();

        try {
            switch (buildTool) {
                case "maven":
                    return isMavenJacocoConfigured(repoDir);
                case "gradle":
                    return isGradleJacocoConfigured(repoDir);
                case "sbt":
                    return isSbtCoverageConfigured(repoDir);
                default:
                    return false;
            }
        } catch (Exception e) {
            log.error("Failed to check Jacoco configuration", e);
            return false;
        }
    }

    /**
     * Auto-configure Jacoco for the project
     */
    public boolean autoConfigureJacoco(String repoDir, ProjectConfiguration projectConfig) {
        if (!autoConfigureEnabled) {
            log.info("Jacoco auto-configuration is disabled");
            return false;
        }

        String buildTool = projectConfig.getBuildTool().toLowerCase();

        try {
            log.info("Auto-configuring Jacoco for {} project", buildTool);

            switch (buildTool) {
                case "maven":
                    return autoConfigureMavenJacoco(repoDir, projectConfig);
                case "gradle":
                    return autoConfigureGradleJacoco(repoDir, projectConfig);
                case "sbt":
                    return autoConfigureSbtCoverage(repoDir, projectConfig);
                default:
                    log.warn("Auto-configuration not supported for build tool: {}", buildTool);
                    return false;
            }
        } catch (Exception e) {
            log.error("Failed to auto-configure Jacoco", e);
            return false;
        }
    }

    private boolean isMavenJacocoConfigured(String repoDir) throws IOException {
        Path pomPath = Paths.get(repoDir, "pom.xml");
        if (!Files.exists(pomPath)) return false;

        String pomContent = Files.readString(pomPath);
        return pomContent.contains("jacoco-maven-plugin") ||
                pomContent.contains("org.jacoco");
    }

    private boolean isGradleJacocoConfigured(String repoDir) throws IOException {
        Path buildGradlePath = Paths.get(repoDir, "build.gradle");
        Path buildGradleKtsPath = Paths.get(repoDir, "build.gradle.kts");

        String buildContent = "";
        if (Files.exists(buildGradlePath)) {
            buildContent = Files.readString(buildGradlePath);
        } else if (Files.exists(buildGradleKtsPath)) {
            buildContent = Files.readString(buildGradleKtsPath);
        } else {
            return false;
        }

        return buildContent.contains("jacoco") ||
                buildContent.contains("JacocoReport") ||
                buildContent.contains("jacocoTestReport");
    }

    private boolean isSbtCoverageConfigured(String repoDir) throws IOException {
        // Check for scoverage plugin
        Path projectDir = Paths.get(repoDir, "project");
        if (!Files.exists(projectDir)) return false;

        try (Stream<Path> paths = Files.walk(projectDir)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".sbt"))
                    .anyMatch(this::containsScoveragePlugin);
        }
    }

    private boolean containsScoveragePlugin(Path sbtFile) {
        try {
            String content = Files.readString(sbtFile);
            return content.contains("scoverage") || content.contains("sbt-scoverage");
        } catch (IOException e) {
            return false;
        }
    }

    private boolean autoConfigureMavenJacoco(String repoDir, ProjectConfiguration projectConfig) throws IOException {
        Path pomPath = Paths.get(repoDir, "pom.xml");
        String originalPom = Files.readString(pomPath);

        if (backupOriginalFiles) {
            createBackup(pomPath, originalPom);
        }

        // Use AI to generate Jacoco configuration
        String jacocoConfig = generateMavenJacocoConfig(originalPom, projectConfig);
        String updatedPom = insertJacocoIntoMaven(originalPom, jacocoConfig);

        Files.writeString(pomPath, updatedPom);
        log.info("Successfully auto-configured Jacoco for Maven project");

        return true;
    }

    private boolean autoConfigureGradleJacoco(String repoDir, ProjectConfiguration projectConfig) throws IOException {
        Path buildGradlePath = Paths.get(repoDir, "build.gradle");
        Path buildGradleKtsPath = Paths.get(repoDir, "build.gradle.kts");

        Path targetFile = Files.exists(buildGradlePath) ? buildGradlePath : buildGradleKtsPath;
        if (!Files.exists(targetFile)) return false;

        String originalBuild = Files.readString(targetFile);

        if (backupOriginalFiles) {
            createBackup(targetFile, originalBuild);
        }

        boolean isKotlinDsl = targetFile.toString().endsWith(".kts");
        String jacocoConfig = generateGradleJacocoConfig(originalBuild, projectConfig, isKotlinDsl);
        log.info("Generated Jacoco configuration for Gradle: {}", jacocoConfig);
        String updatedBuild = insertJacocoIntoGradle(originalBuild, jacocoConfig, isKotlinDsl);

        Files.writeString(targetFile, updatedBuild);
        log.info("Successfully auto-configured Jacoco for Gradle project");

        return true;
    }

    private boolean autoConfigureSbtCoverage(String repoDir, ProjectConfiguration projectConfig) throws IOException {
        Path projectDir = Paths.get(repoDir, "project");
        Path pluginsFile = projectDir.resolve("plugins.sbt");

        Files.createDirectories(projectDir);

        String scoveragePlugin = """
            addSbtPlugin("org.scoverage" % "sbt-scoverage" % "2.0.9")
            """;

        if (Files.exists(pluginsFile)) {
            String existing = Files.readString(pluginsFile);
            if (backupOriginalFiles) {
                createBackup(pluginsFile, existing);
            }
            Files.writeString(pluginsFile, existing + "\n" + scoveragePlugin);
        } else {
            Files.writeString(pluginsFile, scoveragePlugin);
        }

        log.info("Successfully auto-configured scoverage for SBT project");
        return true;
    }

    private String generateMavenJacocoConfig(String originalPom, ProjectConfiguration projectConfig) {
        String prompt = String.format("""
            Generate Jacoco Maven plugin configuration for this project.
            
            Project Configuration:
            - Test Framework: %s
            - Java Version: %s
            - Spring Boot: %s
            
            Existing POM snippet:
            %s
            
            Generate ONLY the Jacoco plugin configuration block that should be added to the <plugins> section.
            Include proper version, executions for prepare-agent, and report generation.
            Format as valid XML without markdown formatting.
            """,
                projectConfig.getTestFramework(),
                projectConfig.getJavaVersion(),
                projectConfig.isSpringBoot(),
                originalPom.substring(0, Math.min(originalPom.length(), 1000))
        );

        return chatClient.prompt(prompt).call().content();
    }

    private String generateGradleJacocoConfig(String originalBuild, ProjectConfiguration projectConfig, boolean isKotlinDsl) {

        String prompt = String.format("""
                Generate Jacoco Gradle configuration for this project.
                
                Constraints:
                - DO NOT rewrite the entire build.gradle file.
                - DO NOT touch or move the existing plugins {} block.
                - ONLY add the following:
                    1. A new plugin line: id 'jacoco'
                    2. Jacoco-specific configuration (jacocoTestReport task block).
                    3. Test task dependency (if needed).
                
                Project Configuration:
                - Test Framework: %s
                - Java Version: %s
                - Spring Boot: %s
                - Kotlin DSL: %s
                
                Existing build.gradle content:
                %s
                
                Expected Output:
                - Just the minimal Jacoco additions required to make `gradle test jacocoTestReport` work.
                - It should NOT reformat or rewrite unrelated parts of the build file.
                - Output must be in %s format (Groovy if false, Kotlin if true), without markdown formatting.
                """,
                projectConfig.getTestFramework(),
                projectConfig.getJavaVersion(),
                projectConfig.isSpringBoot(),
                isKotlinDsl,
                originalBuild,
                isKotlinDsl ? "Gradle Kotlin DSL" : "Gradle Groovy DSL"
        );

        log.info("Generated prompt for Gradle Jacoco configuration: {}", prompt);

        return chatClient.prompt(prompt).call().content();
    }

    private String insertJacocoIntoMaven(String originalPom, String jacocoConfig) {
        // Simple insertion logic - look for </plugins> and insert before it
        int pluginsEnd = originalPom.lastIndexOf("</plugins>");
        if (pluginsEnd == -1) {
            // No plugins section, need to create one
            int buildEnd = originalPom.lastIndexOf("</build>");
            if (buildEnd == -1) {
                // No build section, add it
                int projectEnd = originalPom.lastIndexOf("</project>");
                return originalPom.substring(0, projectEnd) +
                        "\n  <build>\n    <plugins>\n" + jacocoConfig + "\n    </plugins>\n  </build>\n" +
                        originalPom.substring(projectEnd);
            } else {
                return originalPom.substring(0, buildEnd) +
                        "    <plugins>\n" + jacocoConfig + "\n    </plugins>\n" +
                        originalPom.substring(buildEnd);
            }
        } else {
            return originalPom.substring(0, pluginsEnd) +
                    jacocoConfig + "\n" +
                    originalPom.substring(pluginsEnd);
        }
    }

    private String insertJacocoIntoGradle(String originalBuild, String jacocoConfig, boolean isKotlinDsl) {
        // Insert at the end of the file
        return originalBuild + "\n\n// Auto-generated Jacoco configuration\n" + jacocoConfig;
    }

    private void createBackup(Path originalFile, String content) throws IOException {
        Path backupFile = Paths.get(originalFile.toString() + ".backup." + System.currentTimeMillis());
        Files.writeString(backupFile, content);
        log.info("Created backup: {}", backupFile);
    }
}
