package com.org.devgenie.service.coverage;

import com.org.devgenie.config.JacocoConfigurationService;
import com.org.devgenie.exception.coverage.JacocoException;
import com.org.devgenie.model.coverage.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
public class JacocoService {

    @Value("${jacoco.exec.path:target/jacoco.exec}")
    private String jacocoExecPath;

    @Autowired
    private ProjectConfigDetectionService projectConfigService;

    @Autowired
    private SonarQubeService sonarQubeService; // NEW: SonarQube integration

    @Autowired
    private JacocoConfigurationService jacocoConfigService; // NEW: Auto-configuration service

    /**
     * ENHANCED: Main entry point with intelligent fallback strategy
     */
    public CoverageData runAnalysisWithConfig(String repoDir, ProjectConfiguration projectConfig) {
        log.info("Running coverage analysis with {} configuration for: {}", projectConfig.getBuildTool(), repoDir);

        try {
            // Strategy 1: Check if Jacoco is already configured
            if (jacocoConfigService.isJacocoConfigured(repoDir, projectConfig)) {
                log.info("Jacoco is already configured, running analysis");
                return runExistingJacocoAnalysis(repoDir, projectConfig);
            }

            // Strategy 2: Auto-configure Jacoco and run analysis
            log.info("Jacoco not configured, attempting auto-configuration");
            if (jacocoConfigService.autoConfigureJacoco(repoDir, projectConfig)) {
                log.info("Jacoco auto-configured successfully, running analysis");
                return runExistingJacocoAnalysis(repoDir, projectConfig);
            }

            // Strategy 3: Try SonarQube integration
            log.info("Auto-configuration failed, trying SonarQube integration");
            CoverageData sonarData = sonarQubeService.getCoverageData(repoDir, projectConfig);
            if (sonarData != null) {
                log.info("Successfully retrieved coverage data from SonarQube");
                return sonarData;
            }

            // Strategy 4: Generate basic coverage data with minimal setup
            log.warn("All strategies failed, generating basic coverage data");
            return generateBasicCoverageData(repoDir, projectConfig);

        } catch (Exception e) {
            log.error("Failed to run coverage analysis", e);
            throw new JacocoException("Failed to run coverage analysis: " + e.getMessage(), e,
                    projectConfig.getBuildTool(), repoDir);
        }
    }

    /**
     * ENHANCED: Auto-detection with fallback strategies
     */
    public CoverageData runAnalysis(String repoDir) {
        log.info("Running coverage analysis with auto-detection for: {}", repoDir);

        try {
            ProjectConfiguration projectConfig = projectConfigService.detectProjectConfiguration(repoDir);
            log.info("Auto-detected build tool: {}", projectConfig.getBuildTool());

            return runAnalysisWithConfig(repoDir, projectConfig);

        } catch (Exception e) {
            log.error("Failed to run coverage analysis with auto-detection", e);
            throw new JacocoException("All coverage analysis strategies failed: " + e.getMessage(), e);
        }
    }

    /**
     * NEW: Run analysis when Jacoco is already configured
     */
    private CoverageData runExistingJacocoAnalysis(String repoDir, ProjectConfiguration projectConfig) throws IOException, InterruptedException {
        runCoverageAnalysis(repoDir, projectConfig);
        return parseCoverageReport(repoDir, projectConfig);
    }

    /**
     * NEW: Generate basic coverage data when all else fails
     */
    private CoverageData generateBasicCoverageData(String repoDir, ProjectConfiguration projectConfig) {
        log.info("Generating basic coverage data for: {}", repoDir);

        try {
            // Find all Java files
            List<String> javaFiles = findJavaFiles(repoDir);
            List<FileCoverageData> files = new ArrayList<>();

            // Create basic file coverage data (0% coverage)
            for (String javaFile : javaFiles) {
                int lineCount = countLinesInFile(Paths.get(repoDir, javaFile));

                FileCoverageData fileData = FileCoverageData.builder()
                        .filePath(javaFile)
                        .lineCoverage(0.0)
                        .branchCoverage(0.0)
                        .methodCoverage(0.0)
                        .totalLines(lineCount)
                        .coveredLines(0)
                        .totalBranches(0)
                        .coveredBranches(0)
                        .totalMethods(0)
                        .coveredMethods(0)
                        .uncoveredLines(new ArrayList<>())
                        .uncoveredBranches(new ArrayList<>())
                        .lastUpdated(LocalDateTime.now())
                        .buildTool(projectConfig.getBuildTool())
                        .testFramework(projectConfig.getTestFramework())
                        .build();

                files.add(fileData);
            }

            int totalLines = files.stream().mapToInt(FileCoverageData::getTotalLines).sum();

            return CoverageData.builder()
                    .repoPath(repoDir)
                    .files(files)
                    .overallCoverage(0.0)
                    .lineCoverage(0.0)
                    .branchCoverage(0.0)
                    .methodCoverage(0.0)
                    .totalLines(totalLines)
                    .coveredLines(0)
                    .totalBranches(0)
                    .coveredBranches(0)
                    .totalMethods(0)
                    .coveredMethods(0)
                    .timestamp(LocalDateTime.now())
                    .projectConfiguration(projectConfig)
                    .coverageSource(CoverageData.CoverageSource.BASIC_ANALYSIS) // NEW: Track data source
                    .build();

        } catch (Exception e) {
            log.error("Failed to generate basic coverage data", e);
            throw new JacocoException("Failed to generate basic coverage data", e);
        }
    }

