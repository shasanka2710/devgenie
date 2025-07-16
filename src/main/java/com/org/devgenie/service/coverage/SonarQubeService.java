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
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SonarQubeService {

    @Value("${sonar.url:}")
    private String sonarQubeUrl;

    @Value("${sonar.token:}")
    private String sonarQubeToken;

    @Value("${sonar.enabled:true}")
    private boolean sonarQubeEnabled;

    @Autowired
    private RestTemplate restTemplate;

    /**
     * ENHANCED: Get coverage data from SonarQube with complete directory tree
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

            // Fetch all metrics (root, files, directories) in a single call
            String url = String.format("%s/api/measures/component_tree?component=%s&metricKeys=coverage,line_coverage,branch_coverage,lines_to_cover,uncovered_lines,conditions_to_cover,uncovered_conditions",
                    sonarQubeUrl, projectKey);

            HttpHeaders headers = new HttpHeaders();
            if (!sonarQubeToken.isEmpty()) {
                String encodedAuth = Base64.getEncoder().encodeToString(sonarQubeToken.getBytes());
                headers.set("Authorization", "Basic " + encodedAuth);
            }
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<SonarQubeResponse> response = restTemplate.exchange(url, HttpMethod.GET, entity, SonarQubeResponse.class);
            SonarQubeResponse sonarResponse = response.getBody();
            if (sonarResponse == null || sonarResponse.getBaseComponent() == null) {
                log.warn("No coverage metrics found in SonarQube for project: {}", projectKey);
                return null;
            }

            // Parse root metrics
            SonarQubeMetrics metrics = parseSonarQubeMetrics(sonarResponse.getBaseComponent().getMeasures());

            // Build a nested directory tree from the flat list of components
            DirectoryCoverageData rootDirectory = buildNestedDirectoryTree(sonarResponse.getComponents(), projectKey);

            return CoverageData.builder()
                    .repoPath(repoDir)
                    .rootDirectory(rootDirectory)
                    .overallCoverage(metrics.getLineCoverage())
                    .lineCoverage(metrics.getLineCoverage())
                    .branchCoverage(metrics.getBranchCoverage())
                    .methodCoverage(metrics.getLineCoverage()) // SonarQube doesn't have method coverage
                    .totalLines(metrics.getTotalLines())
                    .coveredLines(metrics.getCoveredLines())
                    .totalBranches(metrics.getTotalBranches())
                    .coveredBranches(metrics.getCoveredBranches())
                    .totalMethods(0) // Not available at project level
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

    /**
     * Build a nested directory tree from SonarQube components (files and directories)
     */
    private DirectoryCoverageData buildNestedDirectoryTree(List<SonarQubeComponent> components, String projectKey) {
        if (components == null || components.isEmpty()) return null;
        // Map of path -> DirectoryCoverageData
        Map<String, DirectoryCoverageData.DirectoryCoverageDataBuilder> dirMap = new HashMap<>();
        // Map of path -> FileCoverageData
        Map<String, FileCoverageData> fileMap = new HashMap<>();
        // Temporary maps to collect files and subdirectories for each directory
        Map<String, List<FileCoverageData>> dirFiles = new HashMap<>();
        Map<String, List<DirectoryCoverageData>> dirSubdirs = new HashMap<>();

        // First, create DirectoryCoverageData builders for all directories
        for (SonarQubeComponent comp : components) {
            if ("DIR".equals(comp.getQualifier())) {
                dirMap.put(comp.getPath(), DirectoryCoverageData.builder()
                        .directoryPath(comp.getPath())
                        .lineCoverage(getMetricValue(comp, "line_coverage"))
                        .branchCoverage(getMetricValue(comp, "branch_coverage"))
                        .methodCoverage(getMetricValue(comp, "line_coverage"))
                        .totalLines((int) getMetricValue(comp, "lines_to_cover"))
                        .coveredLines((int) getMetricValue(comp, "covered_lines"))
                        .totalBranches((int) getMetricValue(comp, "conditions_to_cover"))
                        .coveredBranches((int) getMetricValue(comp, "covered_conditions"))
                        .lastUpdated(LocalDateTime.now())
                );
                dirFiles.put(comp.getPath(), new ArrayList<>());
                dirSubdirs.put(comp.getPath(), new ArrayList<>());
            }
        }
        // Then, create FileCoverageData for all files
        for (SonarQubeComponent comp : components) {
            if ("FIL".equals(comp.getQualifier())) {
                FileCoverageData file = FileCoverageData.builder()
                        .filePath(comp.getPath())
                        .lineCoverage(getMetricValue(comp, "line_coverage"))
                        .branchCoverage(getMetricValue(comp, "branch_coverage"))
                        .methodCoverage(getMetricValue(comp, "line_coverage"))
                        .totalLines((int) getMetricValue(comp, "lines_to_cover"))
                        .coveredLines((int) getMetricValue(comp, "covered_lines"))
                        .totalBranches((int) getMetricValue(comp, "conditions_to_cover"))
                        .coveredBranches((int) getMetricValue(comp, "covered_conditions"))
                        .lastUpdated(LocalDateTime.now())
                        .build();
                fileMap.put(comp.getPath(), file);
                String parentDir = getDirectoryPath(file.getFilePath());
                if (dirFiles.containsKey(parentDir)) {
                    dirFiles.get(parentDir).add(file);
                }
            }
        }
        // Place directories into their parent directories
        for (String dirPath : dirMap.keySet()) {
            String parentDir = getDirectoryPath(dirPath);
            if (dirSubdirs.containsKey(parentDir) && !parentDir.equals(dirPath)) {
                dirSubdirs.get(parentDir).add(dirMap.get(dirPath).files(dirFiles.get(dirPath)).subdirectories(dirSubdirs.get(dirPath)).build());
            }
        }
        // Find the root directory (the one not contained by any other)
        String rootPath = findRootDirectoryPath(dirMap.keySet());
        DirectoryCoverageData.DirectoryCoverageDataBuilder rootBuilder = dirMap.get(rootPath);
        if (rootBuilder != null) {
            // Set files and subdirectories for root
            rootBuilder.files(dirFiles.get(rootPath));
            rootBuilder.subdirectories(dirSubdirs.get(rootPath));
            return rootBuilder.build();
        }
        return null;
    }

    private double getMetricValue(SonarQubeComponent comp, String metric) {
        if (comp.getMeasures() == null) return 0.0;
        for (SonarQubeMeasure m : comp.getMeasures()) {
            if (metric.equals(m.getMetric())) {
                try {
                    return Double.parseDouble(m.getValue());
                } catch (Exception ignored) {}
            }
        }
        return 0.0;
    }

    private String findRootDirectoryPath(Set<String> dirPaths) {
        // The root is the shortest path (with no parent in the set)
        return dirPaths.stream().min(Comparator.comparingInt(String::length)).orElse("");
    }

    /**
     * NEW: Build complete directory tree from SonarQube components
     */
    private DirectoryCoverageData buildCompleteDirectoryTree(List<SonarQubeComponent> fileComponents,
                                                             List<SonarQubeComponent> directoryComponents,
                                                             String projectKey) {
        try {
            // Create a map of all directories for easy lookup
            Map<String, SonarQubeComponent> directoryMap = directoryComponents.stream()
                    .collect(Collectors.toMap(SonarQubeComponent::getPath, comp -> comp));

            // Convert file components to FileCoverageData
            Map<String, FileCoverageData> fileMap = fileComponents.stream()
                    .collect(Collectors.toMap(
                            SonarQubeComponent::getPath,
                            comp -> convertToFileCoverageData(comp)));

            // Find root directory (typically src/main/java or similar)
            String rootPath = determineRootPath(fileComponents, directoryComponents);
            log.info("Determined root path: {}", rootPath);

            // Build tree recursively
            return buildDirectoryNodeFromSonarQube(rootPath, directoryMap, fileMap, fileComponents);

        } catch (Exception e) {
            log.error("Failed to build directory tree from SonarQube data", e);
            return null;
        }
    }

    /**
     * Determine the root path for the directory tree
     */
    private String determineRootPath(List<SonarQubeComponent> fileComponents,
                                     List<SonarQubeComponent> directoryComponents) {
        // Strategy 1: Find common root from file paths
        if (!fileComponents.isEmpty()) {
            String firstFilePath = fileComponents.get(0).getPath();
            if (firstFilePath.contains("src/main/java")) {
                return "src/main/java";
            } else if (firstFilePath.contains("src/main/scala")) {
                return "src/main/scala";
            } else if (firstFilePath.contains("src/main/kotlin")) {
                return "src/main/kotlin";
            }
        }

        // Strategy 2: Find shortest directory path
        Optional<String> shortestDir = directoryComponents.stream()
                .map(SonarQubeComponent::getPath)
                .filter(path -> path.contains("src"))
                .min(Comparator.comparing(String::length));

        return shortestDir.orElse("src/main/java");
    }

    /**
     * Build directory node recursively from SonarQube data
     */
    private DirectoryCoverageData buildDirectoryNodeFromSonarQube(String currentPath,
                                                                  Map<String, SonarQubeComponent> directoryMap,
                                                                  Map<String, FileCoverageData> fileMap,
                                                                  List<SonarQubeComponent> allFileComponents) {
        // Get files directly in this directory
        List<FileCoverageData> directFiles = fileMap.entrySet().stream()
                .filter(entry -> {
                    String filePath = entry.getKey();
                    String fileDir = getDirectoryPath(filePath);
                    return currentPath.equals(fileDir);
                })
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());

        // Get subdirectories
        List<DirectoryCoverageData> subdirectories = new ArrayList<>();
        Set<String> immediateSubdirs = getImmediateSubdirectories(currentPath, directoryMap.keySet());

        for (String subdirPath : immediateSubdirs) {
            DirectoryCoverageData subdir = buildDirectoryNodeFromSonarQube(
                    subdirPath, directoryMap, fileMap, allFileComponents);
            if (subdir != null) {
                subdirectories.add(subdir);
            }
        }

        // Skip empty directories
        if (directFiles.isEmpty() && subdirectories.isEmpty()) {
            return null;
        }

        // Get directory-level coverage from SonarQube if available
        SonarQubeComponent dirComponent = directoryMap.get(currentPath);
        DirectoryCoverageMetrics dirMetrics = calculateDirectoryMetrics(directFiles, subdirectories, dirComponent);

        return DirectoryCoverageData.builder()
                .directoryPath(currentPath)
                .overallCoverage(dirMetrics.getLineCoverage())
                .lineCoverage(dirMetrics.getLineCoverage())
                .branchCoverage(dirMetrics.getBranchCoverage())
                .methodCoverage(dirMetrics.getMethodCoverage())
                .totalLines(dirMetrics.getTotalLines())
                .coveredLines(dirMetrics.getCoveredLines())
                .totalBranches(dirMetrics.getTotalBranches())
                .coveredBranches(dirMetrics.getCoveredBranches())
                .totalMethods(dirMetrics.getTotalMethods())
                .coveredMethods(dirMetrics.getCoveredMethods())
                .files(directFiles)
                .subdirectories(subdirectories)
                .lastUpdated(LocalDateTime.now())
                .build();
    }

    /**
     * Calculate directory-level metrics
     */
    private DirectoryCoverageMetrics calculateDirectoryMetrics(List<FileCoverageData> directFiles,
                                                               List<DirectoryCoverageData> subdirectories,
                                                               SonarQubeComponent dirComponent) {
        // Aggregate from files and subdirectories
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

        // If SonarQube has directory-level metrics, use those instead
        if (dirComponent != null && dirComponent.getMeasures() != null) {
            for (SonarQubeMeasure measure : dirComponent.getMeasures()) {
                switch (measure.getMetric()) {
                    case "line_coverage":
                        lineCoverage = Double.parseDouble(measure.getValue());
                        break;
                    case "branch_coverage":
                        branchCoverage = Double.parseDouble(measure.getValue());
                        break;
                    case "lines_to_cover":
                        totalLines = Integer.parseInt(measure.getValue());
                        break;
                    case "covered_lines":
                        coveredLines = Integer.parseInt(measure.getValue());
                        break;
                }
            }
        }

        return DirectoryCoverageMetrics.builder()
                .lineCoverage(lineCoverage)
                .branchCoverage(branchCoverage)
                .methodCoverage(methodCoverage)
                .totalLines(totalLines)
                .coveredLines(coveredLines)
                .totalBranches(totalBranches)
                .coveredBranches(coveredBranches)
                .totalMethods(totalMethods)
                .coveredMethods(coveredMethods)
                .build();
    }

    /**
     * Get immediate subdirectories of a given path
     */
    private Set<String> getImmediateSubdirectories(String parentPath, Set<String> allDirectories) {
        return allDirectories.stream()
                .filter(dir -> dir.startsWith(parentPath + "/"))
                .map(dir -> {
                    String relativePath = dir.substring(parentPath.length() + 1);
                    int slashIndex = relativePath.indexOf('/');
                    return slashIndex > 0 ?
                            parentPath + "/" + relativePath.substring(0, slashIndex) :
                            dir;
                })
                .collect(Collectors.toSet());
    }

    /**
     * Convert SonarQube component to FileCoverageData
     */
    private FileCoverageData convertToFileCoverageData(SonarQubeComponent component) {
        FileCoverageData.FileCoverageDataBuilder builder = FileCoverageData.builder();
        builder.filePath(component.getPath());
        builder.lastUpdated(LocalDateTime.now());

        // Set defaults
        builder.lineCoverage(0.0)
                .branchCoverage(0.0)
                .methodCoverage(0.0)
                .totalLines(0)
                .coveredLines(0)
                .totalBranches(0)
                .coveredBranches(0)
                .totalMethods(0)
                .coveredMethods(0)
                .uncoveredLines(new ArrayList<>())
                .uncoveredBranches(new ArrayList<>());

        // Parse measures if available
        if (component.getMeasures() != null) {
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
                    case "conditions_to_cover":
                        builder.totalBranches(Integer.parseInt(measure.getValue()));
                        break;
                    case "covered_conditions":
                        builder.coveredBranches(Integer.parseInt(measure.getValue()));
                        break;
                }
            }
        }

        // Set method coverage as proxy of line coverage
        FileCoverageData temp = builder.build();
        builder.methodCoverage(temp.getLineCoverage());

        return builder.build();
    }

    private String getDirectoryPath(String filePath) {
        int lastSlash = filePath.lastIndexOf('/');
        return lastSlash > 0 ? filePath.substring(0, lastSlash) : "";
    }

    // Keep existing methods for backward compatibility...

    private String extractProjectKey(String repoDir, ProjectConfiguration projectConfig) {
        String projectKey = null;
        // Strategy 1: Check sonar-project.properties
        Path sonarPropertiesPath = Paths.get(repoDir, "sonar-project.properties");
        if (Files.exists(sonarPropertiesPath)) {
            try {
                String content = Files.readString(sonarPropertiesPath);
                Pattern pattern = Pattern.compile("sonar\\.projectKey\\s*=\\s*(.+)");
                Matcher matcher = pattern.matcher(content);
                if (matcher.find()) {
                    projectKey= matcher.group(1).trim();
                }
            } catch (IOException e) {
                log.debug("Failed to read sonar-project.properties", e);
            }
        }

        // Strategy 2: Check build file configurations
        try {
            if ("maven".equals(projectConfig.getBuildTool())) {
                projectKey= extractProjectKeyFromMaven(repoDir);
            } else if ("gradle".equals(projectConfig.getBuildTool())) {
                projectKey= extractProjectKeyFromGradle(repoDir);
            }
        } catch (Exception e) {
            log.debug("Failed to extract project key from build file", e);
        }

        // Strategy 3: Use directory name as fallback
        projectKey= Paths.get(repoDir).getFileName().toString();
        return projectKey;
    }

    private String extractProjectKeyFromMaven(String repoDir) throws IOException {
        Path pomPath = Paths.get(repoDir, "pom.xml");
        if (!Files.exists(pomPath)) return null;

        String pomContent = Files.readString(pomPath);
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
        Pattern groupPattern = Pattern.compile("group\\s*[=:]?\\s*['\"]([^'\"]+)['\"]");
        Pattern namePattern = Pattern.compile("(name|archivesBaseName)\\s*[=:]?\\s*['\"]([^'\"]+)['\"]");

        Matcher groupMatcher = groupPattern.matcher(buildContent);
        Matcher nameMatcher = namePattern.matcher(buildContent);

        if (groupMatcher.find() && nameMatcher.find()) {
            return groupMatcher.group(1) + ":" + nameMatcher.group(2);
        }

        return null;
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

    private FileCoverageData parseSonarQubeFileCoverage(SonarQubeComponent component, ProjectConfiguration projectConfig) {
        return convertToFileCoverageData(component);
    }

    // Helper classes
    @lombok.Data
    @lombok.Builder
    private static class DirectoryCoverageMetrics {
        private double lineCoverage;
        private double branchCoverage;
        private double methodCoverage;
        private int totalLines;
        private int coveredLines;
        private int totalBranches;
        private int coveredBranches;
        private int totalMethods;
        private int coveredMethods;
    }
}

