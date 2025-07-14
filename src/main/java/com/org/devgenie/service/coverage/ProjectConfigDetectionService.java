package com.org.devgenie.service.coverage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.org.devgenie.model.coverage.BuildCommands;
import com.org.devgenie.model.coverage.ProjectCharacteristics;
import com.org.devgenie.model.coverage.ProjectConfigValidation;
import com.org.devgenie.model.coverage.ProjectConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
public class ProjectConfigDetectionService {

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private ChatClient chatClient;

    public ProjectConfiguration detectProjectConfiguration(String repoDir) {
        log.info("Detecting project configuration for: {}", repoDir);

        ProjectConfiguration.ProjectConfigurationBuilder builder = ProjectConfiguration.builder();

        try {
            // Detect build tool
            String buildTool = detectBuildTool(repoDir);
            builder.buildTool(buildTool);
            log.info("Detected build tool: {}", buildTool);

            // Detect test framework
            String testFramework = detectTestFramework(repoDir, buildTool);
            builder.testFramework(testFramework);
            log.info("Detected test framework: {}", testFramework);

            // Detect Java version
            String javaVersion = detectJavaVersion(repoDir, buildTool);
            builder.javaVersion(javaVersion);
            log.info("Detected Java version: {}", javaVersion);

            // Detect dependencies and frameworks
            List<String> dependencies = detectDependencies(repoDir, buildTool);
            builder.dependencies(dependencies);
            log.info("Detected dependencies: {}", dependencies);

            // Detect existing test directories
            List<String> testDirectories = detectTestDirectories(repoDir);
            builder.testDirectories(testDirectories);
            log.info("Detected test directories: {}", testDirectories);

            // Detect Spring Boot specific configuration
            boolean isSpringBoot = dependencies.contains("spring-boot") ||
                    dependencies.contains("spring-boot-starter") ||
                    hasSpringBootAnnotations(repoDir);
            builder.isSpringBoot(isSpringBoot);
            log.info("Is Spring Boot project: {}", isSpringBoot);

            // Detect additional project characteristics
            ProjectCharacteristics characteristics = detectProjectCharacteristics(repoDir, buildTool);
            builder.projectCharacteristics(characteristics);

            // Generate build commands
            BuildCommands buildCommands = generateBuildCommands(buildTool, testFramework, isSpringBoot);
            builder.buildCommands(buildCommands);

            // Set detection timestamp
            builder.detectedAt(LocalDateTime.now());

            ProjectConfiguration config = builder.build();
            log.info("Successfully detected configuration: {}", config);
            return config;

        } catch (Exception e) {
            log.error("Failed to detect project configuration", e);
            return createDefaultConfiguration();
        }
    }

    /**
     * Detect build tool by checking for build files
     */
    private String detectBuildTool(String repoDir) {
        log.debug("Detecting build tool in: {}", repoDir);

        // Check for Maven
        if (Files.exists(Paths.get(repoDir, "pom.xml"))) {
            log.debug("Found pom.xml - Maven project detected");
            return "maven";
        }

        // Check for Gradle (Kotlin DSL first, then Groovy)
        if (Files.exists(Paths.get(repoDir, "build.gradle.kts"))) {
            log.debug("Found build.gradle.kts - Gradle (Kotlin DSL) project detected");
            return "gradle";
        }

        if (Files.exists(Paths.get(repoDir, "build.gradle"))) {
            log.debug("Found build.gradle - Gradle project detected");
            return "gradle";
        }

        // Check for SBT (Scala)
        if (Files.exists(Paths.get(repoDir, "build.sbt"))) {
            log.debug("Found build.sbt - SBT project detected");
            return "sbt";
        }

        // Check for Ant
        if (Files.exists(Paths.get(repoDir, "build.xml"))) {
            log.debug("Found build.xml - Ant project detected");
            return "ant";
        }

        // Check for Bazel
        if (Files.exists(Paths.get(repoDir, "BUILD")) || Files.exists(Paths.get(repoDir, "WORKSPACE"))) {
            log.debug("Found BUILD/WORKSPACE - Bazel project detected");
            return "bazel";
        }

        log.warn("No build tool detected, defaulting to Maven");
        return "maven";
    }