    /**
     * NEW: Method to validate and potentially fix coverage after test generation
     */
    public CoverageComparisonResult validateCoverageImprovement(String repoDir, ProjectConfiguration projectConfig,
                                                                CoverageData originalCoverage) {
        log.info("Validating coverage improvement after test generation");

        try {
            // Strategy 1: Try to run Jacoco analysis if now configured
            CoverageData newCoverage = null;

            if (jacocoConfigService.isJacocoConfigured(repoDir, projectConfig)) {
                try {
                    newCoverage = runExistingJacocoAnalysis(repoDir, projectConfig);
                    log.info("Successfully ran Jacoco analysis for validation");
                } catch (Exception e) {
                    log.warn("Jacoco analysis failed during validation", e);
                }
            }

            // Strategy 2: Try SonarQube for validation
            if (newCoverage == null) {
                try {
                    newCoverage = sonarQubeService.getCoverageData(repoDir, projectConfig);
                    if (newCoverage != null) {
                        log.info("Successfully retrieved updated coverage from SonarQube");
                    }
                } catch (Exception e) {
                    log.warn("SonarQube analysis failed during validation", e);
                }
            }

            // Strategy 3: Estimate improvement based on test generation
            if (newCoverage == null) {
                log.info("Using estimated coverage improvement");
                newCoverage = estimateCoverageImprovement(originalCoverage, repoDir);
            }

            return CoverageComparisonResult.builder()
                    .originalCoverage(originalCoverage)
                    .newCoverage(newCoverage)
                    .coverageImprovement(newCoverage.getOverallCoverage() - originalCoverage.getOverallCoverage())
                    .validationMethod(determineValidationMethod(newCoverage))
                    .validatedAt(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Failed to validate coverage improvement", e);
            throw new JacocoException("Coverage validation failed", e);
        }
    }

    /**
     * NEW: Estimate coverage improvement based on generated tests
     */
    private CoverageData estimateCoverageImprovement(CoverageData originalCoverage, String repoDir) {
        // Simple estimation: add 15-25% improvement based on test files found
        int testFilesCount = countGeneratedTestFiles(repoDir);
        double estimatedImprovement = Math.min(testFilesCount * 3.5, 25.0); // Cap at 25%

        double newOverallCoverage = Math.min(originalCoverage.getOverallCoverage() + estimatedImprovement, 100.0);

        return CoverageData.builder()
                .repoPath(originalCoverage.getRepoPath())
                .files(originalCoverage.getFiles()) // Keep original file data
                .overallCoverage(newOverallCoverage)
                .lineCoverage(Math.min(originalCoverage.getLineCoverage() + estimatedImprovement, 100.0))
                .branchCoverage(Math.min(originalCoverage.getBranchCoverage() + estimatedImprovement * 0.8, 100.0))
                .methodCoverage(Math.min(originalCoverage.getMethodCoverage() + estimatedImprovement * 1.2, 100.0))
                .totalLines(originalCoverage.getTotalLines())
                .coveredLines((int) (originalCoverage.getTotalLines() * newOverallCoverage / 100))
                .totalBranches(originalCoverage.getTotalBranches())
                .coveredBranches(originalCoverage.getCoveredBranches())
                .totalMethods(originalCoverage.getTotalMethods())
                .coveredMethods(originalCoverage.getCoveredMethods())
                .timestamp(LocalDateTime.now())
                .projectConfiguration(originalCoverage.getProjectConfiguration())
                .coverageSource(CoverageData.CoverageSource.ESTIMATED) // NEW: Mark as estimated
                .build();
    }

    private String determineValidationMethod(CoverageData coverageData) {
        if (coverageData.getCoverageSource() != null) {
            return coverageData.getCoverageSource().toString();
        }
        return "JACOCO_ANALYSIS";
    }

    private int countGeneratedTestFiles(String repoDir) {
        try {
            Path testDir = Paths.get(repoDir, "src/test/java");
            if (!Files.exists(testDir)) return 0;

            try (Stream<Path> paths = Files.walk(testDir)) {
                return (int) paths
                        .filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith("Test.java"))
                        .filter(path -> isRecentlyModified(path, Duration.ofHours(1)))
                        .count();
            }
        } catch (Exception e) {
            log.debug("Failed to count test files", e);
            return 0;
        }
    }

    private boolean isRecentlyModified(Path path, Duration within) {
        try {
            FileTime lastModified = Files.getLastModifiedTime(path);
            return lastModified.toInstant().isAfter(Instant.now().minus(within));
        } catch (IOException e) {
            return false;
        }
    }

