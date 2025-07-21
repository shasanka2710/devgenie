package com.org.devgenie.service.metadata;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Standalone Java Metadata Analyzer
 * Calculates code complexity, business logic patterns, and generates metadata
 */
@Service
public class MetadataAnalyzer {

    private static final String[] BUSINESS_KEYWORDS = {
            "process", "calculate", "validate", "approve", "reject", "create", "update",
            "delete", "save", "find", "search", "payment", "order", "customer",
            "account", "transaction", "service", "repository", "controller"
    };

    private static final String[] BUSINESS_ANNOTATIONS = {
            "Service", "Controller", "Component", "Repository", "Transactional",
            "RestController", "RequestMapping", "PostMapping", "GetMapping"
    };
    /**
     * Analyzes a Java file and extracts metadata including code complexity,
     * business logic patterns, and dependency impact.
     *
     * @param filePath Path to the Java file to analyze
     * @return FileMetadata object containing the analysis results
     * @throws IOException if the file cannot be read
     */

    public FileMetadata analyzeJavaFile(String filePath,String repositoryUrl, String branch) throws IOException {

        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new IOException("File not found: " + filePath);
        }

        // Parse the Java file
        JavaParser parser = new JavaParser();
        CompilationUnit cu = parser.parse(path).getResult()
                .orElseThrow(() -> new RuntimeException("Failed to parse Java file"));

        // Extract metadata
        String className = extractClassName(cu);
        String packageName = extractPackageName(cu);
        int lineCount = (int) Files.lines(path).count();

        // Calculate complexities
        CodeComplexity codeComplexity = calculateCodeComplexity(cu);
        BusinessComplexity businessComplexity = analyzeBusinessLogic(cu);
        DependencyImpact dependencyImpact = analyzeDependencies(cu, className);

        // Calculate risk score
        double riskScore = calculateRiskScore(codeComplexity, businessComplexity, dependencyImpact, lineCount);

        return FileMetadata.builder()
                .repositoryUrl(repositoryUrl)
                .branch(branch)
                .filePath(filePath)
                .className(className)
                .packageName(packageName)
                .lineCount(lineCount)
                .codeComplexity(codeComplexity)
                .businessComplexity(businessComplexity)
                .dependencyImpact(dependencyImpact)
                .riskScore(riskScore)
                .analyzedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Analyzes multiple Java files and extracts metadata for each file.
     *
     * @param filePaths List of paths to Java files to analyze
     * @return List of FileMetadata objects containing the analysis results for each file
     */
    public List<FileMetadata> analyzeJavaFiles(List<String> filePaths,String repositoryUrl, String branch) {
        List<FileMetadata> results = new ArrayList<>();

        for (String filePath : filePaths) {
            try {
                FileMetadata metadata = analyzeJavaFile(filePath,repositoryUrl,branch);
                results.add(metadata);
                System.out.println("✅ Successfully analyzed: " + filePath);
            } catch (IOException e) {
                System.err.println("❌ Failed to analyze file: " + filePath + " - " + e.getMessage());
                // Continue with other files even if one fails
            }
        }

        return results;
    }

    private String extractClassName(CompilationUnit cu) {
        return cu.findFirst(ClassOrInterfaceDeclaration.class)
                .map(cls -> cls.getNameAsString())
                .orElse("Unknown");
    }

    private String extractPackageName(CompilationUnit cu) {
        return cu.getPackageDeclaration()
                .map(pd -> pd.getNameAsString())
                .orElse("default");
    }

    private CodeComplexity calculateCodeComplexity(CompilationUnit cu) {
        CodeComplexityVisitor visitor = new CodeComplexityVisitor();
        cu.accept(visitor, null);

        return CodeComplexity.builder()
                .cyclomaticComplexity(visitor.getCyclomaticComplexity())
                .cognitiveComplexity(visitor.getCognitiveComplexity())
                .totalMethods(visitor.getMethodCount())
                .averageMethodLength(visitor.getAverageMethodLength())
                .maxNestingDepth(visitor.getMaxNestingDepth())
                .complexMethods(visitor.getComplexMethods())
                .build();
    }