    /**
     * Detect test framework by analyzing build file dependencies
     */
    private String detectTestFramework(String repoDir, String buildTool) {
        log.debug("Detecting test framework for {} project", buildTool);

        try {
            String buildFileContent = readBuildFile(repoDir, buildTool);

            // Check for JUnit 5 (Jupiter)
            if (containsJUnit5Dependencies(buildFileContent)) {
                log.debug("JUnit 5 detected");
                return "junit5";
            }

            // Check for JUnit 4
            if (containsJUnit4Dependencies(buildFileContent)) {
                log.debug("JUnit 4 detected");
                return "junit4";
            }

            // Check for TestNG
            if (buildFileContent.toLowerCase().contains("testng")) {
                log.debug("TestNG detected");
                return "testng";
            }

            // Check for Spock (Groovy)
            if (buildFileContent.toLowerCase().contains("spock")) {
                log.debug("Spock detected");
                return "spock";
            }

            // Check for Spring Boot Test (implies JUnit)
            if (buildFileContent.contains("spring-boot-starter-test")) {
                log.debug("Spring Boot Test detected - checking for JUnit version");
                return detectSpringBootTestFramework(buildFileContent);
            }

            // AI-powered detection for complex cases
            return detectTestFrameworkWithAI(buildFileContent);

        } catch (Exception e) {
            log.warn("Failed to detect test framework, defaulting to JUnit 5", e);
            return "junit5";
        }
    }

    /**
     * Detect Java version from build configuration
     */
    private String detectJavaVersion(String repoDir, String buildTool) {
        log.debug("Detecting Java version for {} project", buildTool);

        try {
            String buildFileContent = readBuildFile(repoDir, buildTool);

            if ("maven".equals(buildTool)) {
                return extractJavaVersionFromMaven(buildFileContent);
            } else if ("gradle".equals(buildTool)) {
                return extractJavaVersionFromGradle(buildFileContent);
            } else if ("sbt".equals(buildTool)) {
                return extractJavaVersionFromSbt(buildFileContent);
            }

            // Fallback: check .java-version or .sdkmanrc files
            String versionFromFile = checkJavaVersionFiles(repoDir);
            if (versionFromFile != null) {
                return versionFromFile;
            }

        } catch (Exception e) {
            log.warn("Failed to detect Java version", e);
        }

        log.debug("Defaulting to Java 11");
        return "11";
    }

    /**
     * Detect project dependencies and frameworks
     */
    private List<String> detectDependencies(String repoDir, String buildTool) {
        log.debug("Detecting dependencies for {} project", buildTool);

        try {
            String buildFileContent = readBuildFile(repoDir, buildTool);
            List<String> dependencies = new ArrayList<>();

            // Spring Framework detection
            if (containsSpringDependencies(buildFileContent)) {
                dependencies.addAll(detectSpringDependencies(buildFileContent));
            }

            // Database dependencies
            if (buildFileContent.toLowerCase().contains("hibernate")) {
                dependencies.add("hibernate");
            }
            if (buildFileContent.toLowerCase().contains("jpa")) {
                dependencies.add("jpa");
            }
            if (buildFileContent.toLowerCase().contains("jdbc")) {
                dependencies.add("jdbc");
            }

            // Testing dependencies
            if (buildFileContent.toLowerCase().contains("mockito")) {
                dependencies.add("mockito");
            }
            if (buildFileContent.toLowerCase().contains("testcontainers")) {
                dependencies.add("testcontainers");
            }
            if (buildFileContent.toLowerCase().contains("wiremock")) {
                dependencies.add("wiremock");
            }

            // Utility dependencies
            if (buildFileContent.toLowerCase().contains("lombok")) {
                dependencies.add("lombok");
            }
            if (buildFileContent.toLowerCase().contains("jackson")) {
                dependencies.add("jackson");
            }

            // Web dependencies
            if (buildFileContent.toLowerCase().contains("spring-web") ||
                    buildFileContent.toLowerCase().contains("spring-boot-starter-web")) {
                dependencies.add("spring-web");
            }

            // Security dependencies
            if (buildFileContent.toLowerCase().contains("spring-security")) {
                dependencies.add("spring-security");
            }

            log.debug("Detected {} dependencies", dependencies.size());
            return dependencies;

        } catch (Exception e) {
            log.warn("Failed to detect dependencies", e);
            return new ArrayList<>();
        }
    }

