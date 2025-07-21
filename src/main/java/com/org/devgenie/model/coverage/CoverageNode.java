package com.org.devgenie.model.coverage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "coverage_nodes")
@CompoundIndexes({
    @CompoundIndex(name = "repo_branch_idx", def = "{'repoPath': 1, 'branch': 1}"),
    @CompoundIndex(name = "parent_path_idx", def = "{'parentPathRepo': 1, 'type': 1}"),
    @CompoundIndex(name = "depth_path_idx", def = "{'repoPathBranch': 1, 'depth': 1, 'type': 1}"),
    @CompoundIndex(name = "coverage_performance_idx", def = "{'repoPathBranch': 1, 'lineCoverage': -1, 'type': 1}")
})
public class CoverageNode {
    
    @Id
    private String id;
    
    // Repository identification
    private String repoPath;
    private String branch;
    
    // Path and hierarchy information
    private String fullPath;        // "src/main/java/com/org/service/UserService.java"
    private String parentPath;      // "src/main/java/com/org/service"
    private String name;            // "UserService.java"
    private String type;            // "FILE" or "DIRECTORY"
    private int depth;              // 5 (for efficient level queries)
    
    // Coverage metrics (populated only for files)
    private Double lineCoverage;
    private Double branchCoverage;
    private Double methodCoverage;
    private Integer totalLines;
    private Integer coveredLines;
    private Integer totalBranches;
    private Integer coveredBranches;
    private Integer totalMethods;
    private Integer coveredMethods;
    
    // Directory-specific computed metrics
    private Integer childFileCount;
    private Integer childDirCount;
    private Double aggregatedLineCoverage;    // Computed from all child files
    private Double aggregatedBranchCoverage;  // Computed from all child files
    private Double aggregatedMethodCoverage;  // Computed from all child files
    private Integer totalChildLines;          // Sum of all descendant file lines
    private Integer totalCoveredChildLines;   // Sum of all descendant covered lines
    
    // Metadata
    private LocalDateTime lastUpdated;
    private String packageName;     // Only for Java files
    private String className;       // Only for Java files
    private List<String> tags;      // For categorization (e.g., "controller", "service", "test")
    
    // Performance optimization indexes
    @Indexed
    private String repoPathBranch;  // "repoPath:branch" for compound queries
    
    @Indexed
    private String parentPathRepo;  // "parentPath:repoPath:branch" for children queries
    
    // Improvement opportunities (denormalized for performance)
    private List<ImprovementOpportunity> improvementOpportunities;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImprovementOpportunity {
        private String type;           // "UNCOVERED_BRANCH", "MISSING_TESTS", etc.
        private String description;
        private String priority;       // "HIGH", "MEDIUM", "LOW"
        private String estimatedImpact;
        private Integer lineNumber;    // Specific line if applicable
        private String suggestion;     // AI-generated suggestion
    }
    
    // Helper methods for hierarchy navigation
    public boolean isFile() {
        return "FILE".equals(this.type);
    }
    
    public boolean isDirectory() {
        return "DIRECTORY".equals(this.type);
    }
    
    public boolean isRootDirectory() {
        return parentPath == null || parentPath.isEmpty();
    }
    
    public String getFileExtension() {
        if (!isFile() || name == null) {
            return null;
        }
        int lastDot = name.lastIndexOf('.');
        return lastDot > 0 ? name.substring(lastDot + 1) : null;
    }
    
    public boolean isJavaFile() {
        return "java".equalsIgnoreCase(getFileExtension());
    }
    
    public boolean isTestFile() {
        return isJavaFile() && (
            fullPath.contains("/test/") || 
            name.endsWith("Test.java") || 
            name.endsWith("Tests.java")
        );
    }
    
    // Coverage classification helpers
    public String getCoverageLevel() {
        if (lineCoverage == null) {
            return "UNKNOWN";
        }
        if (lineCoverage >= 80) return "HIGH";
        if (lineCoverage >= 50) return "MEDIUM";
        return "LOW";
    }
    
    public boolean needsAttention() {
        return lineCoverage != null && lineCoverage < 50;
    }
}