// Additional DTO classes for SonarQube API responses

@lombok.Data
class SonarQubeComponentTreeResponse {
    private List<SonarQubeComponent> components;
    private SonarQubePaging paging;
    private SonarQubeComponentWithMeasures baseComponent;
}

@lombok.Data
class SonarQubeComponent {
    private String key;
    private String name;
    private String qualifier; // FIL for files, DIR for directories
    private String path;
    private String language;
    private List<SonarQubeMeasure> measures;
}

@lombok.Data
class SonarQubePaging {
    private int pageIndex;
    private int pageSize;
    private int total;
}

@lombok.Data
class SonarQubeResponse {
    private SonarQubePaging paging;
    private SonarQubeComponentWithMeasures baseComponent;
    private List<SonarQubeComponent> components;
}

@lombok.Data
class SonarQubeComponentWithMeasures {
    private String key;
    private String name;
    private String qualifier;
    private String path;
    private String language;
    private List<SonarQubeMeasure> measures;
}

@lombok.Data
class SonarQubeMeasure {
    private String metric;
    private String value;
}

@lombok.Data
@lombok.Builder
class SonarQubeMetrics {
    private double lineCoverage;
    private double branchCoverage;
    private int totalLines;
    private int coveredLines;
    private int totalBranches;
    private int coveredBranches;
}