    /**
     * Detect existing test directories
     */
    private List<String> detectTestDirectories(String repoDir) {
        log.debug("Detecting test directories in: {}", repoDir);

        List<String> testDirs = new ArrayList<>();

        // Standard Maven/Gradle test directories
        String[] commonTestDirs = {
                "src/test/java",
                "src/test/groovy",
                "src/test/kotlin",
                "src/integrationTest/java",
                "src/it/java",
                "src/functionalTest/java",
                "test",
                "tests",
                "testing"
        };

        for (String testDir : commonTestDirs) {
            Path testPath = Paths.get(repoDir, testDir);
            if (Files.exists(testPath) && Files.isDirectory(testPath)) {
                testDirs.add(testDir);
                log.debug("Found test directory: {}", testDir);
            }
        }

        // Look for custom test directories
        try {
            List<String> customTestDirs = findCustomTestDirectories(repoDir);
            testDirs.addAll(customTestDirs);
        } catch (Exception e) {
            log.warn("Failed to detect custom test directories", e);
        }

        log.debug("Total test directories found: {}", testDirs.size());
        return testDirs;
    }

    /**
     * Detect project characteristics
     */
    private ProjectCharacteristics detectProjectCharacteristics(String repoDir, String buildTool) {
        log.debug("Detecting project characteristics");

        ProjectCharacteristics.ProjectCharacteristicsBuilder builder = ProjectCharacteristics.builder();

        try {
            // Count Java files
            long javaFileCount = countJavaFiles(repoDir);
            builder.javaFileCount(javaFileCount);

            // Check for multi-module structure
            boolean isMultiModule = isMultiModuleProject(repoDir, buildTool);
            builder.multiModule(isMultiModule);

            // Detect architecture patterns
            List<String> architecturePatterns = detectArchitecturePatterns(repoDir);
            builder.architecturePatterns(architecturePatterns);

            // Check for documentation
            boolean hasDocumentation = hasDocumentation(repoDir);
            builder.hasDocumentation(hasDocumentation);

            // Check for CI/CD configuration
            List<String> cicdTools = detectCicdTools(repoDir);
            builder.cicdTools(cicdTools);

            return builder.build();

        } catch (Exception e) {
            log.warn("Failed to detect project characteristics", e);
            return ProjectCharacteristics.builder().build();
        }
    }

    /**
     * Generate build commands based on detected configuration
     */
    private BuildCommands generateBuildCommands(String buildTool, String testFramework, boolean isSpringBoot) {
        log.debug("Generating build commands for {}", buildTool);

        BuildCommands.BuildCommandsBuilder builder = BuildCommands.builder();

        if ("maven".equals(buildTool)) {
            builder.clean("mvn clean")
                    .compile("mvn compile")
                    .test("mvn test")
                    .install("mvn install");

            // Maven-specific coverage commands
            if (isSpringBoot) {
                builder.coverage("mvn clean jacoco:prepare-agent test jacoco:report");
            } else {
                builder.coverage("mvn jacoco:prepare-agent test jacoco:report");
            }

        } else if ("gradle".equals(buildTool)) {
            // Check for gradlew wrapper
            String gradleCommand = Files.exists(Paths.get("gradlew")) ? "./gradlew" : "gradle";

            builder.clean(gradleCommand + " clean")
                    .compile(gradleCommand + " compileJava")
                    .test(gradleCommand + " test")
                    .install(gradleCommand + " build");

            // Gradle-specific coverage commands
            builder.coverage(gradleCommand + " test jacocoTestReport");

        } else if ("sbt".equals(buildTool)) {
            builder.clean("sbt clean")
                    .compile("sbt compile")
                    .test("sbt test")
                    .coverage("sbt clean coverage test coverageReport")
                    .install("sbt package");
        }

        return builder.build();
    }

    // Helper methods for detailed detection

    private boolean containsJUnit5Dependencies(String buildContent) {
        String lowerContent = buildContent.toLowerCase();
        return lowerContent.contains("junit-jupiter") ||
                lowerContent.contains("org.junit.jupiter") ||
                lowerContent.contains("junit5") ||
                (lowerContent.contains("junit") && lowerContent.contains("5."));
    }

