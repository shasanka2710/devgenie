package com.org.devgenie.service.coverage;

import com.org.devgenie.model.coverage.CoverageData;
import com.org.devgenie.mongo.CoverageDataFlatMongoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RepositoryDashboardService {

    @Autowired
    private CoverageDataFlatMongoRepository coverageRepository;

    @Autowired
    private AiImprovementService aiImprovementService;

    public DashboardData getDashboardData(String repoPath, String branch) {
        log.info("Fetching dashboard data for repo: {} and branch: {}", repoPath, branch);
        List<CoverageData> allCoverageData = coverageRepository.findByRepoPathAndBranch(repoPath, branch);

        
        if (allCoverageData.isEmpty()) {
            log.warn("No coverage data found for repo: {} and branch: {}", repoPath, branch);
            return createEmptyDashboard();
        }

        // Calculate overall metrics
        OverallMetrics overallMetrics = calculateOverallMetrics(allCoverageData);
        
        // Build file tree structure
        FileTreeNode fileTree = buildFileTree(allCoverageData);
        
        // Get file details
        List<FileDetails> fileDetails = buildFileDetails(allCoverageData);

        return DashboardData.builder()
                .overallMetrics(overallMetrics)
                .fileTree(fileTree)
                .fileDetails(fileDetails)
                .build();
    }

    private OverallMetrics calculateOverallMetrics(List<CoverageData> coverageData) {
        log.info("Calculating overall metrics for coverage data of size: {}", coverageData.size());
        // Filter out directories to avoid double counting
        List<CoverageData> files = coverageData.stream()
                .filter(data -> "FILE".equals(data.getType()))
                .collect(Collectors.toList());

        if (files.isEmpty()) {
            return OverallMetrics.builder()
                    .overallCoverage(0.0)
                    .lineCoverage(0.0)
                    .branchCoverage(0.0)
                    .methodCoverage(0.0)
                    .build();
        }

        int totalLines = files.stream().mapToInt(CoverageData::getTotalLines).sum();
        int coveredLines = files.stream().mapToInt(CoverageData::getCoveredLines).sum();
        int totalBranches = files.stream().mapToInt(CoverageData::getTotalBranches).sum();
        int coveredBranches = files.stream().mapToInt(CoverageData::getCoveredBranches).sum();
        int totalMethods = files.stream().mapToInt(CoverageData::getTotalMethods).sum();
        int coveredMethods = files.stream().mapToInt(CoverageData::getCoveredMethods).sum();

        double lineCoverage = totalLines > 0 ? (double) coveredLines / totalLines * 100 : 0.0;
        double branchCoverage = totalBranches > 0 ? (double) coveredBranches / totalBranches * 100 : 0.0;
        double methodCoverage = totalMethods > 0 ? (double) coveredMethods / totalMethods * 100 : 0.0;
        
        // Calculate overall coverage as weighted average
        double overallCoverage = (lineCoverage + branchCoverage + methodCoverage) / 3;

        return OverallMetrics.builder()
                .overallCoverage(Math.round(overallCoverage * 10.0) / 10.0)
                .lineCoverage(Math.round(lineCoverage * 10.0) / 10.0)
                .branchCoverage(Math.round(branchCoverage * 10.0) / 10.0)
                .methodCoverage(Math.round(methodCoverage * 10.0) / 10.0)
                .totalLines(totalLines)
                .coveredLines(coveredLines)
                .totalBranches(totalBranches)
                .coveredBranches(coveredBranches)
                .totalMethods(totalMethods)
                .coveredMethods(coveredMethods)
                .build();
    }

    private FileTreeNode buildFileTree(List<CoverageData> coverageData) {
        // Use package-style tree for better UI
        return buildPackageStyleFileTree(coverageData);
    }

    public FileTreeNode buildPackageStyleFileTree(List<CoverageData> coverageData) {
        log.info("=== BUILDING PACKAGE-STYLE FILE TREE ===");
        log.info("Processing {} coverage data items", coverageData.size());
        
        FileTreeNode root = new FileTreeNode("src", "DIRECTORY", null);
        root.setNodeType("DIRECTORY");
        
        // Separate Java files (with package names) from other files
        Map<String, List<CoverageData>> javaFilesByPackage = new HashMap<>();
        List<CoverageData> nonJavaFiles = new ArrayList<>();
        
        for (CoverageData data : coverageData) {
            log.debug("Processing file: {} (package: {}, type: {})", data.getPath(), data.getPackageName(), data.getType());
            if (data.getPackageName() != null && !data.getPackageName().isEmpty() && 
                "FILE".equals(data.getType()) && data.getPath().endsWith(".java")) {
                javaFilesByPackage.computeIfAbsent(data.getPackageName(), k -> new ArrayList<>()).add(data);
                log.debug("Added to package: {}", data.getPackageName());
            } else {
                nonJavaFiles.add(data);
            }
        }
        
        log.info("Found {} Java packages: {}", javaFilesByPackage.size(), javaFilesByPackage.keySet());
        
        // Create main directory node
        FileTreeNode mainNode = new FileTreeNode("main", "DIRECTORY", null);
        mainNode.setNodeType("DIRECTORY");
        root.addChild(mainNode);
        
        // Add Java package nodes
        if (!javaFilesByPackage.isEmpty()) {
            FileTreeNode javaNode = new FileTreeNode("java", "DIRECTORY", null);
            javaNode.setNodeType("DIRECTORY");
            javaNode.setFlattened(true);
            mainNode.addChild(javaNode);
            
            // Create package nodes
            for (Map.Entry<String, List<CoverageData>> packageEntry : javaFilesByPackage.entrySet()) {
                String packageName = packageEntry.getKey();
                List<CoverageData> packageFiles = packageEntry.getValue();
                
                log.info("Creating PACKAGE node: {} with {} files", packageName, packageFiles.size());
                FileTreeNode packageNode = new FileTreeNode(packageName, "DIRECTORY", null, "PACKAGE", packageName);
                log.info("Package node created - nodeType: {}, packageName: {}", packageNode.getNodeType(), packageNode.getPackageName());
                javaNode.addChild(packageNode);
                
                // Add files to package
                for (CoverageData fileData : packageFiles) {
                    FileTreeNode fileNode = new FileTreeNode(fileData.getFileName(), "FILE", fileData);
                    fileNode.setNodeType("FILE");
                    packageNode.addChild(fileNode);
                }
            }
        }
        
        // Add non-Java files using traditional hierarchy (but skip empty intermediate directories)
        buildTraditionalFileTree(nonJavaFiles, mainNode);
        
        return root;
    }

    private void buildTraditionalFileTree(List<CoverageData> coverageData, FileTreeNode parentNode) {
        if (coverageData.isEmpty()) {
            return;
        }
        
        Map<String, FileTreeNode> nodeMap = new HashMap<>();
        
        // Sort by path to ensure proper hierarchy
        coverageData.sort(Comparator.comparing(CoverageData::getPath));

        for (CoverageData data : coverageData) {
            String[] pathParts = data.getPath().split("/");
            FileTreeNode currentNode = parentNode;
            
            // Skip 'src/main' parts since we already have them
            int startIndex = 0;
            if (pathParts.length > 2 && "src".equals(pathParts[0]) && "main".equals(pathParts[1])) {
                startIndex = 2;
            }

            // Build the path hierarchy
            StringBuilder currentPath = new StringBuilder();
            for (int i = startIndex; i < pathParts.length; i++) {
                if (currentPath.length() > 0) currentPath.append("/");
                currentPath.append(pathParts[i]);
                
                String fullPath = currentPath.toString();
                
                if (!nodeMap.containsKey(fullPath)) {
                    FileTreeNode newNode = new FileTreeNode(
                            pathParts[i],
                            i == pathParts.length - 1 ? data.getType() : "DIRECTORY",
                            data
                    );
                    newNode.setNodeType(i == pathParts.length - 1 ? "FILE" : "DIRECTORY");
                    nodeMap.put(fullPath, newNode);
                    currentNode.addChild(newNode);
                    currentNode = newNode;
                } else {
                    currentNode = nodeMap.get(fullPath);
                }
            }
        }
    }

    private List<FileDetails> buildFileDetails(List<CoverageData> coverageData) {
        return coverageData.stream()
                .filter(data -> "FILE".equals(data.getType()))
                .map(this::convertToFileDetails)
                .collect(Collectors.toList());
    }

    private FileDetails convertToFileDetails(CoverageData data) {
        return FileDetails.builder()
                .fileName(data.getFileName())
                .filePath(data.getPath())
                .packageName(data.getPackageName())
                .className(data.getClassName())
                .lineCoverage(data.getLineCoverage())
                .branchCoverage(data.getBranchCoverage())
                .methodCoverage(data.getMethodCoverage())
                .totalLines(data.getTotalLines())
                .coveredLines(data.getCoveredLines())
                .totalBranches(data.getTotalBranches())
                .coveredBranches(data.getCoveredBranches())
                .totalMethods(data.getTotalMethods())
                .coveredMethods(data.getCoveredMethods())
                .improvementOpportunities(generateImprovementOpportunities(data))
                .build();
    }

    private List<ImprovementOpportunity> generateImprovementOpportunities(CoverageData data) {
        // Use AI service to generate smart improvement opportunities
        List<AiImprovementService.ImprovementOpportunity> aiOpportunities = 
            aiImprovementService.generateImprovementOpportunities(data);
        
        // Convert to our internal format
        return aiOpportunities.stream()
                .map(aiOpp -> ImprovementOpportunity.builder()
                        .type(aiOpp.getType())
                        .description(aiOpp.getDescription())
                        .priority(aiOpp.getPriority())
                        .estimatedImpact(aiOpp.getEstimatedImpact())
                        .build())
                .collect(Collectors.toList());
    }

    private DashboardData createEmptyDashboard() {
        return DashboardData.builder()
                .overallMetrics(OverallMetrics.builder()
                        .overallCoverage(0.0)
                        .lineCoverage(0.0)
                        .branchCoverage(0.0)
                        .methodCoverage(0.0)
                        .build())
                .fileTree(new FileTreeNode("src", "DIRECTORY", null))
                .fileDetails(new ArrayList<>())
                .build();
    }

    // Inner classes for data transfer
    @lombok.Data
    @lombok.Builder
    public static class DashboardData {
        private OverallMetrics overallMetrics;
        private FileTreeNode fileTree;
        private List<FileDetails> fileDetails;
    }

    @lombok.Data
    @lombok.Builder
    public static class OverallMetrics {
        private double overallCoverage;
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

    @lombok.Data
    @lombok.Builder
    public static class FileDetails {
        private String fileName;
        private String filePath;
        private String packageName;
        private String className;
        private double lineCoverage;
        private double branchCoverage;
        private double methodCoverage;
        private int totalLines;
        private int coveredLines;
        private int totalBranches;
        private int coveredBranches;
        private int totalMethods;
        private int coveredMethods;
        private List<ImprovementOpportunity> improvementOpportunities;
    }

    @lombok.Data
    @lombok.Builder
    public static class ImprovementOpportunity {
        private String type;
        private String description;
        private String priority;
        private String estimatedImpact;
    }

    public static class FileTreeNode {
        private String name;
        private String type;
        private CoverageData data;
        private List<FileTreeNode> children;
        private String nodeType; // "PACKAGE", "DIRECTORY", "FILE"
        private String packageName; // For package nodes
        private boolean isFlattened; // Indicates if this is a flattened package structure

        public FileTreeNode(String name, String type, CoverageData data) {
            this.name = name;
            this.type = type;
            this.data = data;
            this.children = new ArrayList<>();
            this.nodeType = "DIRECTORY"; // Default
            this.isFlattened = false;
        }

        // New constructor for package nodes
        public FileTreeNode(String name, String type, CoverageData data, String nodeType, String packageName) {
            this(name, type, data);
            this.nodeType = nodeType;
            this.packageName = packageName;
            this.isFlattened = "PACKAGE".equals(nodeType);
        }

        public void addChild(FileTreeNode child) {
            this.children.add(child);
        }

        // Getters and setters
        public String getName() { return name; }
        public String getType() { return type; }
        public CoverageData getData() { return data; }
        public List<FileTreeNode> getChildren() { return children; }
        public String getNodeType() { return nodeType; }
        public String getPackageName() { return packageName; }
        public boolean isFlattened() { return isFlattened; }
        public boolean isDirectory() { return "DIRECTORY".equals(type); }
        public boolean isPackage() { return "PACKAGE".equals(nodeType); }
        public boolean hasChildren() { return !children.isEmpty(); }
        
        public void setNodeType(String nodeType) { this.nodeType = nodeType; }
        public void setPackageName(String packageName) { this.packageName = packageName; }
        public void setFlattened(boolean flattened) { this.isFlattened = flattened; }
        
        public double getLineCoverage() {
            if (data != null) {
                return data.getLineCoverage();
            }
            // For package nodes, calculate average coverage of children
            if (isPackage() && hasChildren()) {
                return children.stream()
                    .filter(child -> child.getData() != null)
                    .mapToDouble(FileTreeNode::getLineCoverage)
                    .average()
                    .orElse(0.0);
            }
            return 0.0;
        }
        
        public String getCoverageClass() {
            double coverage = getLineCoverage();
            if (coverage > 80) return "coverage-high";
            if (coverage > 50) return "coverage-medium";
            return "coverage-low";
        }
    }
}