    private BusinessComplexity analyzeBusinessLogic(CompilationUnit cu) {
        BusinessLogicVisitor visitor = new BusinessLogicVisitor();
        cu.accept(visitor, null);

        return BusinessComplexity.builder()
                .businessCriticality(visitor.getBusinessCriticality())
                .hasBusinessAnnotations(visitor.hasBusinessAnnotations())
                .businessMethodCount(visitor.getBusinessMethodCount())
                .validationComplexity(visitor.getValidationComplexity())
                .transactionHandling(visitor.hasTransactionHandling())
                .exceptionHandling(visitor.hasExceptionHandling())
                .businessMethods(visitor.getBusinessMethods())
                .build();
    }

    private DependencyImpact analyzeDependencies(CompilationUnit cu, String className) {
        Set<String> outgoingDeps = new HashSet<>();

        // Analyze imports
        for (ImportDeclaration imp : cu.getImports()) {
            String importName = imp.getNameAsString();
            if (isProjectClass(importName)) {
                outgoingDeps.add(importName);
            }
        }

        // For this standalone analyzer, we can't calculate incoming dependencies
        // without analyzing the entire project
        Set<String> incomingDeps = new HashSet<>(); // Would need project-wide analysis

        int fanOut = outgoingDeps.size();
        int fanIn = incomingDeps.size();
        double instability = fanOut + fanIn == 0 ? 0 : (double) fanOut / (fanOut + fanIn);

        return DependencyImpact.builder()
                .outgoingDependencies(outgoingDeps)
                .incomingDependencies(incomingDeps)
                .fanOut(fanOut)
                .fanIn(fanIn)
                .instability(instability)
                .build();
    }

    private boolean isProjectClass(String importName) {
        // Simple heuristic: if it doesn't start with java., javax., org.springframework, etc.
        return !importName.startsWith("java.") &&
                !importName.startsWith("javax.") &&
                !importName.startsWith("org.springframework") &&
                !importName.startsWith("org.slf4j") &&
                !importName.startsWith("lombok");
    }

    private double calculateRiskScore(CodeComplexity code, BusinessComplexity business,
                                      DependencyImpact dep, int lineCount) {
        double complexityScore = (code.getCyclomaticComplexity() * 0.3) +
                (code.getCognitiveComplexity() * 0.2) +
                (lineCount > 500 ? 0.2 : 0.1);

        double businessScore = business.getBusinessCriticality() * 0.25;
        double dependencyScore = (dep.getFanOut() > 10) ? 0.15 : 0.05;

        return Math.min(complexityScore + businessScore + dependencyScore, 100.0);
    }

    /**
     * Prints detailed analysis for a single file metadata
     *
     * @param metadata The FileMetadata object to print analysis for
     */
    public void printAnalysis(FileMetadata metadata) {
        printDetailedAnalysis(metadata);
    }

    /**
     * Prints detailed analysis for multiple file metadata objects
     *
     * @param metadataList List of FileMetadata objects to print analysis for
     */
    public void printAnalysis(List<FileMetadata> metadataList) {
        for (FileMetadata metadata : metadataList) {
            printDetailedAnalysis(metadata);
            System.out.println("\n" + "=".repeat(80) + "\n");
        }
    }