    private boolean containsJUnit4Dependencies(String buildContent) {
        String lowerContent = buildContent.toLowerCase();
        return lowerContent.contains("junit") &&
                !lowerContent.contains("junit-jupiter") &&
                !lowerContent.contains("junit5") &&
                (lowerContent.contains("4.") || !lowerContent.contains("5."));
    }

    private boolean containsSpringDependencies(String buildContent) {
        String lowerContent = buildContent.toLowerCase();
        return lowerContent.contains("spring-boot") ||
                lowerContent.contains("springframework") ||
                lowerContent.contains("spring-core") ||
                lowerContent.contains("spring-context");
    }

    private List<String> detectSpringDependencies(String buildContent) {
        List<String> springDeps = new ArrayList<>();
        String lowerContent = buildContent.toLowerCase();

        if (lowerContent.contains("spring-boot-starter")) {
            springDeps.add("spring-boot");
        }
        if (lowerContent.contains("spring-boot-starter-web")) {
            springDeps.add("spring-web");
        }
        if (lowerContent.contains("spring-boot-starter-data")) {
            springDeps.add("spring-data");
        }
        if (lowerContent.contains("spring-boot-starter-security")) {
            springDeps.add("spring-security");
        }
        if (lowerContent.contains("spring-boot-starter-test")) {
            springDeps.add("spring-test");
        }

        return springDeps;
    }

