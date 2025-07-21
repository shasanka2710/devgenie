package com.org.devgenie.service.coverage;

import com.org.devgenie.model.SonarBaseComponentMetrics;
import com.org.devgenie.model.SonarQubeMetricsResponse;
import com.org.devgenie.model.coverage.*;
import com.org.devgenie.service.metadata.MetadataAnalyzer;
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

        // Calculate directory statistics for UI
        int totalFileCount = calculateTotalFileCount(directFiles, subdirectories);
        int highRiskFileCount = calculateHighRiskFileCount(directFiles, subdirectories);
        int lowCoverageFileCount = calculateLowCoverageFileCount(directFiles, subdirectories);

        // Generate improvement summary
        List<DirectoryCoverageData.DirectoryImprovementSummary> improvementSummary =
                generateDirectoryImprovementSummary(directFiles, subdirectories);

        return DirectoryCoverageData.builder()
                .directoryPath(currentPath)
                .directoryName(extractDirectoryName(currentPath))
                .parentPath(getParentDirectoryPath(currentPath))
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
                .totalFileCount(totalFileCount)
                .highRiskFileCount(highRiskFileCount)
                .lowCoverageFileCount(lowCoverageFileCount)
                .improvementSummary(improvementSummary)
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

        // Basic file information
        String filePath = component.getPath();
        builder.filePath(filePath);
        builder.fileName(extractFileName(filePath));
        builder.className(extractClassName(filePath));
        builder.packageName(extractPackageName(filePath));
        builder.lastUpdated(LocalDateTime.now());
        builder.coverageSource("SONARQUBE");

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
                .uncoveredBranches(new ArrayList<>())
                .uncoveredMethods(new ArrayList<>());

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

        // Set method coverage as proxy of line coverage (SonarQube doesn't have method coverage)
        FileCoverageData temp = builder.build();
        builder.methodCoverage(temp.getLineCoverage());

        // Add improvement opportunities placeholder
        builder.improvementOpportunities(new ArrayList<>());

        return builder.build();
    }

    private String getDirectoryPath(String filePath) {
        int lastSlash = filePath.lastIndexOf('/');
        return lastSlash > 0 ? filePath.substring(0, lastSlash) : "";
    }

    private String extractDirectoryName(String directoryPath) {
        if (directoryPath == null || directoryPath.isEmpty()) return "";
        int lastSlash = directoryPath.lastIndexOf('/');
        return lastSlash >= 0 ? directoryPath.substring(lastSlash + 1) : directoryPath;
    }

    private String getParentDirectoryPath(String directoryPath) {
        if (directoryPath == null || directoryPath.isEmpty()) return "";
        int lastSlash = directoryPath.lastIndexOf('/');
        return lastSlash > 0 ? directoryPath.substring(0, lastSlash) : "";
    }

    private int calculateTotalFileCount(List<FileCoverageData> directFiles, List<DirectoryCoverageData> subdirectories) {
        int count = directFiles.size();
        for (DirectoryCoverageData subdir : subdirectories) {
            count += subdir.getTotalFileCount();
        }
        return count;
    }

    private int calculateHighRiskFileCount(List<FileCoverageData> directFiles, List<DirectoryCoverageData> subdirectories) {
        int count = (int) directFiles.stream()
                .filter(file -> file.getLineCoverage() < 30.0)
                .count();

        for (DirectoryCoverageData subdir : subdirectories) {
            count += subdir.getHighRiskFileCount();
        }
        return count;
    }

    private int calculateLowCoverageFileCount(List<FileCoverageData> directFiles, List<DirectoryCoverageData> subdirectories) {
        int count = (int) directFiles.stream()
                .filter(file -> file.getLineCoverage() < 50.0)
                .count();

        for (DirectoryCoverageData subdir : subdirectories) {
            count += subdir.getLowCoverageFileCount();
        }
        return count;
    }

    private List<DirectoryCoverageData.DirectoryImprovementSummary> generateDirectoryImprovementSummary(
            List<FileCoverageData> directFiles, List<DirectoryCoverageData> subdirectories) {

        List<DirectoryCoverageData.DirectoryImprovementSummary> summaries = new ArrayList<>();

        int highRiskCount = calculateHighRiskFileCount(directFiles, subdirectories);
        if (highRiskCount > 0) {
            summaries.add(DirectoryCoverageData.DirectoryImprovementSummary.builder()
                    .category("High Risk Files")
                    .count(highRiskCount)
                    .description("Files with coverage below 30%")
                    .priority("HIGH")
                    .estimatedImpact(highRiskCount * 15.0)
                    .build());
        }

        int lowCoverageCount = calculateLowCoverageFileCount(directFiles, subdirectories);
        if (lowCoverageCount > 0) {
            summaries.add(DirectoryCoverageData.DirectoryImprovementSummary.builder()
                    .category("Low Coverage Files")
                    .count(lowCoverageCount)
                    .description("Files with coverage below 50%")
                    .priority("MEDIUM")
                    .estimatedImpact(lowCoverageCount * 10.0)
                    .build());
        }

        return summaries;
    }

    /**
     * ENHANCED: Get coverage data from SonarQube as a flat, normalized list
     * Returns a list of CoverageData objects for both files and directories
     */
    public SonarQubeMetricsResponse getFlatCoverageData(String repoDir, String branch, ProjectConfiguration projectConfig) {
        if (!sonarQubeEnabled || sonarQubeUrl.isEmpty()) {
            log.debug("SonarQube integration is disabled or not configured");
            return SonarQubeMetricsResponse.builder().build();
        }

        try {
            String projectKey = extractProjectKey(repoDir, projectConfig);
            if (projectKey == null) {
                log.warn("Could not determine SonarQube project key");
                return SonarQubeMetricsResponse.builder().build();
            }
            log.info("Retrieving flat coverage data from SonarQube for project: {}", projectKey);

            String url = String.format("%s/api/measures/component_tree?component=%s&metricKeys=coverage,line_coverage,branch_coverage,lines_to_cover,uncovered_lines,conditions_to_cover,uncovered_conditions",
                    sonarQubeUrl, projectKey);

            HttpHeaders headers = new HttpHeaders();
            if (!sonarQubeToken.isEmpty()) {
                String encodedAuth = Base64.getEncoder().encodeToString(sonarQubeToken.getBytes());
                headers.set("Authorization", "Basic " + encodedAuth);
            }
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<SonarQubeResponse> response = restTemplate.exchange(url, HttpMethod.GET, entity, SonarQubeResponse.class);
            // Parse root metrics
            SonarQubeMetrics metrics = parseSonarQubeMetrics(response.getBody().getBaseComponent().getMeasures());


            SonarQubeResponse sonarResponse = response.getBody();
            if (sonarResponse == null || sonarResponse.getBaseComponent() == null) {
                log.warn("No coverage metrics found in SonarQube for project: {}", projectKey);
                return SonarQubeMetricsResponse.builder().build();
            }

            List<CoverageData> flatCoverageList = new ArrayList<>();
            List<SonarQubeComponent> components = sonarResponse.getComponents();
            if (components == null) return SonarQubeMetricsResponse.builder().build();;

            for (SonarQubeComponent comp : components) {
                CoverageData.CoverageDataBuilder builder = CoverageData.builder()
                        .repoPath(repoDir)
                        .branch(branch)
                        .timestamp(LocalDateTime.now())
                        .projectConfiguration(projectConfig)
                        .coverageSource(CoverageData.CoverageSource.SONARQUBE)
                        .path(comp.getPath())
                        .parentPath(getParentDirectoryPath(comp.getPath()))
                        .name(comp.getName());

                if ("FIL".equals(comp.getQualifier())) {
                    builder.type("FILE")
                        .fileName(extractFileName(comp.getPath()))
                        .className(extractClassName(comp.getPath()))
                        .packageName(extractPackageName(comp.getPath()));
                    // Coverage metrics
                    double lineCoverage = getMeasureValue(comp, "line_coverage");
                    double branchCoverage = getMeasureValue(comp, "branch_coverage");
                    int totalLines = (int) getMeasureValue(comp, "lines_to_cover");
                    int coveredLines = (int) getMeasureValue(comp, "covered_lines");
                    int totalBranches = (int) getMeasureValue(comp, "conditions_to_cover");
                    int coveredBranches = (int) getMeasureValue(comp, "covered_conditions");
                    builder.lineCoverage(lineCoverage)
                        .branchCoverage(branchCoverage)
                        .methodCoverage(lineCoverage)
                        .overallCoverage(lineCoverage)
                        .totalLines(totalLines)
                        .coveredLines(coveredLines)
                        .totalBranches(totalBranches)
                        .coveredBranches(coveredBranches)
                        .totalMethods(0)
                        .coveredMethods(0);
                } else if ("DIR".equals(comp.getQualifier())) {
                    builder.type("DIRECTORY")
                        .directoryName(comp.getName());
                    double lineCoverage = getMeasureValue(comp, "line_coverage");
                    double branchCoverage = getMeasureValue(comp, "branch_coverage");
                    int totalLines = (int) getMeasureValue(comp, "lines_to_cover");
                    int coveredLines = (int) getMeasureValue(comp, "covered_lines");
                    int totalBranches = (int) getMeasureValue(comp, "conditions_to_cover");
                    int coveredBranches = (int) getMeasureValue(comp, "covered_conditions");
                    builder.lineCoverage(lineCoverage)
                        .branchCoverage(branchCoverage)
                        .methodCoverage(lineCoverage)
                        .overallCoverage(lineCoverage)
                        .totalLines(totalLines)
                        .coveredLines(coveredLines)
                        .totalBranches(totalBranches)
                        .coveredBranches(coveredBranches)
                        .totalMethods(0)
                        .coveredMethods(0);
                    // Optionally, set children (paths of files/subdirs)
                    List<String> children = getChildrenPaths(comp.getPath(), components);
                    builder.children(children);
                }
                flatCoverageList.add(builder.build());
            }
            SonarBaseComponentMetrics sonarBaseComponentMetrics = SonarBaseComponentMetrics.builder()
                    .repositoryUrl(repoDir)
                    .branch(branch)
                    .overallCoverage(metrics.getOverallCoverage())
                    .lineCoverage(metrics.getLineCoverage())
                    .branchCoverage(metrics.getBranchCoverage())
                    .totalLines(metrics.getTotalLines())
                    .coveredLines(metrics.getCoveredLines())
                    .totalBranches(metrics.getTotalBranches())
                    .coveredBranches(metrics.getCoveredBranches())
                    .build();

            return SonarQubeMetricsResponse.builder()
                    .sonarBaseComponentMetrics(sonarBaseComponentMetrics)
                    .coverageDataList(flatCoverageList)
                    .build();

        } catch (Exception e) {
            log.error("Failed to retrieve flat coverage data from SonarQube", e);
            return SonarQubeMetricsResponse.builder().build();
        }
    }

    // Helper to get children paths for a directory
    private List<String> getChildrenPaths(String dirPath, List<SonarQubeComponent> components) {
        List<String> children = new ArrayList<>();
        for (SonarQubeComponent comp : components) {
            String parent = getParentDirectoryPath(comp.getPath());
            if (dirPath.equals(parent)) {
                children.add(comp.getPath());
            }
        }
        return children;
    }

    // Helper to get double value from measures
    private double getMeasureValue(SonarQubeComponent comp, String metric) {
        if (comp.getMeasures() == null) return 0.0;
        return comp.getMeasures().stream()
            .filter(m -> metric.equals(m.getMetric()))
            .findFirst()
            .map(m -> {
                try { return Double.parseDouble(m.getValue()); } catch (Exception e) { return 0.0; }
            })
            .orElse(0.0);
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
                    builder.overallCoverage(Double.parseDouble(measure.getValue()));
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

    /**
     * Helper methods for file path parsing
     */
    private String extractFileName(String filePath) {
        if (filePath == null) return "";
        int lastSlash = filePath.lastIndexOf('/');
        return lastSlash >= 0 ? filePath.substring(lastSlash + 1) : filePath;
    }

    private String extractClassName(String filePath) {
        String fileName = extractFileName(filePath);
        if (fileName.endsWith(".java")) {
            return fileName.substring(0, fileName.length() - 5);
        }
        return fileName;
    }

    private String extractPackageName(String filePath) {
        if (filePath == null || !filePath.contains("/")) return "";

        // Look for common Java source patterns
        String[] patterns = {"src/main/java/", "src/test/java/", "src/"};

        for (String pattern : patterns) {
            int index = filePath.indexOf(pattern);
            if (index >= 0) {
                String packagePath = filePath.substring(index + pattern.length());
                int lastSlash = packagePath.lastIndexOf('/');
                if (lastSlash > 0) {
                    return packagePath.substring(0, lastSlash).replace('/', '.');
                }
            }
        }

        // Fallback: use directory structure
        int lastSlash = filePath.lastIndexOf('/');
        if (lastSlash > 0) {
            String dirPath = filePath.substring(0, lastSlash);
            return dirPath.replace('/', '.');
        }

        return "";
    }

    /**
     * Enrich file coverage data with metadata analysis and improvement opportunities
     */
    public FileCoverageData enrichFileWithMetadata(FileCoverageData fileCoverageData,
            MetadataAnalyzer.FileMetadata fileMetadata,
            FileImprovementOpportunityService improvementService) {

        if (fileMetadata == null) {
            return fileCoverageData;
        }

        // Update file with metadata-derived complexity metrics
        FileCoverageData.FileComplexityMetrics complexityMetrics =
                FileCoverageData.FileComplexityMetrics.builder()
                        .cyclomaticComplexity(fileMetadata.getCodeComplexity() != null ?
                                fileMetadata.getCodeComplexity().getCyclomaticComplexity() : 0)
                        .cognitiveComplexity(fileMetadata.getCodeComplexity() != null ?
                                fileMetadata.getCodeComplexity().getCognitiveComplexity() : 0)
                        .businessCriticality(fileMetadata.getBusinessComplexity() != null ?
                                fileMetadata.getBusinessComplexity().getBusinessCriticality() : 0.0)
                        .riskScore(fileMetadata.getRiskScore())
                        .methodCount(fileMetadata.getCodeComplexity() != null ?
                                fileMetadata.getCodeComplexity().getTotalMethods() : 0)
                        .averageMethodLength(fileMetadata.getCodeComplexity() != null ?
                                fileMetadata.getCodeComplexity().getAverageMethodLength() : 0.0)
                        .maxNestingDepth(fileMetadata.getCodeComplexity() != null ?
                                fileMetadata.getCodeComplexity().getMaxNestingDepth() : 0)
                        .build();

        // Generate improvement opportunities
        List<FileImprovementOpportunity> opportunities =
                improvementService.generateImprovementOpportunities(fileCoverageData, fileMetadata);

        // Create enriched file coverage data
        return FileCoverageData.builder()
                .filePath(fileCoverageData.getFilePath())
                .fileName(fileCoverageData.getFileName())
                .className(fileMetadata.getClassName())
                .packageName(fileMetadata.getPackageName())
                .lineCoverage(fileCoverageData.getLineCoverage())
                .branchCoverage(fileCoverageData.getBranchCoverage())
                .methodCoverage(fileCoverageData.getMethodCoverage())
                .totalLines(fileCoverageData.getTotalLines())
                .coveredLines(fileCoverageData.getCoveredLines())
                .totalBranches(fileCoverageData.getTotalBranches())
                .coveredBranches(fileCoverageData.getCoveredBranches())
                .totalMethods(fileCoverageData.getTotalMethods())
                .coveredMethods(fileCoverageData.getCoveredMethods())
                .uncoveredLines(fileCoverageData.getUncoveredLines())
                .uncoveredBranches(fileCoverageData.getUncoveredBranches())
                .uncoveredMethods(fileCoverageData.getUncoveredMethods())
                .complexityMetrics(complexityMetrics)
                .improvementOpportunities(opportunities)
                .lastUpdated(fileCoverageData.getLastUpdated())
                .buildTool(fileCoverageData.getBuildTool())
                .testFramework(fileCoverageData.getTestFramework())
                .coverageSource(fileCoverageData.getCoverageSource())
                .build();
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

/**
 * Additional DTO classes for SonarQube API responses
 */

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
    private double overallCoverage;
    private double lineCoverage;
    private double branchCoverage;
    private int totalLines;
    private int coveredLines;
    private int totalBranches;
    private int coveredBranches;
}