    private void printDetailedAnalysis(FileMetadata metadata) {
        System.out.println("╔══════════════════════════════════════════════════════════════════╗");
        System.out.println("║                    JAVA FILE METADATA ANALYSIS                  ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════╝");
        System.out.println();

        // Basic Information
        System.out.println("📁 FILE INFORMATION");
        System.out.println("   Class Name:    " + metadata.getClassName());
        System.out.println("   Package:       " + metadata.getPackageName());
        System.out.println("   File Path:     " + metadata.getFilePath());
        System.out.println("   Line Count:    " + metadata.getLineCount());
        System.out.println("   Analyzed At:   " + metadata.getAnalyzedAt());
        System.out.println();

        // Code Complexity
        CodeComplexity code = metadata.getCodeComplexity();
        System.out.println("🔢 CODE COMPLEXITY METRICS");
        System.out.println("   Cyclomatic Complexity:    " + code.getCyclomaticComplexity() + " " + getComplexityRating(code.getCyclomaticComplexity()));
        System.out.println("   Cognitive Complexity:     " + code.getCognitiveComplexity() + " " + getCognitiveRating(code.getCognitiveComplexity()));
        System.out.println("   Total Methods:            " + code.getTotalMethods());
        System.out.println("   Average Method Length:    " + String.format("%.1f", code.getAverageMethodLength()) + " lines");
        System.out.println("   Max Nesting Depth:        " + code.getMaxNestingDepth());
        System.out.println();

        if (!code.getComplexMethods().isEmpty()) {
            System.out.println("   🚨 COMPLEX METHODS:");
            for (MethodComplexity method : code.getComplexMethods()) {
                System.out.println("      • " + method.getName() + "(): " +
                        method.getCyclomaticComplexity() + " cyclomatic, " +
                        method.getCognitiveComplexity() + " cognitive, " +
                        method.getLineCount() + " lines");
            }
            System.out.println();
        }

        // Business Complexity
        BusinessComplexity business = metadata.getBusinessComplexity();
        System.out.println("💼 BUSINESS LOGIC ANALYSIS");
        System.out.println("   Business Criticality:     " + String.format("%.1f", business.getBusinessCriticality()) + "/10.0");
        System.out.println("   Has Business Annotations: " + (business.isHasBusinessAnnotations() ? "✅ Yes" : "❌ No"));
        System.out.println("   Business Method Count:    " + business.getBusinessMethodCount());
        System.out.println("   Validation Complexity:    " + business.getValidationComplexity());
        System.out.println("   Transaction Handling:     " + (business.isTransactionHandling() ? "✅ Yes" : "❌ No"));
        System.out.println("   Exception Handling:       " + (business.isExceptionHandling() ? "✅ Yes" : "❌ No"));
        System.out.println();

        if (!business.getBusinessMethods().isEmpty()) {
            System.out.println("   📋 BUSINESS METHODS:");
            for (String method : business.getBusinessMethods()) {
                System.out.println("      • " + method);
            }
            System.out.println();
        }

        // Dependency Impact
        DependencyImpact dep = metadata.getDependencyImpact();
        System.out.println("🔗 DEPENDENCY ANALYSIS");
        System.out.println("   Fan-Out (Outgoing):       " + dep.getFanOut());
        System.out.println("   Fan-In (Incoming):        " + dep.getFanIn() + " (requires project-wide analysis)");
        System.out.println("   Instability:              " + String.format("%.3f", dep.getInstability()));
        System.out.println();

        if (!dep.getOutgoingDependencies().isEmpty()) {
            System.out.println("   📤 OUTGOING DEPENDENCIES:");
            for (String dependency : dep.getOutgoingDependencies()) {
                System.out.println("      • " + dependency);
            }
            System.out.println();
        }

        // Risk Assessment
        System.out.println("⚠️  RISK ASSESSMENT");
        System.out.println("   Overall Risk Score:       " + String.format("%.2f", metadata.getRiskScore()) + "/100.0 " + getRiskRating(metadata.getRiskScore()));
        System.out.println();

        // Recommendations
        System.out.println("📋 RECOMMENDATIONS");
        generateRecommendations(metadata).forEach(rec -> System.out.println("   • " + rec));
    }

    private String getComplexityRating(int complexity) {
        if (complexity <= 10) return "(🟢 Low)";
        if (complexity <= 20) return "(🟡 Medium)";
        if (complexity <= 50) return "(🟠 High)";
        return "(🔴 Very High)";
    }

    private String getCognitiveRating(int cognitive) {
        if (cognitive <= 15) return "(🟢 Low)";
        if (cognitive <= 25) return "(🟡 Medium)";
        if (cognitive <= 50) return "(🟠 High)";
        return "(🔴 Very High)";
    }

    private String getRiskRating(double risk) {
        if (risk <= 20) return "(🟢 Low Risk)";
        if (risk <= 40) return "(🟡 Medium Risk)";
        if (risk <= 70) return "(🟠 High Risk)";
        return "(🔴 Critical Risk)";
    }

    private List<String> generateRecommendations(FileMetadata metadata) {
        List<String> recommendations = new ArrayList<>();

        CodeComplexity code = metadata.getCodeComplexity();
        BusinessComplexity business = metadata.getBusinessComplexity();

        if (code.getCyclomaticComplexity() > 20) {
            recommendations.add("HIGH PRIORITY: Reduce cyclomatic complexity through method extraction");
        }

        if (code.getCognitiveComplexity() > 25) {
            recommendations.add("HIGH PRIORITY: Simplify complex logic to improve readability");
        }

        if (metadata.getLineCount() > 500) {
            recommendations.add("MEDIUM PRIORITY: Consider breaking down large class into smaller components");
        }

        if (business.isHasBusinessAnnotations() && business.getBusinessMethodCount() == 0) {
            recommendations.add("REVIEW: Business class with no business methods detected");
        }

        if (!business.isExceptionHandling() && business.getBusinessCriticality() > 7.0) {
            recommendations.add("HIGH PRIORITY: Add proper exception handling for business-critical class");
        }

        if (code.getMaxNestingDepth() > 4) {
            recommendations.add("MEDIUM PRIORITY: Reduce nesting depth through guard clauses");
        }

        if (recommendations.isEmpty()) {
            recommendations.add("✅ Code quality looks good! Consider adding comprehensive unit tests.");
        }

        return recommendations;
    }

