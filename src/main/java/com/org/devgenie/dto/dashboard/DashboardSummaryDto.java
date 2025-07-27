package com.org.devgenie.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryDto {
    
    // Top-level metrics (RBGY cards)
    private MetricCard totalSessions;
    private MetricCard successRate;
    private MetricCard averageCoverageIncrease;
    private MetricCard timeSaved;
    
    // Recent activity
    private List<ActivityItem> recentActivity;
    
    // Analytics data
    private AnalyticsData analytics;
    
    // In-progress sessions
    private List<ActiveSessionDto> activeSessions;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetricCard {
        private String title;
        private String value;
        private String subtitle;
        private String icon;
        private String color; // red, blue, green, yellow
        private Double changePercentage;
        private String changeDirection; // up, down, stable
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActivityItem {
        private String sessionId;
        private String type; // FILE_IMPROVEMENT, REPOSITORY_IMPROVEMENT
        private String status;
        private String description;
        private String timestamp;
        private String filePath;
        private Double coverageIncrease;
        private Integer testsGenerated;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnalyticsData {
        private List<TrendData> coverageTrends;
        private List<TrendData> successRateTrends;
        private List<CategoryData> improvementsByCategory;
        private List<ComplexityData> complexityAnalysis;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendData {
        private String date;
        private Double value;
        private String label;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryData {
        private String category;
        private Integer count;
        private Double averageImprovement;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComplexityData {
        private String complexityLevel; // LOW, MEDIUM, HIGH
        private Integer fileCount;
        private Double averageImprovement;
        private Double averageTimeMs;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActiveSessionDto {
        private String sessionId;
        private String type;
        private String status;
        private String filePath;
        private Double progress;
        private String currentStep;
        private String startedAt;
        private Long estimatedTimeRemaining;
        private Boolean canModify;
    }
}