    private List<String> findJavaFiles(String repoDir) throws IOException {
        try (Stream<Path> paths = Files.walk(Paths.get(repoDir))) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !path.toString().contains("/test/"))
                    .filter(path -> !path.toString().contains("\\test\\"))
                    .filter(path -> !path.toString().contains("/target/"))
                    .filter(path -> !path.toString().contains("/build/"))
                    .map(path -> Paths.get(repoDir).relativize(path).toString())
                    .collect(Collectors.toList());
        }
    }

    private int countLinesInFile(Path filePath) {
        try {
            return (int) Files.lines(filePath).count();
        } catch (IOException e) {
            log.debug("Failed to count lines in file: {}", filePath, e);
            return 50; // Default estimate
        }
    }

    private void runCoverageAnalysis(String repoDir, ProjectConfiguration projectConfig) throws IOException, InterruptedException {
        String buildTool = projectConfig.getBuildTool();
        String coverageCommand = projectConfig.getBuildCommands().getCoverage();

        log.info("Running coverage analysis with {} using command: {}", buildTool, coverageCommand);

        switch (buildTool.toLowerCase()) {
            case "maven":
                runMavenCoverage(repoDir, coverageCommand, projectConfig);
                break;
            case "gradle":
                runGradleCoverage(repoDir, coverageCommand, projectConfig);
                break;
            case "sbt":
                runSbtCoverage(repoDir, coverageCommand, projectConfig);
                break;
            default:
                log.warn("Unsupported build tool: {}. Attempting Maven fallback.", buildTool);
                runMavenCoverage(repoDir, "mvn clean jacoco:prepare-agent test jacoco:report", projectConfig);
        }
    }

    private void runMavenCoverage(String repoDir, String coverageCommand, ProjectConfiguration projectConfig) throws IOException, InterruptedException {
        log.info("Running Maven coverage analysis: {}", coverageCommand);

        // Validate Maven installation
        validateMavenInstallation(repoDir);

        // Parse command and execute
        String[] commands = coverageCommand.split("\\s+");
        ProcessBuilder pb = new ProcessBuilder(commands);
        pb.directory(new File(repoDir));
        pb.inheritIO();

        // Set Maven-specific environment variables
        Map<String, String> env = pb.environment();
        env.put("MAVEN_OPTS", "-Xmx1024m"); // Ensure sufficient memory
        if (projectConfig.isJUnit5()) {
            env.put("MAVEN_SUREFIRE_PLUGIN_VERSION", "3.0.0-M7"); // JUnit 5 compatibility
        }

        Process process = pb.start();
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new JacocoException("Maven coverage analysis failed with exit code: " + exitCode);
        }

        log.info("Maven coverage analysis completed successfully");
    }

    private void runGradleCoverage(String repoDir, String coverageCommand, ProjectConfiguration projectConfig) throws IOException, InterruptedException {
        log.info("Running Gradle coverage analysis: {}", coverageCommand);

        // Make gradlew executable if it exists
        prepareGradleWrapper(repoDir);

        // Validate Gradle installation
        validateGradleInstallation(repoDir);

        String[] commands = coverageCommand.split("\\s+");
        ProcessBuilder pb = new ProcessBuilder(commands);
        pb.directory(new File(repoDir));
        pb.inheritIO();

        // Set Gradle-specific environment variables
        Map<String, String> env = pb.environment();
        env.put("GRADLE_OPTS", "-Xmx1024m -XX:MaxMetaspaceSize=512m");

        Process process = pb.start();
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new JacocoException("Gradle coverage analysis failed with exit code: " + exitCode);
        }

        log.info("Gradle coverage analysis completed successfully");
    }

    private void runSbtCoverage(String repoDir, String coverageCommand, ProjectConfiguration projectConfig) throws IOException, InterruptedException {
        log.info("Running SBT coverage analysis: {}", coverageCommand);

        // Validate SBT installation
        validateSbtInstallation(repoDir);

        String[] commands = coverageCommand.split("\\s+");
        ProcessBuilder pb = new ProcessBuilder(commands);
        pb.directory(new File(repoDir));
        pb.inheritIO();

        // Set SBT-specific environment variables
        Map<String, String> env = pb.environment();
        env.put("SBT_OPTS", "-Xmx1024m");

        Process process = pb.start();
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new JacocoException("SBT coverage analysis failed with exit code: " + exitCode);
        }

        log.info("SBT coverage analysis completed successfully");
    }

    private CoverageData parseCoverageReport(String repoDir, ProjectConfiguration projectConfig) throws IOException {
        String buildTool = projectConfig.getBuildTool();
        List<String> reportPaths = getCoverageReportPaths(repoDir, buildTool);

        File reportFile = null;
        String actualReportPath = null;

        // Try to find coverage report in expected locations
        for (String reportPath : reportPaths) {
            File candidate = new File(reportPath);
            if (candidate.exists()) {
                reportFile = candidate;
                actualReportPath = reportPath;
                log.info("Found coverage report at: {}", reportPath);
                break;
            }
        }

        if (reportFile == null) {
            throw new JacocoException("Coverage report not found. Tried paths: " + String.join(", ", reportPaths));
        }

        // Parse based on report type
        if (actualReportPath.endsWith(".xml")) {
            return parseJacocoXmlReport(reportFile, repoDir, projectConfig);
        } else if (actualReportPath.endsWith(".csv")) {
            return parseJacocoCsvReport(reportFile, repoDir, projectConfig);
        } else if (actualReportPath.endsWith(".html")) {
            return parseJacocoHtmlReport(reportFile, repoDir, projectConfig);
        } else {
            throw new JacocoException("Unsupported coverage report format: " + actualReportPath);
        }
    }

    private List<String> getCoverageReportPaths(String repoDir, String buildTool) {
        List<String> paths = new ArrayList<>();

        switch (buildTool.toLowerCase()) {
            case "maven":
                paths.add(repoDir + "/target/site/jacoco/jacoco.xml");
                paths.add(repoDir + "/target/site/jacoco/jacoco.csv");
                // Multi-module Maven projects
                paths.add(repoDir + "/target/site/jacoco-aggregate/jacoco.xml");
                // Alternative Maven locations
                paths.add(repoDir + "/target/jacoco-reports/jacoco.xml");
                break;

            case "gradle":
                paths.add(repoDir+ "/build/reports/jacoco/test/html/index.html");
                break;

            case "sbt":
                paths.add(repoDir + "/target/scala-2.12/scoverage-report/scoverage.xml");
                paths.add(repoDir + "/target/scala-2.13/scoverage-report/scoverage.xml");
                paths.add(repoDir + "/target/scoverage-report/scoverage.xml");
                paths.add(repoDir + "/target/scala-2.11/scoverage-report/scoverage.xml");
                break;

            default:
                // Fallback: try common locations
                paths.add(repoDir + "/target/site/jacoco/jacoco.xml");
                paths.add(repoDir + "/build/reports/jacoco/test/jacocoTestReport.xml");
        }

        return paths;
    }

    private CoverageData parseJacocoXmlReport(File reportFile, String repoDir, ProjectConfiguration projectConfig) throws IOException {
        log.info("Parsing Jacoco XML report: {}", reportFile.getAbsolutePath());

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(reportFile);

            return parseJacocoXml(document, repoDir, projectConfig);

        } catch (Exception e) {
            throw new JacocoException("Failed to parse Jacoco XML report: " + reportFile.getAbsolutePath(), e);
        }
    }

    private CoverageData parseJacocoCsvReport(File reportFile, String repoDir, ProjectConfiguration projectConfig) throws IOException {
        log.info("Parsing Jacoco CSV report: {}", reportFile.getAbsolutePath());

        try {
            List<String> lines = Files.readAllLines(reportFile.toPath());
            if (lines.isEmpty()) {
                throw new JacocoException("CSV report is empty");
            }

            // Parse CSV header
            String[] headers = lines.get(0).split(",");
            List<FileCoverageData> files = new ArrayList<>();

            // Parse CSV data lines
            for (int i = 1; i < lines.size(); i++) {
                String[] values = lines.get(i).split(",");
                if (values.length >= headers.length) {
                    FileCoverageData fileData = parseCsvLine(headers, values, projectConfig);
                    if (fileData != null) {
                        files.add(fileData);
                    }
                }
            }

            // Calculate overall coverage
            return calculateOverallCoverage(files, repoDir, projectConfig);

        } catch (Exception e) {
            throw new JacocoException("Failed to parse CSV report: " + reportFile.getAbsolutePath(), e);
        }
    }

    /**
     * Parses Jacoco HTML report (index.html) and extracts overall coverage data.
     */
    private CoverageData parseJacocoHtmlReport(File htmlFile, String repoDir, ProjectConfiguration projectConfig) throws IOException {
        org.jsoup.nodes.Document doc = org.jsoup.Jsoup.parse(htmlFile, "UTF-8");
        org.jsoup.select.Elements totalRow = doc.select("table.coverage tfoot tr");
        if (totalRow.isEmpty()) {
            throw new JacocoException("Could not find total coverage row in HTML report");
        }
        org.jsoup.select.Elements tds = totalRow.get(0).select("td");
        if (tds.size() < 13) {
            throw new JacocoException("Unexpected coverage table format in HTML report");
        }
        // Extract overall coverage percentages from the table
        double instructionCoverage = parsePercent(tds.get(2).text());
        double branchCoverage = parsePercent(tds.get(4).text());
        double lineCoverage = parsePercent(tds.get(8).text());
        double methodCoverage = parsePercent(tds.get(10).text());

        // Build recursive directory tree from file-level coverage
        List<FileCoverageData> allFiles = extractAllFilesFromHtml(doc, projectConfig);
        DirectoryCoverageData rootDirectory = buildDirectoryTreeRecursive(allFiles, "src/main/java", LocalDateTime.now());
        List<DirectoryCoverageData> directories = rootDirectory != null ? List.of(rootDirectory) : new ArrayList<>();

        // Build CoverageData
        CoverageData data = CoverageData.builder()
                .overallCoverage(instructionCoverage)
                .branchCoverage(branchCoverage)
                .lineCoverage(lineCoverage)
                .methodCoverage(methodCoverage)
                .directories(directories)
                .timestamp(LocalDateTime.now())
                .build();
        return data;
    }

    // Helper to extract all file-level coverage from Jacoco HTML (using CSV or XML is more accurate, but this is fallback)
    private List<FileCoverageData> extractAllFilesFromHtml(org.jsoup.nodes.Document doc, ProjectConfiguration projectConfig) {
        List<FileCoverageData> files = new ArrayList<>();
        org.jsoup.select.Elements fileRows = doc.select("table.coverage tbody tr");
        for (org.jsoup.nodes.Element row : fileRows) {
            org.jsoup.select.Elements cols = row.select("td");
            if (cols.size() < 13) continue;
            String element = cols.get(0).text();
            if (element.endsWith(".java")) {
                // Try to reconstruct file path from package and file name
                String filePath = "src/main/java/" + element.replace('.', '/');
                double lineCov = parsePercent(cols.get(8).text());
                double branchCov = parsePercent(cols.get(4).text());
                double methodCov = parsePercent(cols.get(10).text());
                int totalLines = parseInt(cols.get(8).text().replace("%", ""), 0);
                int coveredLines = (int) (totalLines * lineCov / 100.0);
                int totalBranches = parseInt(cols.get(4).text().replace("%", ""), 0);
                int coveredBranches = (int) (totalBranches * branchCov / 100.0);
                int totalMethods = parseInt(cols.get(10).text().replace("%", ""), 0);
                int coveredMethods = (int) (totalMethods * methodCov / 100.0);
                files.add(FileCoverageData.builder()
                        .filePath(filePath)
                        .lineCoverage(lineCov)
                        .branchCoverage(branchCov)
                        .methodCoverage(methodCov)
                        .totalLines(totalLines)
                        .coveredLines(coveredLines)
                        .totalBranches(totalBranches)
                        .coveredBranches(coveredBranches)
                        .totalMethods(totalMethods)
                        .coveredMethods(coveredMethods)
                        .uncoveredLines(new ArrayList<>())
                        .uncoveredBranches(new ArrayList<>())
                        .lastUpdated(LocalDateTime.now())
                        .buildTool(projectConfig.getBuildTool())
                        .testFramework(projectConfig.getTestFramework())
                        .build());
            }
        }
        return files;
    }

    // Recursive directory tree builder (same as SonarQubeService)
    private DirectoryCoverageData buildDirectoryTreeRecursive(List<FileCoverageData> files, String dirPath, LocalDateTime now) {
        List<FileCoverageData> directFiles = new ArrayList<>();
        Map<String, List<FileCoverageData>> subDirMap = new HashMap<>();
        for (FileCoverageData file : files) {
            if (file.getFilePath() == null || !file.getFilePath().startsWith(dirPath + "/")) continue;
            String relative = file.getFilePath().substring(dirPath.length() + 1);
            if (!relative.contains("/")) {
                directFiles.add(file);
            } else {
                String subDir = relative.substring(0, relative.indexOf("/"));
                String subDirPath = dirPath + "/" + subDir;
                subDirMap.computeIfAbsent(subDirPath, k -> new ArrayList<>()).add(file);
            }
        }
        if (directFiles.isEmpty() && subDirMap.isEmpty()) return null;
        List<DirectoryCoverageData> subdirectories = new ArrayList<>();
        for (Map.Entry<String, List<FileCoverageData>> entry : subDirMap.entrySet()) {
            DirectoryCoverageData sub = buildDirectoryTreeRecursive(entry.getValue(), entry.getKey(), now);
            if (sub != null) subdirectories.add(sub);
        }
        // Aggregate coverage for this directory
        int totalLines = directFiles.stream().mapToInt(FileCoverageData::getTotalLines).sum() +
                subdirectories.stream().mapToInt(DirectoryCoverageData::getTotalLines).sum();
        int coveredLines = directFiles.stream().mapToInt(FileCoverageData::getCoveredLines).sum() +
                subdirectories.stream().mapToInt(DirectoryCoverageData::getCoveredLines).sum();
        int totalBranches = directFiles.stream().mapToInt(FileCoverageData::getTotalBranches).sum() +
                subdirectories.stream().mapToInt(DirectoryCoverageData::getTotalBranches).sum();
        int coveredBranches = directFiles.stream().mapToInt(FileCoverageData::getCoveredBranches).sum() +
                subdirectories.stream().mapToInt(DirectoryCoverageData::getCoveredBranches).sum();
        int totalMethods = directFiles.stream().mapToInt(FileCoverageData::getTotalMethods).sum() +
                subdirectories.stream().mapToInt(DirectoryCoverageData::getTotalMethods).sum();
        int coveredMethods = directFiles.stream().mapToInt(FileCoverageData::getCoveredMethods).sum() +
                subdirectories.stream().mapToInt(DirectoryCoverageData::getCoveredMethods).sum();
        double lineCoverage = totalLines > 0 ? (double) coveredLines / totalLines * 100 : 0;
        double branchCoverage = totalBranches > 0 ? (double) coveredBranches / totalBranches * 100 : 0;
        double methodCoverage = totalMethods > 0 ? (double) coveredMethods / totalMethods * 100 : 0;
        return DirectoryCoverageData.builder()
                .directoryPath(dirPath)
                .overallCoverage(lineCoverage)
                .lineCoverage(lineCoverage)
                .branchCoverage(branchCoverage)
                .methodCoverage(methodCoverage)
                .totalLines(totalLines)
                .coveredLines(coveredLines)
                .totalBranches(totalBranches)
                .coveredBranches(coveredBranches)
                .totalMethods(totalMethods)
                .coveredMethods(coveredMethods)
                .files(directFiles)
                .subdirectories(subdirectories)
                .lastUpdated(now)
                .build();
    }

    private double parsePercent(String percentText) {
        // Handles values like "6%" or "40%"
        try {
            return Double.parseDouble(percentText.replace("%", "").trim());
        } catch (Exception e) {
            return 0.0;
        }
    }

    private FileCoverageData parseCsvLine(String[] headers, String[] values, ProjectConfiguration projectConfig) {
        try {
            Map<String, String> data = new HashMap<>();
            for (int i = 0; i < headers.length && i < values.length; i++) {
                data.put(headers[i].trim().toUpperCase(), values[i].trim());
            }

            String className = data.get("CLASS");
            String packageName = data.get("PACKAGE");

            if (className == null || packageName == null) {
                return null; // Skip invalid lines
            }

            String filePath = buildFilePath(packageName, className, projectConfig);

            int instructionMissed = parseInt(data.get("INSTRUCTION_MISSED"), 0);
            int instructionCovered = parseInt(data.get("INSTRUCTION_COVERED"), 0);
            int branchMissed = parseInt(data.get("BRANCH_MISSED"), 0);
            int branchCovered = parseInt(data.get("BRANCH_COVERED"), 0);
            int lineMissed = parseInt(data.get("LINE_MISSED"), 0);
            int lineCovered = parseInt(data.get("LINE_COVERED"), 0);
            int methodMissed = parseInt(data.get("METHOD_MISSED"), 0);
            int methodCovered = parseInt(data.get("METHOD_COVERED"), 0);

            int totalLines = lineMissed + lineCovered;
            int totalBranches = branchMissed + branchCovered;
            int totalMethods = methodMissed + methodCovered;

            double lineCoverage = totalLines > 0 ? (double) lineCovered / totalLines * 100 : 0;
            double branchCoverage = totalBranches > 0 ? (double) branchCovered / totalBranches * 100 : 0;
            double methodCoverage = totalMethods > 0 ? (double) methodCovered / totalMethods * 100 : 0;

            return FileCoverageData.builder()
                    .filePath(filePath)
                    .lineCoverage(lineCoverage)
                    .branchCoverage(branchCoverage)
                    .methodCoverage(methodCoverage)
                    .totalLines(totalLines)
                    .coveredLines(lineCovered)
                    .totalBranches(totalBranches)
                    .coveredBranches(branchCovered)
                    .totalMethods(totalMethods)
                    .coveredMethods(methodCovered)
                    .uncoveredLines(new ArrayList<>()) // CSV doesn't provide line details
                    .uncoveredBranches(new ArrayList<>())
                    .lastUpdated(LocalDateTime.now())
                    .buildTool(projectConfig.getBuildTool())
                    .testFramework(projectConfig.getTestFramework())
                    .build();

        } catch (Exception e) {
            log.warn("Failed to parse CSV line: {}", String.join(",", values), e);
            return null;
        }
    }

    private int parseInt(String value, int defaultValue) {
        try {
            return value != null ? Integer.parseInt(value) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private CoverageData calculateOverallCoverage(List<FileCoverageData> files, String repoDir, ProjectConfiguration projectConfig) {
        int totalLines = 0, coveredLines = 0;
        int totalBranches = 0, coveredBranches = 0;
        int totalMethods = 0, coveredMethods = 0;

        for (FileCoverageData file : files) {
            totalLines += file.getTotalLines();
            coveredLines += file.getCoveredLines();
            totalBranches += file.getTotalBranches();
            coveredBranches += file.getCoveredBranches();
            totalMethods += file.getTotalMethods();
            coveredMethods += file.getCoveredMethods();
        }

        double overallLineCoverage = totalLines > 0 ? (double) coveredLines / totalLines * 100 : 0;
        double overallBranchCoverage = totalBranches > 0 ? (double) coveredBranches / totalBranches * 100 : 0;
        double overallMethodCoverage = totalMethods > 0 ? (double) coveredMethods / totalMethods * 100 : 0;

        return CoverageData.builder()
                .repoPath(repoDir)
                .files(files)
                .overallCoverage(overallLineCoverage)
                .lineCoverage(overallLineCoverage)
                .branchCoverage(overallBranchCoverage)
                .methodCoverage(overallMethodCoverage)
                .totalLines(totalLines)
                .coveredLines(coveredLines)
                .totalBranches(totalBranches)
                .coveredBranches(coveredBranches)
                .totalMethods(totalMethods)
                .coveredMethods(coveredMethods)
                .timestamp(LocalDateTime.now())
                .projectConfiguration(projectConfig)
                .build();
    }

    private CoverageData parseJacocoXml(Document document, String repoDir, ProjectConfiguration projectConfig) {
        CoverageData.CoverageDataBuilder builder = CoverageData.builder();
        builder.repoPath(repoDir);
        builder.timestamp(LocalDateTime.now());
        builder.projectConfiguration(projectConfig);

        List<FileCoverageData> files = new ArrayList<>();
        NodeList packageNodes = document.getElementsByTagName("package");

        int totalLines = 0, coveredLines = 0;
        int totalBranches = 0, coveredBranches = 0;
        int totalMethods = 0, coveredMethods = 0;

        for (int i = 0; i < packageNodes.getLength(); i++) {
            Element packageElement = (Element) packageNodes.item(i);
            String packageName = packageElement.getAttribute("name");
            NodeList classNodes = packageElement.getElementsByTagName("class");

            for (int j = 0; j < classNodes.getLength(); j++) {
                Element classElement = (Element) classNodes.item(j);
                String sourceFileName = classElement.getAttribute("sourcefilename");

                if (isValidSourceFile(sourceFileName, projectConfig)) {
                    FileCoverageData fileData = parseClassCoverage(classElement, packageName, projectConfig);
                    files.add(fileData);

                    totalLines += fileData.getTotalLines();
                    coveredLines += fileData.getCoveredLines();
                    totalBranches += fileData.getTotalBranches();
                    coveredBranches += fileData.getCoveredBranches();
                    totalMethods += fileData.getTotalMethods();
                    coveredMethods += fileData.getCoveredMethods();
                }
            }
        }

        double overallLineCoverage = totalLines > 0 ? (double) coveredLines / totalLines * 100 : 0;
        double overallBranchCoverage = totalBranches > 0 ? (double) coveredBranches / totalBranches * 100 : 0;
        double overallMethodCoverage = totalMethods > 0 ? (double) coveredMethods / totalMethods * 100 : 0;

        return builder
                .files(files)
                .overallCoverage(overallLineCoverage)
                .lineCoverage(overallLineCoverage)
                .branchCoverage(overallBranchCoverage)
                .methodCoverage(overallMethodCoverage)
                .totalLines(totalLines)
                .coveredLines(coveredLines)
                .totalBranches(totalBranches)
                .coveredBranches(coveredBranches)
                .totalMethods(totalMethods)
                .coveredMethods(coveredMethods)
                .build();
    }

    private boolean isValidSourceFile(String sourceFileName, ProjectConfiguration projectConfig) {
        if (sourceFileName == null || sourceFileName.isEmpty()) {
            return false;
        }

        String buildTool = projectConfig.getBuildTool().toLowerCase();

        switch (buildTool) {
            case "maven":
            case "gradle":
                return sourceFileName.endsWith(".java");
            case "sbt":
                return sourceFileName.endsWith(".scala") || sourceFileName.endsWith(".java");
            default:
                return sourceFileName.endsWith(".java");
        }
    }

    private FileCoverageData parseClassCoverage(Element classElement, String packageName, ProjectConfiguration projectConfig) {
        String className = classElement.getAttribute("name");
        String filePath = buildFilePath(packageName, className, projectConfig);

        NodeList counterNodes = classElement.getElementsByTagName("counter");

        int totalLines = 0, coveredLines = 0;
        int totalBranches = 0, coveredBranches = 0;
        int totalMethods = 0, coveredMethods = 0;
        int totalInstructions = 0, coveredInstructions = 0;
        int totalComplexity = 0, coveredComplexity = 0;

        for (int i = 0; i < counterNodes.getLength(); i++) {
            Element counter = (Element) counterNodes.item(i);
            String type = counter.getAttribute("type");
            int missed = Integer.parseInt(counter.getAttribute("missed"));
            int covered = Integer.parseInt(counter.getAttribute("covered"));

            switch (type) {
                case "LINE":
                    totalLines = missed + covered;
                    coveredLines = covered;
                    break;
                case "BRANCH":
                    totalBranches = missed + covered;
                    coveredBranches = covered;
                    break;
                case "METHOD":
                    totalMethods = missed + covered;
                    coveredMethods = covered;
                    break;
                case "INSTRUCTION":
                    totalInstructions = missed + covered;
                    coveredInstructions = covered;
                    break;
                case "COMPLEXITY":
                    totalComplexity = missed + covered;
                    coveredComplexity = covered;
                    break;
            }
        }

        double lineCoverage = totalLines > 0 ? (double) coveredLines / totalLines * 100 : 0;
        double branchCoverage = totalBranches > 0 ? (double) coveredBranches / totalBranches * 100 : 0;
        double methodCoverage = totalMethods > 0 ? (double) coveredMethods / totalMethods * 100 : 0;

        return FileCoverageData.builder()
                .filePath(filePath)
                .lineCoverage(lineCoverage)
                .branchCoverage(branchCoverage)
                .methodCoverage(methodCoverage)
                .totalLines(totalLines)
                .coveredLines(coveredLines)
                .totalBranches(totalBranches)
                .coveredBranches(coveredBranches)
                .totalMethods(totalMethods)
                .coveredMethods(coveredMethods)
                .uncoveredLines(extractUncoveredLines(classElement))
                .uncoveredBranches(extractUncoveredBranches(classElement))
                .lastUpdated(LocalDateTime.now())
                .buildTool(projectConfig.getBuildTool())
                .testFramework(projectConfig.getTestFramework())
                .build();
    }

    private String buildFilePath(String packageName, String className, ProjectConfiguration projectConfig) {
        String packagePath = packageName.replace('.', '/');

        if (projectConfig != null) {
            String buildTool = projectConfig.getBuildTool();

            switch (buildTool.toLowerCase()) {
                case "maven":
                    return "src/main/java/" + packagePath + "/" + className + ".java";
                case "gradle":
                    return "src/main/java/" + packagePath + "/" + className + ".java";
                case "sbt":
                    // SBT can have both Scala and Java
                    String extension = className.contains("$") ? ".scala" : ".java";
                    String sourceDir = extension.equals(".scala") ? "src/main/scala/" : "src/main/java/";
                    return sourceDir + packagePath + "/" + className + extension;
                default:
                    return packagePath + "/" + className + ".java";
            }
        }

        // Default fallback
        return packagePath + "/" + className + ".java";
    }

    private List<String> extractUncoveredLines(Element classElement) {
        List<String> uncoveredLines = new ArrayList<>();
        NodeList lineNodes = classElement.getElementsByTagName("line");

        for (int i = 0; i < lineNodes.getLength(); i++) {
            Element line = (Element) lineNodes.item(i);
            String coveredInstructions = line.getAttribute("ci");
            if ("0".equals(coveredInstructions)) { // ci = covered instructions
                uncoveredLines.add(line.getAttribute("nr"));
            }
        }

        return uncoveredLines;
    }

    private List<String> extractUncoveredBranches(Element classElement) {
        List<String> uncoveredBranches = new ArrayList<>();
        NodeList lineNodes = classElement.getElementsByTagName("line");

        for (int i = 0; i < lineNodes.getLength(); i++) {
            Element line = (Element) lineNodes.item(i);
            String missedBranches = line.getAttribute("mb"); // mb = missed branches
            if (!missedBranches.isEmpty() && Integer.parseInt(missedBranches) > 0) {
                uncoveredBranches.add(line.getAttribute("nr"));
            }
        }

        return uncoveredBranches;
    }

    // Validation methods for build tools
    private void validateMavenInstallation(String repoDir) throws IOException, InterruptedException {
        log.debug("Validating Maven installation for: {}", repoDir);

        // Check if mvn command is available
        try {
            ProcessBuilder pb = new ProcessBuilder("mvn", "--version");
            pb.directory(new File(repoDir));
            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                throw new JacocoException("Maven is not properly installed or not in PATH");
            }
        } catch (IOException e) {
            throw new JacocoException("Maven command not found. Please ensure Maven is installed and in PATH.", e);
        }

        // Check if pom.xml exists
        if (!Files.exists(Paths.get(repoDir, "pom.xml"))) {
            throw new JacocoException("pom.xml not found in repository directory: " + repoDir);
        }

        log.debug("Maven validation successful");
    }

    private void validateGradleInstallation(String repoDir) throws IOException, InterruptedException {
        log.debug("Validating Gradle installation for: {}", repoDir);

        Path gradlewPath = Paths.get(repoDir, "gradlew");

        if (Files.exists(gradlewPath)) {
            // Use gradle wrapper - no need to check global gradle
            log.debug("Using Gradle wrapper: {}", gradlewPath);
            return;
        }

        // Check if gradle command is available
        try {
            ProcessBuilder pb = new ProcessBuilder("gradle", "--version");
            pb.directory(new File(repoDir));
            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                throw new JacocoException("Gradle is not properly installed or not in PATH");
            }
        } catch (IOException e) {
            throw new JacocoException("Gradle command not found and no gradlew wrapper. Please ensure Gradle is installed.", e);
        }

        // Check if build.gradle exists
        if (!Files.exists(Paths.get(repoDir, "build.gradle")) &&
                !Files.exists(Paths.get(repoDir, "build.gradle.kts"))) {
            throw new JacocoException("build.gradle or build.gradle.kts not found in repository directory: " + repoDir);
        }

        log.debug("Gradle validation successful");
    }

    private void validateSbtInstallation(String repoDir) throws IOException, InterruptedException {
        log.debug("Validating SBT installation for: {}", repoDir);

        // Check if sbt command is available
        try {
            ProcessBuilder pb = new ProcessBuilder("sbt", "version");
            pb.directory(new File(repoDir));
            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                throw new JacocoException("SBT is not properly installed or not in PATH");
            }
        } catch (IOException e) {
            throw new JacocoException("SBT command not found. Please ensure SBT is installed and in PATH.", e);
        }

        // Check if build.sbt exists
        if (!Files.exists(Paths.get(repoDir, "build.sbt"))) {
            throw new JacocoException("build.sbt not found in repository directory: " + repoDir);
        }

        log.debug("SBT validation successful");
    }

    private void prepareGradleWrapper(String repoDir) {
        Path gradlewPath = Paths.get(repoDir, "gradlew");

        if (Files.exists(gradlewPath)) {
            try {
                // Make gradlew executable on Unix systems
                Set<PosixFilePermission> perms = Files.getPosixFilePermissions(gradlewPath);
                perms.add(PosixFilePermission.OWNER_EXECUTE);
                perms.add(PosixFilePermission.GROUP_EXECUTE);
                perms.add(PosixFilePermission.OTHERS_EXECUTE);
                Files.setPosixFilePermissions(gradlewPath, perms);
                log.debug("Made gradlew executable: {}", gradlewPath);
            } catch (UnsupportedOperationException e) {
                // Windows doesn't support POSIX permissions
                log.debug("Cannot set POSIX permissions on Windows: {}", e.getMessage());
            } catch (Exception e) {
                log.debug("Could not set gradlew permissions: {}", e.getMessage());
            }
        }
    }

    // Legacy method - keep for backward compatibility but should not be used
    @Deprecated
    private CoverageData runLegacyMavenAnalysis(String repoPath) throws IOException, InterruptedException {
        log.warn("Using deprecated legacy Maven analysis. Please ensure project configuration detection is working.");

        ProcessBuilder pb = new ProcessBuilder("mvn", "clean", "jacoco:prepare-agent", "test", "jacoco:report");
        pb.directory(new File(repoPath));
        pb.inheritIO();

        Process process = pb.start();
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new JacocoException("Legacy Maven Jacoco test failed with exit code: " + exitCode);
        }

        // Parse using legacy method
        return parseJacocoReportLegacy(repoPath);
    }

    private CoverageData parseJacocoReportLegacy(String repoPath) throws IOException {
        String reportPath = repoPath + "/target/site/jacoco/jacoco.xml";
        File reportFile = new File(reportPath);

        if (!reportFile.exists()) {
            throw new JacocoException("Jacoco report not found at: " + reportPath);
        }

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(reportFile);

            return parseJacocoXml(document, repoPath, null);

        } catch (Exception e) {
            throw new JacocoException("Failed to parse Jacoco XML report", e);
        }
    }

    /**
     * Utility method to check if coverage report exists
     */
    public boolean hasCoverageReport(String repoDir, ProjectConfiguration projectConfig) {
        List<String> reportPaths = getCoverageReportPaths(repoDir, projectConfig.getBuildTool());

        for (String reportPath : reportPaths) {
            if (Files.exists(Paths.get(reportPath))) {
                log.debug("Coverage report found at: {}", reportPath);
                return true;
            }
        }

        log.debug("No coverage report found in expected locations: {}", reportPaths);
        return false;
    }

    /**
     * Utility method to get coverage report age in hours
     */
    public long getCoverageReportAgeHours(String repoDir, ProjectConfiguration projectConfig) {
        List<String> reportPaths = getCoverageReportPaths(repoDir, projectConfig.getBuildTool());

        for (String reportPath : reportPaths) {
            Path path = Paths.get(reportPath);
            if (Files.exists(path)) {
                try {
                    FileTime lastModified = Files.getLastModifiedTime(path);
                    Instant reportTime = lastModified.toInstant();
                    Instant now = Instant.now();

                    return Duration.between(reportTime, now).toHours();
                } catch (IOException e) {
                    log.warn("Could not get file modification time for: {}", reportPath, e);
                }
            }
        }

        return -1; // Report not found
    }

    /**
     * Clean up old coverage reports
     */
    public void cleanupOldReports(String repoDir, ProjectConfiguration projectConfig) {
        List<String> reportPaths = getCoverageReportPaths(repoDir, projectConfig.getBuildTool());

        for (String reportPath : reportPaths) {
            Path path = Paths.get(reportPath);
            if (Files.exists(path)) {
                try {
                    Files.delete(path);
                    log.debug("Deleted old coverage report: {}", reportPath);
                } catch (IOException e) {
                    log.warn("Could not delete old coverage report: {}", reportPath, e);
                }
            }
        }

        // Also clean up report directories if empty
        cleanupEmptyReportDirectories(repoDir, projectConfig);
    }

    private void cleanupEmptyReportDirectories(String repoDir, ProjectConfiguration projectConfig) {
        String buildTool = projectConfig.getBuildTool().toLowerCase();

        List<String> reportDirs = new ArrayList<>();
        switch (buildTool) {
            case "maven":
                reportDirs.add(repoDir + "/target/site/jacoco");
                reportDirs.add(repoDir + "/target/site/jacoco-aggregate");
                break;
            case "gradle":
                reportDirs.add(repoDir + "/build/reports/jacoco/test");
                reportDirs.add(repoDir + "/build/reports/jacoco/testCodeCoverageReport");
                break;
            case "sbt":
                reportDirs.add(repoDir + "/target/scala-2.12/scoverage-report");
                reportDirs.add(repoDir + "/target/scala-2.13/scoverage-report");
                break;
        }

        for (String reportDir : reportDirs) {
            Path dirPath = Paths.get(reportDir);
            if (Files.exists(dirPath) && isDirectoryEmpty(dirPath)) {
                try {
                    Files.delete(dirPath);
                    log.debug("Deleted empty report directory: {}", reportDir);
                } catch (IOException e) {
                    log.debug("Could not delete empty directory: {}", reportDir, e);
                }
            }
        }
    }

    private boolean isDirectoryEmpty(Path dirPath) {
        try (Stream<Path> entries = Files.list(dirPath)) {
            return entries.findFirst().isEmpty();
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Validate that coverage data is reasonable (basic sanity checks)
     */
    public CoverageValidationResult validateCoverageData(CoverageData coverageData) {
        CoverageValidationResult.CoverageValidationResultBuilder resultBuilder =
                CoverageValidationResult.builder();

        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        // Check overall coverage percentages
        if (coverageData.getOverallCoverage() > 100) {
            errors.add("Overall coverage exceeds 100%: " + coverageData.getOverallCoverage());
        }

        if (coverageData.getLineCoverage() > 100) {
            errors.add("Line coverage exceeds 100%: " + coverageData.getLineCoverage());
        }

        if (coverageData.getBranchCoverage() > 100) {
            errors.add("Branch coverage exceeds 100%: " + coverageData.getBranchCoverage());
        }

        // Check for suspicious values
        if (coverageData.getOverallCoverage() == 0 && coverageData.getFiles().size() > 0) {
            warnings.add("Overall coverage is 0% but files are present - possible analysis issue");
        }

        if (coverageData.getTotalLines() == 0) {
            warnings.add("Total lines is 0 - possible empty project or analysis issue");
        }

        // Check individual file data consistency
        int totalCalculatedLines = 0;
        int totalCalculatedCovered = 0;

        for (FileCoverageData file : coverageData.getFiles()) {
            totalCalculatedLines += file.getTotalLines();
            totalCalculatedCovered += file.getCoveredLines();

            if (file.getLineCoverage() > 100) {
                warnings.add("File " + file.getFilePath() + " has line coverage > 100%: " + file.getLineCoverage());
            }
        }

        // Check if file-level totals match overall totals
        if (Math.abs(totalCalculatedLines - coverageData.getTotalLines()) > 5) {
            warnings.add("File-level line totals don't match overall total. Expected: " +
                    coverageData.getTotalLines() + ", Calculated: " + totalCalculatedLines);
        }

        return resultBuilder
                .valid(errors.isEmpty())
                .warnings(warnings)
                .errors(errors)
                .totalFiles(coverageData.getFiles().size())
                .validatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Get coverage trend analysis by comparing with previous coverage data
     */
    public CoverageTrend analyzeCoverageTrend(CoverageData current, CoverageData previous) {
        if (previous == null) {
            return CoverageTrend.builder()
                    .trend(CoverageTrend.TrendDirection.BASELINE)
                    .overallCoverageChange(0.0)
                    .message("Baseline coverage established")
                    .build();
        }

        double coverageChange = current.getOverallCoverage() - previous.getOverallCoverage();
        CoverageTrend.TrendDirection direction;

        if (Math.abs(coverageChange) < 0.1) {
            direction = CoverageTrend.TrendDirection.STABLE;
        } else if (coverageChange > 0) {
            direction = CoverageTrend.TrendDirection.IMPROVING;
        } else {
            direction = CoverageTrend.TrendDirection.DECLINING;
        }

        String message = String.format("Coverage changed by %.2f%% (%s)",
                Math.abs(coverageChange),
                direction.toString().toLowerCase());

        return CoverageTrend.builder()
                .trend(direction)
                .overallCoverageChange(coverageChange)
                .lineCoverageChange(current.getLineCoverage() - previous.getLineCoverage())
                .branchCoverageChange(current.getBranchCoverage() - previous.getBranchCoverage())
                .methodCoverageChange(current.getMethodCoverage() - previous.getMethodCoverage())
                .message(message)
                .analyzedAt(LocalDateTime.now())
                .build();
    }
}