    // Visitor classes for complexity calculation
    private static class CodeComplexityVisitor extends VoidVisitorAdapter<Void> {
        private int cyclomaticComplexity = 1; // Base complexity
        private int methodCount = 0;
        private int totalMethodLines = 0;
        private int maxNestingDepth = 0;
        private int currentNestingDepth = 0;
        private List<MethodComplexity> complexMethods = new ArrayList<>();
        private int totalCognitiveComplexity = 0; // Sum of all method cognitive complexities

        // Method-level tracking
        private boolean inMethod = false;
        private int methodCyclomatic = 0;
        private int methodCognitive = 0;
        private int methodLineCount = 0;
        private String currentMethodName = "";

        @Override
        public void visit(MethodDeclaration n, Void arg) {
            inMethod = true;
            methodCount++;
            methodCyclomatic = 1; // Base complexity for method
            methodCognitive = 0;
            currentMethodName = n.getNameAsString();

            int startLine = n.getBegin().map(pos -> pos.line).orElse(0);
            int endLine = n.getEnd().map(pos -> pos.line).orElse(0);
            methodLineCount = endLine - startLine + 1;
            totalMethodLines += methodLineCount;

            super.visit(n, arg);

            // Add method cognitive complexity to total
            totalCognitiveComplexity += methodCognitive;

            // Store complex methods (threshold: cyclomatic > 10 or cognitive > 15)
            if (methodCyclomatic > 10 || methodCognitive > 15) {
                complexMethods.add(MethodComplexity.builder()
                        .name(currentMethodName)
                        .cyclomaticComplexity(methodCyclomatic)
                        .cognitiveComplexity(methodCognitive)
                        .lineCount(methodLineCount)
                        .build());
            }

            inMethod = false;
        }

        @Override
        public void visit(IfStmt n, Void arg) {
            incrementComplexity(1);
            currentNestingDepth++;
            maxNestingDepth = Math.max(maxNestingDepth, currentNestingDepth);
            super.visit(n, arg);
            currentNestingDepth--;
        }

        @Override
        public void visit(WhileStmt n, Void arg) {
            incrementComplexity(1);
            currentNestingDepth++;
            maxNestingDepth = Math.max(maxNestingDepth, currentNestingDepth);
            super.visit(n, arg);
            currentNestingDepth--;
        }

        @Override
        public void visit(ForStmt n, Void arg) {
            incrementComplexity(1);
            currentNestingDepth++;
            maxNestingDepth = Math.max(maxNestingDepth, currentNestingDepth);
            super.visit(n, arg);
            currentNestingDepth--;
        }

        @Override
        public void visit(ForEachStmt n, Void arg) {
            incrementComplexity(1);
            currentNestingDepth++;
            maxNestingDepth = Math.max(maxNestingDepth, currentNestingDepth);
            super.visit(n, arg);
            currentNestingDepth--;
        }

        @Override
        public void visit(SwitchStmt n, Void arg) {
            incrementComplexity(n.getEntries().size());
            currentNestingDepth++;
            maxNestingDepth = Math.max(maxNestingDepth, currentNestingDepth);
            super.visit(n, arg);
            currentNestingDepth--;
        }

        @Override
        public void visit(CatchClause n, Void arg) {
            incrementComplexity(1);
            super.visit(n, arg);
        }

        @Override
        public void visit(ConditionalExpr n, Void arg) {
            incrementComplexity(1);
            super.visit(n, arg);
        }

        private void incrementComplexity(int increment) {
            cyclomaticComplexity += increment;
            if (inMethod) {
                methodCyclomatic += increment;
                // Cognitive complexity adds nesting penalty (each level of nesting adds +1)
                methodCognitive += increment + Math.max(currentNestingDepth - 1, 0);
            }
            // Global cognitive complexity is sum of all method cognitive complexities
        }

        public int getCyclomaticComplexity() { return cyclomaticComplexity; }

        public int getCognitiveComplexity() {
            // Return the total cognitive complexity from all methods
            return totalCognitiveComplexity;
        }

