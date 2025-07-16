package com.org.devgenie.service.coverage;

import com.org.devgenie.model.coverage.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class SonarQubeService {

    @Value("${sonarqube.url:}")
    private String sonarQubeUrl;

    @Value("${sonarqube.token:}")
    private String sonarQubeToken;

    @Value("${sonarqube.enabled:false}")
    private boolean sonarQubeEnabled;

    @Autowired
    private RestTemplate restTemplate;

    /**
     * Get coverage data from SonarQube
     */
    public CoverageData getCoverageData(String repoDir, ProjectConfiguration projectConfig) {
        if (!sonarQubeEnabled || sonarQubeUrl.isEmpty()) {
            log.debug("SonarQube integration is disabled or not configured");
            return null;
        }

        try {
            String projectKey = extractProjectKey(repoDir, projectConfig);
            if (projectKey == null) {
                log.warn("Could not determine SonarQube project key");
                return null;
            }

            log.info("Retrieving coverage data from SonarQube for project: {}", projectKey);

            // Get overall metrics
            SonarQubeMetrics metrics = getSonarQubeMetrics(projectKey);
            if (metrics == null) {
                log.warn("No coverage metrics found in SonarQube for project: {}", projectKey);
                return null;
            }

            // Get file-level coverage
            List<FileCoverageData> files = getSonarQubeFileCoverage(projectKey, projectConfig);

            // Build a recursive directory tree from file list
            DirectoryCoverageData rootDirectory = buildDirectoryTreeRecursive(files, "src/main/java", LocalDateTime.now());
            List<DirectoryCoverageData> directories = rootDirectory != null ? List.of(rootDirectory) : new ArrayList<>();

            return CoverageData.builder()
                    .repoPath(repoDir)
                    .files(files)
                    .directories(directories)
                    .overallCoverage(metrics.getLineCoverage())
                    .lineCoverage(metrics.getLineCoverage())
                    .branchCoverage(metrics.getBranchCoverage())
                    .methodCoverage(metrics.getLineCoverage()) // SonarQube doesn't have method coverage
                    .totalLines(metrics.getTotalLines())
                    .coveredLines(metrics.getCoveredLines())
                    .totalBranches(metrics.getTotalBranches())
                    .coveredBranches(metrics.getCoveredBranches())
                    .totalMethods(0) // Not available in SonarQube
                    .coveredMethods(0)
                    .timestamp(LocalDateTime.now())
                    .projectConfiguration(projectConfig)
                    .coverageSource(CoverageData.CoverageSource.SONARQUBE)
                    .build();

        } catch (Exception e) {
            log.error("Failed to retrieve coverage data from SonarQube", e);
            return null;
        }
    }

    private String extractProjectKey(String repoDir, ProjectConfiguration projectConfig) {
        // Strategy 1: Check sonar-project.properties
        Path sonarPropertiesPath = Paths.get(repoDir, "sonar-project.properties");
        if (Files.exists(sonarPropertiesPath)) {
            try {
                String content = Files.readString(sonarPropertiesPath);
                Pattern pattern = Pattern.compile("sonar\\.projectKey\\s*=\\s*(.+)");
                Matcher matcher = pattern.matcher(content);
                if (matcher.find()) {
                    return matcher.group(1).trim();
                }
            } catch (IOException e) {
                log.debug("Failed to read sonar-project.properties", e);
            }
        }

        // Strategy 2: Check build file configurations
        try {
            if ("maven".equals(projectConfig.getBuildTool())) {
                return extractProjectKeyFromMaven(repoDir);
            } else if ("gradle".equals(projectConfig.getBuildTool())) {
                return extractProjectKeyFromGradle(repoDir);
            }
        } catch (Exception e) {
            log.debug("Failed to extract project key from build file", e);
        }

        // Strategy 3: Use directory name as fallback
        return Paths.get(repoDir).getFileName().toString();
    }

    private String extractProjectKeyFromMaven(String repoDir) throws IOException {
        Path pomPath = Paths.get(repoDir, "pom.xml");
        if (!Files.exists(pomPath)) return null;

        String pomContent = Files.readString(pomPath);

        // Look for groupId and artifactId
        Pattern groupPattern = Pattern.compile("<groupId>([^<]+)</groupId>");
        Pattern artifactPattern = Pattern.compile("<artifactId>([^<]+)</artifactId>");

        Matcher groupMatcher = groupPattern.matcher(pomContent);
        Matcher artifactMatcher = artifactPattern.matcher(pomContent);

        if (groupMatcher.find() && artifactMatcher.find()) {
            return groupMatcher.group(1) + ":" + artifactMatcher.group(1);
        }

        return null;
    }

    private String extractProjectKeyFromGradle(String repoDir) throws IOException {
        Path buildGradlePath = Paths.get(repoDir, "build.gradle");
        Path buildGradleKtsPath = Paths.get(repoDir, "build.gradle.kts");

        Path targetFile = Files.exists(buildGradlePath) ? buildGradlePath : buildGradleKtsPath;
        if (!Files.exists(targetFile)) return null;

        String buildContent = Files.readString(targetFile);

        // Look for group and name/archivesBaseName
        Pattern groupPattern = Pattern.compile("group\\s*[=:]?\\s*['\"]([^'\"]+)['\"]");
        Pattern namePattern = Pattern.compile("(name|archivesBaseName)\\s*[=:]?\\s*['\"]([^'\"]+)['\"]");

        Matcher groupMatcher = groupPattern.matcher(buildContent);
        Matcher nameMatcher = namePattern.matcher(buildContent);

        if (groupMatcher.find() && nameMatcher.find()) {
            return groupMatcher.group(1) + ":" + nameMatcher.group(2);
        }

        return null;
    }

    private SonarQubeMetrics getSonarQubeMetrics(String projectKey) {
        try {
            String url = String.format("%s/api/measures/component?component=%s&metricKeys=coverage,line_coverage,branch_coverage,lines_to_cover,covered_lines,conditions_to_cover,covered_conditions",
                    sonarQubeUrl, projectKey);

            HttpHeaders headers = new HttpHeaders();
            if (!sonarQubeToken.isEmpty()) {
                headers.set("Authorization", "Bearer " + sonarQubeToken);
            }

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<SonarQubeResponse> response = restTemplate.exchange(url, HttpMethod.GET, entity, SonarQubeResponse.class);

            if (response.getBody() != null && response.getBody().getComponent() != null) {
                return parseSonarQubeMetrics(response.getBody().getComponent().getMeasures());
            }

            return null;

        } catch (Exception e) {
            log.error("Failed to get SonarQube metrics for project: {}", projectKey, e);
            return null;
        }
    }

    private SonarQubeMetrics parseSonarQubeMetrics(List<SonarQubeMeasure> measures) {
        SonarQubeMetrics.SonarQubeMetricsBuilder builder = SonarQubeMetrics.builder();

        for (SonarQubeMeasure measure : measures) {
            switch (measure.getMetric()) {
                case "coverage":
                case "line_coverage":
                    builder.lineCoverage(Double.parseDouble(measure.getValue()));
                    break;
                case "branch_coverage":
                    builder.branchCoverage(Double.parseDouble(measure.getValue()));
                    break;
                case "lines_to_cover":
                    builder.totalLines(Integer.parseInt(measure.getValue()));
                    break;
                case "covered_lines":
                    builder.coveredLines(Integer.parseInt(measure.getValue()));
                    break;
                case "conditions_to_cover":
                    builder.totalBranches(Integer.parseInt(measure.getValue()));
                    break;
                case "covered_conditions":
                    builder.coveredBranches(Integer.parseInt(measure.getValue()));
                    break;
            }
        }

        return builder.build();
    }

    private List<FileCoverageData> getSonarQubeFileCoverage(String projectKey, ProjectConfiguration projectConfig) {
        try {
            String url = String.format("%s/api/measures/component_tree?component=%s&qualifiers=FIL&metricKeys=coverage,line_coverage,branch_coverage,lines_to_cover,covered_lines",
                    sonarQubeUrl, projectKey);

            HttpHeaders headers = new HttpHeaders();
            if (!sonarQubeToken.isEmpty()) {
                headers.set("Authorization", "Bearer " + sonarQubeToken);
            }

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<SonarQubeComponentTreeResponse> response = restTemplate.exchange(url, HttpMethod.GET, entity, SonarQubeComponentTreeResponse.class);

            List<FileCoverageData> files = new ArrayList<>();

            if (response.getBody() != null && response.getBody().getComponents() != null) {
                for (SonarQubeComponent component : response.getBody().getComponents()) {
                    FileCoverageData fileData = parseSonarQubeFileCoverage(component, projectConfig);
                    if (fileData != null) {
                        files.add(fileData);
                    }
                }
            }

            return files;

        } catch (Exception e) {
            log.error("Failed to get SonarQube file coverage for project: {}", projectKey, e);
            return new ArrayList<>();
        }
    }

    private FileCoverageData parseSonarQubeFileCoverage(SonarQubeComponent component, ProjectConfiguration projectConfig) {
        if (component.getMeasures() == null ) {
            return null;
        }

        FileCoverageData.FileCoverageDataBuilder builder = FileCoverageData.builder();
        builder.filePath(component.getPath());
        builder.lastUpdated(LocalDateTime.now());
        builder.buildTool(projectConfig.getBuildTool());
        builder.testFramework(projectConfig.getTestFramework());

        for (SonarQubeMeasure measure : component.getMeasures()) {
            switch (measure.getMetric()) {
                case "coverage":
                case "line_coverage":
                    builder.lineCoverage(Double.parseDouble(measure.getValue()));
                    break;
                case "branch_coverage":
                    builder.branchCoverage(Double.parseDouble(measure.getValue()));
                    break;
                case "lines_to_cover":
                    builder.totalLines(Integer.parseInt(measure.getValue()));
                    break;
                case "covered_lines":
                    builder.coveredLines(Integer.parseInt(measure.getValue()));
                    break;
            }
        }

        // Set defaults for missing data
        if (builder.build().getMethodCoverage() == 0.0) {
            builder.methodCoverage(builder.build().getLineCoverage()); // Use line coverage as proxy
        }

        builder.uncoveredLines(new ArrayList<>())
                .uncoveredBranches(new ArrayList<>());

        return builder.build();
    }

    // Recursively build directory tree from file list
    private DirectoryCoverageData buildDirectoryTreeRecursive(List<FileCoverageData> files, String dirPath, LocalDateTime now) {
        List<FileCoverageData> directFiles = new ArrayList<>();
        Map<String, List<FileCoverageData>> subDirMap = new java.util.HashMap<>();
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
}