    private boolean hasSpringBootAnnotations(String repoDir) {
        try {
            // Look for @SpringBootApplication annotation in Java files
            try (Stream<Path> paths = Files.walk(Paths.get(repoDir))) {
                return paths
                        .filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".java"))
                        .limit(20) // Check first 20 Java files for performance
                        .anyMatch(this::containsSpringBootAnnotation);
            }
        } catch (Exception e) {
            log.debug("Failed to check for Spring Boot annotations", e);
            return false;
        }
    }

    private boolean containsSpringBootAnnotation(Path javaFile) {
        try {
            String content = Files.readString(javaFile);
            return content.contains("@SpringBootApplication") ||
                    content.contains("@EnableAutoConfiguration") ||
                    content.contains("org.springframework.boot");
        } catch (Exception e) {
            return false;
        }
    }

    private String detectSpringBootTestFramework(String buildContent) {
        // Spring Boot 2.2+ uses JUnit 5 by default
        if (buildContent.contains("spring-boot-starter-test")) {
            if (buildContent.contains("junit-vintage") || buildContent.contains("junit:junit")) {
                return "junit4";
            }
            return "junit5"; // Default for modern Spring Boot
        }
        return "junit5";
    }

    private String readBuildFile(String repoDir, String buildTool) throws IOException {
        String buildFile;

        switch (buildTool) {
            case "maven":
                buildFile = "pom.xml";
                break;
            case "gradle":
                // Try Kotlin DSL first, then Groovy
                if (Files.exists(Paths.get(repoDir, "build.gradle.kts"))) {
                    buildFile = "build.gradle.kts";
                } else {
                    buildFile = "build.gradle";
                }
                break;
            case "sbt":
                buildFile = "build.sbt";
                break;
            case "ant":
                buildFile = "build.xml";
                break;
            default:
                throw new IllegalArgumentException("Unsupported build tool: " + buildTool);
        }

        Path buildFilePath = Paths.get(repoDir, buildFile);
        if (!Files.exists(buildFilePath)) {
            throw new FileNotFoundException("Build file not found: " + buildFile);
        }

        return repositoryService.readFileContent(repoDir, buildFile);
    }

    private String extractJavaVersionFromMaven(String pomContent) {
        log.debug("Extracting Java version from Maven POM");

        // Try maven.compiler.source or maven.compiler.target
        Pattern pattern = Pattern.compile("<maven\\.compiler\\.(source|target)>(\\d+(?:\\.\\d+)?)</maven\\.compiler\\.(source|target)>");
        Matcher matcher = pattern.matcher(pomContent);
        if (matcher.find()) {
            String version = matcher.group(2);
            return normalizeJavaVersion(version);
        }

        // Try java.version property
        pattern = Pattern.compile("<java\\.version>(\\d+(?:\\.\\d+)?)</java\\.version>");
        matcher = pattern.matcher(pomContent);
        if (matcher.find()) {
            String version = matcher.group(1);
            return normalizeJavaVersion(version);
        }

        // Try release configuration
        pattern = Pattern.compile("<release>(\\d+)</release>");
        matcher = pattern.matcher(pomContent);
        if (matcher.find()) {
            return matcher.group(1);
        }

        // Check Spring Boot parent version for defaults
        pattern = Pattern.compile("<parent>.*?<groupId>org\\.springframework\\.boot</groupId>.*?<version>([^<]+)</version>.*?</parent>", Pattern.DOTALL);
        matcher = pattern.matcher(pomContent);
        if (matcher.find()) {
            String springBootVersion = matcher.group(1);
            return inferJavaVersionFromSpringBoot(springBootVersion);
        }

        return "11"; // Default
    }

    private String extractJavaVersionFromGradle(String gradleContent) {
        log.debug("Extracting Java version from Gradle build");

        // Try sourceCompatibility or targetCompatibility
        Pattern pattern = Pattern.compile("(source|target)Compatibility\\s*[=:]\\s*['\"]?(?:JavaVersion\\.VERSION_)?(\\d+)['\"]?");
        Matcher matcher = pattern.matcher(gradleContent);
        if (matcher.find()) {
            return matcher.group(2);
        }

        // Try toolchain configuration
        pattern = Pattern.compile("languageVersion\\s*[=:]\\s*JavaLanguageVersion\\.of\\((\\d+)\\)");
        matcher = pattern.matcher(gradleContent);
        if (matcher.find()) {
            return matcher.group(1);
        }

        // Try java block configuration
        pattern = Pattern.compile("java\\s*\\{[^}]*sourceCompatibility\\s*[=:]\\s*['\"]?(\\d+)['\"]?", Pattern.DOTALL);
        matcher = pattern.matcher(gradleContent);
        if (matcher.find()) {
            return matcher.group(1);
        }

        return "11"; // Default
    }

    private String extractJavaVersionFromSbt(String sbtContent) {
        log.debug("Extracting Java version from SBT build");

        // Try scalaVersion or javaVersion settings
        Pattern pattern = Pattern.compile("javaVersion\\s*:=\\s*\"(\\d+(?:\\.\\d+)?)\"");
        Matcher matcher = pattern.matcher(sbtContent);
        if (matcher.find()) {
            return normalizeJavaVersion(matcher.group(1));
        }

        return "11"; // Default
    }

    private String checkJavaVersionFiles(String repoDir) {
        // Check .java-version file
        Path javaVersionFile = Paths.get(repoDir, ".java-version");
        if (Files.exists(javaVersionFile)) {
            try {
                String version = Files.readString(javaVersionFile).trim();
                return normalizeJavaVersion(version);
            } catch (Exception e) {
                log.debug("Failed to read .java-version file", e);
            }
        }

        // Check .sdkmanrc file
        Path sdkmanrcFile = Paths.get(repoDir, ".sdkmanrc");
        if (Files.exists(sdkmanrcFile)) {
            try {
                String content = Files.readString(sdkmanrcFile);
                Pattern pattern = Pattern.compile("java=(\\d+(?:\\.\\d+\\.\\d+)?)");
                Matcher matcher = pattern.matcher(content);
                if (matcher.find()) {
                    return normalizeJavaVersion(matcher.group(1));
                }
            } catch (Exception e) {
                log.debug("Failed to read .sdkmanrc file", e);
            }
        }

        return null;
    }

    private String normalizeJavaVersion(String version) {
        if (version.contains(".")) {
            String majorVersion = version.split("\\.")[0];
            // Handle legacy 1.8 format
            if ("1".equals(majorVersion) && version.startsWith("1.")) {
                return version.split("\\.")[1];
            }
            return majorVersion;
        }
        return version;
    }

    private String inferJavaVersionFromSpringBoot(String springBootVersion) {
        if (springBootVersion.startsWith("3.")) {
            return "17"; // Spring Boot 3.x requires Java 17+
        } else if (springBootVersion.startsWith("2.")) {
            return "11"; // Spring Boot 2.x defaults to Java 11
        }
        return "11"; // Default
    }

    private List<String> findCustomTestDirectories(String repoDir) throws IOException {
        List<String> customTestDirs = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(Paths.get(repoDir), 3)) { // Limit depth
            paths.filter(Files::isDirectory)
                    .filter(path -> path.getFileName().toString().toLowerCase().contains("test"))
                    .filter(path -> !path.toString().contains("target"))
                    .filter(path -> !path.toString().contains("build"))
                    .map(path -> Paths.get(repoDir).relativize(path).toString())
                    .forEach(customTestDirs::add);
        }

        return customTestDirs;
    }

    private long countJavaFiles(String repoDir) throws IOException {
        try (Stream<Path> paths = Files.walk(Paths.get(repoDir))) {
            return paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !path.toString().contains("target"))
                    .filter(path -> !path.toString().contains("build"))
                    .count();
        }
    }

    private boolean isMultiModuleProject(String repoDir, String buildTool) {
        if ("maven".equals(buildTool)) {
            // Check for modules in pom.xml
            try {
                String pomContent = readBuildFile(repoDir, buildTool);
                return pomContent.contains("<modules>") || pomContent.contains("<module>");
            } catch (Exception e) {
                return false;
            }
        } else if ("gradle".equals(buildTool)) {
            // Check for settings.gradle with multiple projects
            try {
                Path settingsFile = Paths.get(repoDir, "settings.gradle");
                if (!Files.exists(settingsFile)) {
                    settingsFile = Paths.get(repoDir, "settings.gradle.kts");
                }
                if (Files.exists(settingsFile)) {
                    String content = Files.readString(settingsFile);
                    return content.contains("include") && content.contains(":");
                }
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    private List<String> detectArchitecturePatterns(String repoDir) {
        List<String> patterns = new ArrayList<>();

        try {
            // Check directory structure for common patterns
            if (Files.exists(Paths.get(repoDir, "src/main/java"))) {
                patterns.add("Maven Standard Directory Layout");
            }

            // Look for common package patterns
            try (Stream<Path> paths = Files.walk(Paths.get(repoDir, "src/main/java"), 4)) {
                Set<String> packageNames = paths
                        .filter(Files::isDirectory)
                        .map(path -> path.getFileName().toString().toLowerCase())
                        .collect(Collectors.toSet());

                if (packageNames.contains("controller")) patterns.add("MVC Pattern");
                if (packageNames.contains("service")) patterns.add("Service Layer");
                if (packageNames.contains("repository")) patterns.add("Repository Pattern");
                if (packageNames.contains("config")) patterns.add("Configuration Pattern");
                if (packageNames.contains("dto")) patterns.add("DTO Pattern");
                if (packageNames.contains("entity")) patterns.add("Entity Pattern");
                if (packageNames.contains("mapper")) patterns.add("Mapper Pattern");
            }

        } catch (Exception e) {
            log.debug("Failed to detect architecture patterns", e);
        }

        return patterns;
    }

    private boolean hasDocumentation(String repoDir) {
        String[] docFiles = {"README.md", "README.rst", "README.txt", "docs", "documentation"};

        for (String docFile : docFiles) {
            if (Files.exists(Paths.get(repoDir, docFile))) {
                return true;
            }
        }

        return false;
    }

    private List<String> detectCicdTools(String repoDir) {
        List<String> cicdTools = new ArrayList<>();

        // GitHub Actions
        if (Files.exists(Paths.get(repoDir, ".github/workflows"))) {
            cicdTools.add("GitHub Actions");
        }

        // Jenkins
        if (Files.exists(Paths.get(repoDir, "Jenkinsfile"))) {
            cicdTools.add("Jenkins");
        }

        // GitLab CI
        if (Files.exists(Paths.get(repoDir, ".gitlab-ci.yml"))) {
            cicdTools.add("GitLab CI");
        }

        // Travis CI
        if (Files.exists(Paths.get(repoDir, ".travis.yml"))) {
            cicdTools.add("Travis CI");
        }

        // CircleCI
        if (Files.exists(Paths.get(repoDir, ".circleci/config.yml"))) {
            cicdTools.add("CircleCI");
        }

        // Azure Pipelines
        if (Files.exists(Paths.get(repoDir, "azure-pipelines.yml"))) {
            cicdTools.add("Azure Pipelines");
        }

        // Bitbucket Pipelines
        if (Files.exists(Paths.get(repoDir, "bitbucket-pipelines.yml"))) {
            cicdTools.add("Bitbucket Pipelines");
        }

        return cicdTools;
    }

    private String detectTestFrameworkWithAI(String buildFileContent) {
        try {
            String prompt = String.format("""
                Analyze this build file content and determine the primary test framework being used.
                
                Build File Content:
                %s
                
                Please respond with JSON:
                {
                    "testFramework": "junit4|junit5|testng|spock",
                    "confidence": "HIGH|MEDIUM|LOW",
                    "reasoning": "Brief explanation of the detection",
                    "version": "Specific version if detectable"
                }
                """, buildFileContent.substring(0, Math.min(buildFileContent.length(), 2000)));

            String aiResponse = chatClient.prompt(prompt).call().content();
            JsonNode jsonResponse = new ObjectMapper().readTree(extractJsonFromResponse(aiResponse));

            String detectedFramework = jsonResponse.get("testFramework").asText();
            String confidence = jsonResponse.get("confidence").asText();

            log.info("AI detected test framework: {} (confidence: {})", detectedFramework, confidence);
            return detectedFramework;

        } catch (Exception e) {
            log.warn("AI detection failed, defaulting to JUnit 5", e);
            return "junit5";
        }
    }

    private ProjectConfiguration createDefaultConfiguration() {
        log.info("Creating default project configuration");

        return ProjectConfiguration.builder()
                .buildTool("maven")
                .testFramework("junit5")
                .javaVersion("11")
                .dependencies(List.of("spring-boot"))
                .testDirectories(List.of("src/test/java"))
                .isSpringBoot(true)
                .projectCharacteristics(ProjectCharacteristics.builder()
                        .javaFileCount(0L)
                        .multiModule(false)
                        .architecturePatterns(List.of("Unknown"))
                        .hasDocumentation(false)
                        .cicdTools(List.of())
                        .build())
                .buildCommands(BuildCommands.builder()
                        .clean("mvn clean")
                        .compile("mvn compile")
                        .test("mvn test")
                        .coverage("mvn jacoco:prepare-agent test jacoco:report")
                        .install("mvn install")
                        .build())
                .detectedAt(LocalDateTime.now())
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

    /**
     * Validate detected configuration and provide warnings
     */
    public ProjectConfigValidation validateConfiguration(ProjectConfiguration config) {
        log.debug("Validating project configuration");

        ProjectConfigValidation.ProjectConfigValidationBuilder builder = ProjectConfigValidation.builder();
        List<String> warnings = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();

        // Validate Java version compatibility
        if (config.isSpringBoot()) {
            String javaVersion = config.getJavaVersion();
            if (Integer.parseInt(javaVersion) < 8) {
                warnings.add("Java version " + javaVersion + " is not supported by Spring Boot");
            }
        }

        // Validate test framework and build tool compatibility
        if ("gradle".equals(config.getBuildTool()) && "junit4".equals(config.getTestFramework())) {
            recommendations.add("Consider upgrading to JUnit 5 for better Gradle integration");
        }

        // Check for missing test directories
        if (config.getTestDirectories().isEmpty()) {
            warnings.add("No test directories detected - tests will be created in standard locations");
        }

        // Validate dependencies consistency
        if (config.isSpringBoot() && !config.getDependencies().contains("spring-boot")) {
            warnings.add("Spring Boot project detected but spring-boot dependency not found");
        }

        return builder
                .valid(warnings.isEmpty())
                .warnings(warnings)
                .recommendations(recommendations)
                .validatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Update project configuration after changes
     */
    public ProjectConfiguration updateConfiguration(ProjectConfiguration existing, String repoDir) {
        log.info("Updating project configuration");

        try {
            // Re-detect in case build files changed
            ProjectConfiguration fresh = detectProjectConfiguration(repoDir);

            // Merge with existing to preserve any manual overrides
            return ProjectConfiguration.builder()
                    .buildTool(fresh.getBuildTool())
                    .testFramework(fresh.getTestFramework())
                    .javaVersion(fresh.getJavaVersion())
                    .dependencies(fresh.getDependencies())
                    .testDirectories(fresh.getTestDirectories())
                    .isSpringBoot(fresh.isSpringBoot())
                    .projectCharacteristics(fresh.getProjectCharacteristics())
                    .buildCommands(fresh.getBuildCommands())
                    .detectedAt(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Failed to update configuration, returning existing", e);
            return existing;
        }
    }
}