        public int getMethodCount() { return methodCount; }
        public double getAverageMethodLength() {
            return methodCount > 0 ? (double) totalMethodLines / methodCount : 0;
        }
        public int getMaxNestingDepth() { return maxNestingDepth; }
        public List<MethodComplexity> getComplexMethods() { return complexMethods; }
    }

    private static class BusinessLogicVisitor extends VoidVisitorAdapter<Void> {
        private double businessCriticality = 0.0;
        private boolean hasBusinessAnnotations = false;
        private int businessMethodCount = 0;
        private int validationComplexity = 0;
        private boolean hasTransactionHandling = false;
        private boolean hasExceptionHandling = false;
        private List<String> businessMethods = new ArrayList<>();

        @Override
        public void visit(ClassOrInterfaceDeclaration n, Void arg) {
            // Check for business annotations
            for (AnnotationExpr annotation : n.getAnnotations()) {
                String annName = annotation.getNameAsString();
                if (Arrays.stream(BUSINESS_ANNOTATIONS).anyMatch(ba -> ba.equals(annName))) {
                    hasBusinessAnnotations = true;
                    businessCriticality += 2.0;
                }
            }
            super.visit(n, arg);
        }

        @Override
        public void visit(MethodDeclaration n, Void arg) {
            String methodName = n.getNameAsString().toLowerCase();

            // Check if method name indicates business logic
            if (Arrays.stream(BUSINESS_KEYWORDS).anyMatch(keyword -> methodName.contains(keyword))) {
                businessMethodCount++;
                businessMethods.add(n.getNameAsString());
                businessCriticality += 1.0;
            }

            // Check for validation logic
            if (hasValidationLogic(n)) {
                validationComplexity++;
                businessCriticality += 0.5;
            }

            // Check for transaction annotations
            for (AnnotationExpr annotation : n.getAnnotations()) {
                String annName = annotation.getNameAsString();
                if (annName.contains("Transactional")) {
                    hasTransactionHandling = true;
                    businessCriticality += 1.5;
                }
            }

            super.visit(n, arg);
        }

        @Override
        public void visit(TryStmt n, Void arg) {
            hasExceptionHandling = true;
            super.visit(n, arg);
        }

        @Override
        public void visit(ThrowStmt n, Void arg) {
            hasExceptionHandling = true;
            super.visit(n, arg);
        }

        private boolean hasValidationLogic(MethodDeclaration method) {
            return method.getBody().map(body -> {
                String bodyStr = body.toString();
                return bodyStr.contains("if") &&
                        (bodyStr.contains("throw") || bodyStr.contains("return false")) &&
                        (bodyStr.contains("null") || bodyStr.contains("empty") ||
                                bodyStr.contains("invalid") || bodyStr.contains("validate"));
            }).orElse(false);
        }

        public double getBusinessCriticality() {
            return Math.min(businessCriticality, 10.0); // Cap at 10.0
        }
        public boolean hasBusinessAnnotations() { return hasBusinessAnnotations; }
        public int getBusinessMethodCount() { return businessMethodCount; }
        public int getValidationComplexity() { return validationComplexity; }
        public boolean hasTransactionHandling() { return hasTransactionHandling; }
        public boolean hasExceptionHandling() { return hasExceptionHandling; }
        public List<String> getBusinessMethods() { return businessMethods; }
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    // Data classes
    public static class FileMetadata {
        private String repositoryUrl;
        private String branch;
        private String filePath;
        private String className;
        private String packageName;
        private int lineCount;
        private CodeComplexity codeComplexity;
        private BusinessComplexity businessComplexity;
        private DependencyImpact dependencyImpact;
        private double riskScore;
        private LocalDateTime analyzedAt;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CodeComplexity {
        private int cyclomaticComplexity;
        private int cognitiveComplexity;
        private int totalMethods;
        private double averageMethodLength;
        private int maxNestingDepth;
        private List<MethodComplexity> complexMethods;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MethodComplexity {
        private String name;
        private int cyclomaticComplexity;
        private int cognitiveComplexity;
        private int lineCount;
    }
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BusinessComplexity {
        private double businessCriticality;
        private boolean hasBusinessAnnotations;
        private int businessMethodCount;
        private int validationComplexity;
        private boolean transactionHandling;
        private boolean exceptionHandling;
        private List<String> businessMethods;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DependencyImpact {
        private Set<String> outgoingDependencies;
        private Set<String> incomingDependencies;
        private int fanOut;
        private int fanIn;
        private double instability;
    }
}
