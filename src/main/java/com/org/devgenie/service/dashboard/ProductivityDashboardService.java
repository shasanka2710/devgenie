package com.org.devgenie.service.dashboard;

import com.org.devgenie.dto.dashboard.DashboardFilterDto;
import com.org.devgenie.dto.dashboard.DashboardSummaryDto;
import com.org.devgenie.dto.dashboard.ImprovementRecordDto;
import com.org.devgenie.model.coverage.CoverageImprovementSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ProductivityDashboardService {

    @Autowired
    private MongoTemplate mongoTemplate;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public DashboardSummaryDto getDashboardSummary(String repositoryUrl, String branch, DashboardFilterDto filter) {
        try {
            log.info("Generating dashboard summary for repo: {} branch: {} filter: {}", repositoryUrl, branch, filter);

            // Build query criteria
            Query query = buildQuery(repositoryUrl, branch, filter);

            // Get all sessions for analysis
            List<CoverageImprovementSession> sessions = mongoTemplate.find(query, CoverageImprovementSession.class);
            log.info("Found {} sessions for dashboard", sessions.size());

            return DashboardSummaryDto.builder()
                    .totalSessions(buildTotalSessionsCard(sessions))
                    .successRate(buildSuccessRateCard(sessions))
                    .averageCoverageIncrease(buildCoverageIncreaseCard(sessions))
                    .timeSaved(buildTimeSavedCard(sessions))
                    .recentActivity(buildRecentActivity(sessions, 10))
                    .analytics(buildAnalyticsData(sessions))
                    .activeSessions(buildActiveSessions(sessions))
                    .build();

        } catch (Exception e) {
            log.error("Error generating dashboard summary", e);
            return getDefaultDashboardSummary();
        }
    }

    public Page<ImprovementRecordDto> getImprovementRecords(String repositoryUrl, String branch, DashboardFilterDto filter) {
        try {
            log.info("Fetching improvement records for repo: {} branch: {} filter: {}", repositoryUrl, branch, filter);

            // Build query and pagination
            Query query = buildQuery(repositoryUrl, branch, filter);
            Pageable pageable = buildPageable(filter);
            query.with(pageable);

            // Execute query
            List<CoverageImprovementSession> sessions = mongoTemplate.find(query, CoverageImprovementSession.class);
            long total = mongoTemplate.count(Query.of(query).limit(-1).skip(-1), CoverageImprovementSession.class);

            log.info("Found {} sessions, total: {}", sessions.size(), total);

            // Convert to DTOs and filter out null records
            List<ImprovementRecordDto> records = sessions.stream()
                    .filter(Objects::nonNull) // Filter out null sessions
                    .map(this::convertToImprovementRecord)
                    .filter(Objects::nonNull) // Filter out failed conversions
                    .collect(Collectors.toList());

            return PageableExecutionUtils.getPage(records, pageable, () -> total);

        } catch (Exception e) {
            log.error("Error fetching improvement records", e);
            return Page.empty();
        }
    }

    private Query buildQuery(String repositoryUrl, String branch, DashboardFilterDto filter) {
        Criteria criteria = Criteria.where("repositoryUrl").is(repositoryUrl).and("branch").is(branch);

        // Filter by category (type)
        if (filter != null && !"ALL".equals(filter.getCategory())) {
            if ("COVERAGE".equals(filter.getCategory())) {
                criteria.and("type").in("FILE_IMPROVEMENT", "REPOSITORY_IMPROVEMENT");
            }
            // Add more categories when implemented (ISSUES, etc.)
        }

        // Filter by sub-category
        if (filter != null && !"ALL".equals(filter.getSubCategory())) {
            if ("FILE_LEVEL".equals(filter.getSubCategory())) {
                criteria.and("type").is("FILE_IMPROVEMENT");
            } else if ("REPOSITORY_LEVEL".equals(filter.getSubCategory())) {
                criteria.and("type").is("REPOSITORY_IMPROVEMENT");
            }
        }

        // Filter by status
        if (filter != null && !"ALL".equals(filter.getStatus())) {
            if ("COMPLETED".equals(filter.getStatus())) {
                criteria.and("status").in("COMPLETED", "READY_FOR_REVIEW");
            } else if ("IN_PROGRESS".equals(filter.getStatus())) {
                criteria.and("status").in("PROCESSING", "ANALYZING", "GENERATING_TESTS");
            } else if ("FAILED".equals(filter.getStatus())) {
                criteria.and("status").in("FAILED", "ERROR");
            }
        }

        // Filter by time range
        if (filter != null && !"ALL_TIME".equals(filter.getTimeRange())) {
            LocalDateTime cutoff = calculateTimeRangeCutoff(filter.getTimeRange());
            criteria.and("startedAt").gte(cutoff);
        }

        return new Query(criteria);
    }

    private Pageable buildPageable(DashboardFilterDto filter) {
        int page = filter != null && filter.getPage() != null ? filter.getPage() : 0;
        int size = filter != null && filter.getSize() != null ? filter.getSize() : 20;

        Sort sort = Sort.by(Sort.Direction.DESC, "startedAt"); // Default sort
        if (filter != null && filter.getSortBy() != null) {
            switch (filter.getSortBy()) {
                case "DATE_ASC":
                    sort = Sort.by(Sort.Direction.ASC, "startedAt");
                    break;
                case "COVERAGE_DESC":
                    sort = Sort.by(Sort.Direction.DESC, "results.coverageIncrease");
                    break;
                case "COVERAGE_ASC":
                    sort = Sort.by(Sort.Direction.ASC, "results.coverageIncrease");
                    break;
                default:
                    sort = Sort.by(Sort.Direction.DESC, "startedAt");
            }
        }

        return PageRequest.of(page, size, sort);
    }

    private LocalDateTime calculateTimeRangeCutoff(String timeRange) {
        LocalDateTime now = LocalDateTime.now();
        switch (timeRange) {
            case "LAST_7_DAYS":
                return now.minusDays(7);
            case "LAST_30_DAYS":
                return now.minusDays(30);
            case "LAST_90_DAYS":
                return now.minusDays(90);
            default:
                return LocalDateTime.of(2020, 1, 1, 0, 0); // Far past
        }
    }

    private DashboardSummaryDto.MetricCard buildTotalSessionsCard(List<CoverageImprovementSession> sessions) {
        int total = sessions.size();
        int lastWeekCount = (int) sessions.stream()
                .filter(s -> s.getStartedAt().isAfter(LocalDateTime.now().minusDays(7)))
                .count();

        double changePercentage = calculateWeeklyChange(sessions, s -> 1);

        return DashboardSummaryDto.MetricCard.builder()
                .title("Total Sessions")
                .value(String.valueOf(total))
                .subtitle(lastWeekCount + " this week")
                .icon("fas fa-chart-line")
                .color("blue")
                .changePercentage(changePercentage)
                .changeDirection(changePercentage >= 0 ? "up" : "down")
                .build();
    }

    private DashboardSummaryDto.MetricCard buildSuccessRateCard(List<CoverageImprovementSession> sessions) {
        long completed = sessions.stream()
                .filter(s -> Arrays.asList("COMPLETED", "READY_FOR_REVIEW").contains(s.getStatus().toString()))
                .count();

        double successRate = sessions.isEmpty() ? 0 : (completed * 100.0) / sessions.size();
        double changePercentage = calculateWeeklySuccessRateChange(sessions);

        return DashboardSummaryDto.MetricCard.builder()
                .title("Success Rate")
                .value(String.format("%.1f%%", successRate))
                .subtitle("Completion rate")
                .icon("fas fa-check-circle")
                .color("green")
                .changePercentage(changePercentage)
                .changeDirection(changePercentage >= 0 ? "up" : "down")
                .build();
    }

    private DashboardSummaryDto.MetricCard buildCoverageIncreaseCard(List<CoverageImprovementSession> sessions) {
        double avgIncrease = sessions.stream()
                .filter(s -> s.getResults() != null)
                .mapToDouble(this::extractCoverageIncrease)
                .filter(coverage -> coverage > 0)
                .average()
                .orElse(0.0);

        double changePercentage = calculateWeeklyCoverageChange(sessions);

        return DashboardSummaryDto.MetricCard.builder()
                .title("Avg Coverage Increase")
                .value(String.format("%.1f%%", avgIncrease))
                .subtitle("Per improvement")
                .icon("fas fa-arrow-up")
                .color("red")
                .changePercentage(changePercentage)
                .changeDirection(changePercentage >= 0 ? "up" : "down")
                .build();
    }

    private DashboardSummaryDto.MetricCard buildTimeSavedCard(List<CoverageImprovementSession> sessions) {
        // Estimate: Each generated test saves ~15 minutes of manual work
        int totalTests = sessions.stream()
                .filter(s -> s.getResults() != null)
                .mapToInt(this::extractTotalTests)
                .sum();

        double hoursSaved = (totalTests * 15) / 60.0; // Convert minutes to hours
        double changePercentage = calculateWeeklyTimeSavedChange(sessions);

        return DashboardSummaryDto.MetricCard.builder()
                .title("Time Saved")
                .value(String.format("%.1f hrs", hoursSaved))
                .subtitle("Estimated dev time")
                .icon("fas fa-clock")
                .color("yellow")
                .changePercentage(changePercentage)
                .changeDirection(changePercentage >= 0 ? "up" : "down")
                .build();
    }

    private List<DashboardSummaryDto.ActivityItem> buildRecentActivity(List<CoverageImprovementSession> sessions, int limit) {
        return sessions.stream()
                .filter(Objects::nonNull)
                .sorted((a, b) -> b.getStartedAt().compareTo(a.getStartedAt()))
                .limit(limit)
                .map(this::convertToActivityItem)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private DashboardSummaryDto.AnalyticsData buildAnalyticsData(List<CoverageImprovementSession> sessions) {
        return DashboardSummaryDto.AnalyticsData.builder()
                .coverageTrends(buildCoverageTrends(sessions))
                .successRateTrends(buildSuccessRateTrends(sessions))
                .improvementsByCategory(buildImprovementsByCategory(sessions))
                .complexityAnalysis(buildComplexityAnalysis(sessions))
                .build();
    }

    private List<DashboardSummaryDto.ActiveSessionDto> buildActiveSessions(List<CoverageImprovementSession> sessions) {
        return sessions.stream()
                .filter(Objects::nonNull)
                .filter(s -> Arrays.asList("PROCESSING", "ANALYZING", "GENERATING_TESTS", "CREATED", "INITIALIZING")
                        .contains(s.getStatus().toString()))
                .map(this::convertToActiveSession)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    // Helper methods for data extraction with improved error handling
    private double extractCoverageIncrease(CoverageImprovementSession session) {
        try {
            if (session == null || session.getResults() == null) {
                return 0.0;
            }

            if (session.getResults() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> results = (Map<String, Object>) session.getResults();
                if (results != null && results.containsKey("coverageIncrease")) {
                    Object coverage = results.get("coverageIncrease");
                    if (coverage instanceof Number) {
                        double value = ((Number) coverage).doubleValue();
                        log.debug("Extracted coverage increase {} for session {}", value, session.getSessionId());
                        return value;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Could not extract coverage increase from session: {}", session.getSessionId(), e);
        }
        return 0.0;
    }

    private int extractTotalTests(CoverageImprovementSession session) {
        try {
            if (session == null || session.getResults() == null) {
                return 0;
            }

            if (session.getResults() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> results = (Map<String, Object>) session.getResults();
                if (results != null && results.containsKey("totalTestsGenerated")) {
                    Object tests = results.get("totalTestsGenerated");
                    if (tests instanceof Number) {
                        int value = ((Number) tests).intValue();
                        log.debug("Extracted total tests {} for session {}", value, session.getSessionId());
                        return value;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Could not extract total tests from session: {}", session.getSessionId(), e);
        }
        return 0;
    }

    private long extractProcessingTime(CoverageImprovementSession session) {
        try {
            if (session == null || session.getResults() == null) {
                return 0L;
            }

            if (session.getResults() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> results = (Map<String, Object>) session.getResults();
                if (results != null && results.containsKey("processingTimeMs")) {
                    Object time = results.get("processingTimeMs");
                    if (time instanceof Number) {
                        return ((Number) time).longValue();
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Could not extract processing time from session: {}", session.getSessionId(), e);
        }
        return 0L;
    }

    // Calculation methods with null safety
    private double calculateWeeklyChange(List<CoverageImprovementSession> sessions, java.util.function.Function<CoverageImprovementSession, Integer> extractor) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime weekAgo = now.minusDays(7);
        LocalDateTime twoWeeksAgo = now.minusDays(14);

        int thisWeek = sessions.stream()
                .filter(Objects::nonNull)
                .filter(s -> s.getStartedAt() != null && s.getStartedAt().isAfter(weekAgo))
                .mapToInt(extractor::apply)
                .sum();

        int lastWeek = sessions.stream()
                .filter(Objects::nonNull)
                .filter(s -> s.getStartedAt() != null && s.getStartedAt().isAfter(twoWeeksAgo) && s.getStartedAt().isBefore(weekAgo))
                .mapToInt(extractor::apply)
                .sum();

        return lastWeek == 0 ? 0 : ((thisWeek - lastWeek) * 100.0) / lastWeek;
    }

    private double calculateWeeklySuccessRateChange(List<CoverageImprovementSession> sessions) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime weekAgo = now.minusDays(7);
        LocalDateTime twoWeeksAgo = now.minusDays(14);

        List<CoverageImprovementSession> thisWeek = sessions.stream()
                .filter(Objects::nonNull)
                .filter(s -> s.getStartedAt() != null && s.getStartedAt().isAfter(weekAgo))
                .collect(Collectors.toList());

        List<CoverageImprovementSession> lastWeek = sessions.stream()
                .filter(Objects::nonNull)
                .filter(s -> s.getStartedAt() != null && s.getStartedAt().isAfter(twoWeeksAgo) && s.getStartedAt().isBefore(weekAgo))
                .collect(Collectors.toList());

        double thisWeekRate = thisWeek.isEmpty() ? 0 :
                (thisWeek.stream().filter(s -> s.getStatus() != null && Arrays.asList("COMPLETED", "READY_FOR_REVIEW").contains(s.getStatus().toString())).count() * 100.0) / thisWeek.size();

        double lastWeekRate = lastWeek.isEmpty() ? 0 :
                (lastWeek.stream().filter(s -> s.getStatus() != null && Arrays.asList("COMPLETED", "READY_FOR_REVIEW").contains(s.getStatus().toString())).count() * 100.0) / lastWeek.size();

        return lastWeekRate == 0 ? 0 : thisWeekRate - lastWeekRate;
    }

    private double calculateWeeklyCoverageChange(List<CoverageImprovementSession> sessions) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime weekAgo = now.minusDays(7);
        LocalDateTime twoWeeksAgo = now.minusDays(14);

        double thisWeekAvg = sessions.stream()
                .filter(Objects::nonNull)
                .filter(s -> s.getStartedAt() != null && s.getStartedAt().isAfter(weekAgo))
                .filter(s -> s.getResults() != null)
                .mapToDouble(this::extractCoverageIncrease)
                .average()
                .orElse(0.0);

        double lastWeekAvg = sessions.stream()
                .filter(Objects::nonNull)
                .filter(s -> s.getStartedAt() != null && s.getStartedAt().isAfter(twoWeeksAgo) && s.getStartedAt().isBefore(weekAgo))
                .filter(s -> s.getResults() != null)
                .mapToDouble(this::extractCoverageIncrease)
                .average()
                .orElse(0.0);

        return lastWeekAvg == 0 ? 0 : thisWeekAvg - lastWeekAvg;
    }

    private double calculateWeeklyTimeSavedChange(List<CoverageImprovementSession> sessions) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime weekAgo = now.minusDays(7);
        LocalDateTime twoWeeksAgo = now.minusDays(14);

        int thisWeekTests = sessions.stream()
                .filter(Objects::nonNull)
                .filter(s -> s.getStartedAt() != null && s.getStartedAt().isAfter(weekAgo))
                .filter(s -> s.getResults() != null)
                .mapToInt(this::extractTotalTests)
                .sum();

        int lastWeekTests = sessions.stream()
                .filter(Objects::nonNull)
                .filter(s -> s.getStartedAt() != null && s.getStartedAt().isAfter(twoWeeksAgo) && s.getStartedAt().isBefore(weekAgo))
                .filter(s -> s.getResults() != null)
                .mapToInt(this::extractTotalTests)
                .sum();

        return lastWeekTests == 0 ? 0 : ((thisWeekTests - lastWeekTests) * 100.0) / lastWeekTests;
    }

    // Trend and analytics methods
    private List<DashboardSummaryDto.TrendData> buildCoverageTrends(List<CoverageImprovementSession> sessions) {
        Map<String, List<CoverageImprovementSession>> dailyGroups = sessions.stream()
                .filter(Objects::nonNull)
                .filter(s -> s.getResults() != null)
                .filter(s -> s.getStartedAt() != null && s.getStartedAt().isAfter(LocalDateTime.now().minusDays(30)))
                .collect(Collectors.groupingBy(s -> s.getStartedAt().toLocalDate().toString()));

        return dailyGroups.entrySet().stream()
                .map(entry -> DashboardSummaryDto.TrendData.builder()
                        .date(entry.getKey())
                        .value(entry.getValue().stream()
                                .mapToDouble(this::extractCoverageIncrease)
                                .average()
                                .orElse(0.0))
                        .label("Coverage Increase")
                        .build())
                .sorted(Comparator.comparing(DashboardSummaryDto.TrendData::getDate))
                .collect(Collectors.toList());
    }

    private List<DashboardSummaryDto.TrendData> buildSuccessRateTrends(List<CoverageImprovementSession> sessions) {
        Map<String, List<CoverageImprovementSession>> dailyGroups = sessions.stream()
                .filter(Objects::nonNull)
                .filter(s -> s.getStartedAt() != null && s.getStartedAt().isAfter(LocalDateTime.now().minusDays(30)))
                .collect(Collectors.groupingBy(s -> s.getStartedAt().toLocalDate().toString()));

        return dailyGroups.entrySet().stream()
                .map(entry -> {
                    List<CoverageImprovementSession> daySessions = entry.getValue();
                    long completed = daySessions.stream()
                            .filter(s -> s.getStatus() != null && Arrays.asList("COMPLETED", "READY_FOR_REVIEW").contains(s.getStatus().toString()))
                            .count();
                    double rate = daySessions.isEmpty() ? 0 : (completed * 100.0) / daySessions.size();

                    return DashboardSummaryDto.TrendData.builder()
                            .date(entry.getKey())
                            .value(rate)
                            .label("Success Rate")
                            .build();
                })
                .sorted(Comparator.comparing(DashboardSummaryDto.TrendData::getDate))
                .collect(Collectors.toList());
    }

    private List<DashboardSummaryDto.CategoryData> buildImprovementsByCategory(List<CoverageImprovementSession> sessions) {
        Map<String, List<CoverageImprovementSession>> typeGroups = sessions.stream()
                .filter(Objects::nonNull)
                .filter(s -> s.getResults() != null && s.getType() != null)
                .collect(Collectors.groupingBy(s -> s.getType().toString()));

        return typeGroups.entrySet().stream()
                .map(entry -> DashboardSummaryDto.CategoryData.builder()
                        .category(entry.getKey())
                        .count(entry.getValue().size())
                        .averageImprovement(entry.getValue().stream()
                                .mapToDouble(this::extractCoverageIncrease)
                                .average()
                                .orElse(0.0))
                        .build())
                .collect(Collectors.toList());
    }

    private List<DashboardSummaryDto.ComplexityData> buildComplexityAnalysis(List<CoverageImprovementSession> sessions) {
        List<CoverageImprovementSession> validSessions = sessions.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return Arrays.asList(
                DashboardSummaryDto.ComplexityData.builder()
                        .complexityLevel("LOW")
                        .fileCount((int) validSessions.stream().filter(s -> extractProcessingTime(s) < 30000).count())
                        .averageImprovement(validSessions.stream().filter(s -> extractProcessingTime(s) < 30000)
                                .mapToDouble(this::extractCoverageIncrease).average().orElse(0.0))
                        .averageTimeMs(validSessions.stream().filter(s -> extractProcessingTime(s) < 30000)
                                .mapToDouble(this::extractProcessingTime).average().orElse(0.0))
                        .build(),
                DashboardSummaryDto.ComplexityData.builder()
                        .complexityLevel("MEDIUM")
                        .fileCount((int) validSessions.stream().filter(s -> {
                            long time = extractProcessingTime(s);
                            return time >= 30000 && time < 90000;
                        }).count())
                        .averageImprovement(validSessions.stream().filter(s -> {
                            long time = extractProcessingTime(s);
                            return time >= 30000 && time < 90000;
                        }).mapToDouble(this::extractCoverageIncrease).average().orElse(0.0))
                        .averageTimeMs(validSessions.stream().filter(s -> {
                            long time = extractProcessingTime(s);
                            return time >= 30000 && time < 90000;
                        }).mapToDouble(this::extractProcessingTime).average().orElse(0.0))
                        .build(),
                DashboardSummaryDto.ComplexityData.builder()
                        .complexityLevel("HIGH")
                        .fileCount((int) validSessions.stream().filter(s -> extractProcessingTime(s) >= 90000).count())
                        .averageImprovement(validSessions.stream().filter(s -> extractProcessingTime(s) >= 90000)
                                .mapToDouble(this::extractCoverageIncrease).average().orElse(0.0))
                        .averageTimeMs(validSessions.stream().filter(s -> extractProcessingTime(s) >= 90000)
                                .mapToDouble(this::extractProcessingTime).average().orElse(0.0))
                        .build()
        );
    }

    // Conversion methods with null safety
    private DashboardSummaryDto.ActivityItem convertToActivityItem(CoverageImprovementSession session) {
        if (session == null) return null;

        try {
            return DashboardSummaryDto.ActivityItem.builder()
                    .sessionId(session.getSessionId())
                    .type(session.getType() != null ? session.getType().toString() : "UNKNOWN")
                    .status(session.getStatus() != null ? session.getStatus().toString() : "UNKNOWN")
                    .description(buildActivityDescription(session))
                    .timestamp(session.getStartedAt() != null ? session.getStartedAt().format(DATE_FORMATTER) : "Unknown")
                    .filePath(session.getFilePath())
                    .coverageIncrease(extractCoverageIncrease(session))
                    .testsGenerated(extractTotalTests(session))
                    .build();
        } catch (Exception e) {
            log.warn("Error converting session to activity item: {}", session.getSessionId(), e);
            return null;
        }
    }

    private String buildActivityDescription(CoverageImprovementSession session) {
        if (session == null || session.getType() == null) {
            return "Unknown improvement session";
        }

        if ("FILE_IMPROVEMENT".equals(session.getType().toString())) {
            return "Improved coverage for " + (session.getFilePath() != null ?
                    session.getFilePath().substring(session.getFilePath().lastIndexOf('/') + 1) : "file");
        } else if ("REPOSITORY_IMPROVEMENT".equals(session.getType().toString())) {
            return "Repository-wide coverage improvement";
        }
        return "Coverage improvement session";
    }

    private DashboardSummaryDto.ActiveSessionDto convertToActiveSession(CoverageImprovementSession session) {
        if (session == null) return null;

        try {
            return DashboardSummaryDto.ActiveSessionDto.builder()
                    .sessionId(session.getSessionId())
                    .type(session.getType() != null ? session.getType().toString() : "UNKNOWN")
                    .status(session.getStatus() != null ? session.getStatus().toString() : "UNKNOWN")
                    .filePath(session.getFilePath())
                    .progress(session.getProgress())
                    .currentStep(session.getCurrentStep())
                    .startedAt(session.getStartedAt() != null ? session.getStartedAt().format(DATE_FORMATTER) : "Unknown")
                    .estimatedTimeRemaining(calculateEstimatedTimeRemaining(session))
                    .canModify(canModifySession(session))
                    .build();
        } catch (Exception e) {
            log.warn("Error converting session to active session: {}", session.getSessionId(), e);
            return null;
        }
    }

    private Long calculateEstimatedTimeRemaining(CoverageImprovementSession session) {
        if (session == null || session.getProgress() == null || session.getProgress() <= 0 || session.getStartedAt() == null) {
            return null;
        }

        try {
            long elapsed = java.time.Duration.between(session.getStartedAt(), LocalDateTime.now()).toMillis();
            double progressDecimal = session.getProgress() / 100.0;
            if (progressDecimal >= 0.95) {
                return 0L; // Almost done
            }

            long totalEstimated = (long) (elapsed / progressDecimal);
            return totalEstimated - elapsed;
        } catch (Exception e) {
            log.warn("Error calculating estimated time for session: {}", session.getSessionId(), e);
            return null;
        }
    }

    private Boolean canModifySession(CoverageImprovementSession session) {
        if (session == null || session.getStatus() == null) {
            return false;
        }
        return Arrays.asList("CREATED", "FAILED", "ERROR").contains(session.getStatus().toString());
    }

    private ImprovementRecordDto convertToImprovementRecord(CoverageImprovementSession session) {
        if (session == null) {
            log.warn("Attempting to convert null session to improvement record");
            return null;
        }

        try {
            ImprovementRecordDto.ImprovementRecordDtoBuilder builder = ImprovementRecordDto.builder()
                    .sessionId(session.getSessionId())
                    .type(session.getType() != null ? session.getType().toString() : "UNKNOWN")
                    .status(session.getStatus() != null ? session.getStatus().toString() : "UNKNOWN")
                    .repositoryUrl(session.getRepositoryUrl())
                    .branch(session.getBranch())
                    .filePath(session.getFilePath())
                    .startedAt(session.getStartedAt() != null ? session.getStartedAt().format(DATE_FORMATTER) : "Unknown");

            // Extract data from results if available
            if (session.getResults() instanceof Map) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> results = (Map<String, Object>) session.getResults();
                    builder
                            .fileName(extractStringFromResults(results, "fileName"))
                            .originalCoverage(extractDoubleFromResults(results, "originalCoverage"))
                            .improvedCoverage(extractDoubleFromResults(results, "improvedCoverage"))
                            .coverageIncrease(extractDoubleFromResults(results, "coverageIncrease"))
                            .totalTestsGenerated(extractIntegerFromResults(results, "totalTestsGenerated"))
                            .processingTimeMs(extractLongFromResults(results, "processingTimeMs"))
                            .testsCompiled(extractBooleanFromResults(results, "testsCompiled"))
                            .testsExecuted(extractBooleanFromResults(results, "testsExecuted"));

                    // Extract completion time
                    String completedAt = extractStringFromResults(results, "completedAt");
                    if (completedAt != null) {
                        builder.completedAt(completedAt);
                    }

                    // Extract validation details
                    Object validationObj = results.get("validationResult");
                    if (validationObj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> validation = (Map<String, Object>) validationObj;
                        builder.validation(ImprovementRecordDto.ValidationDetails.builder()
                                .success(extractBooleanFromMap(validation, "success"))
                                .testsExecuted(extractIntegerFromMap(validation, "testsExecuted"))
                                .testsPassed(extractIntegerFromMap(validation, "testsPassed"))
                                .testsFailed(extractIntegerFromMap(validation, "testsFailed"))
                                .executionTimeMs(extractLongFromMap(validation, "executionTimeMs"))
                                .validationMethod(extractStringFromMap(validation, "validationMethod"))
                                .build());
                    }

                    // Extract lists
                    builder
                            .recommendations(extractStringListFromResults(results, "recommendations"))
                            .warnings(extractStringListFromResults(results, "warnings"))
                            .errors(extractStringListFromResults(results, "errors"))
                            .generatedTests(extractGeneratedTests(results));

                } catch (Exception e) {
                    log.warn("Error extracting results for session: {}", session.getSessionId(), e);
                }
            }

            return builder.build();

        } catch (Exception e) {
            log.error("Error converting session {} to improvement record", session.getSessionId(), e);
            return null;
        }
    }

    // Helper methods for extracting data from results map with null safety
    private String extractStringFromResults(Map<String, Object> results, String key) {
        if (results == null || key == null) return null;
        Object value = results.get(key);
        return value != null ? value.toString() : null;
    }

    private Double extractDoubleFromResults(Map<String, Object> results, String key) {
        if (results == null || key == null) return null;
        Object value = results.get(key);
        return value instanceof Number ? ((Number) value).doubleValue() : null;
    }

    private Integer extractIntegerFromResults(Map<String, Object> results, String key) {
        if (results == null || key == null) return null;
        Object value = results.get(key);
        return value instanceof Number ? ((Number) value).intValue() : null;
    }

    private Long extractLongFromResults(Map<String, Object> results, String key) {
        if (results == null || key == null) return null;
        Object value = results.get(key);
        return value instanceof Number ? ((Number) value).longValue() : null;
    }

    private Boolean extractBooleanFromResults(Map<String, Object> results, String key) {
        if (results == null || key == null) return null;
        Object value = results.get(key);
        return value instanceof Boolean ? (Boolean) value : null;
    }

    private String extractStringFromMap(Map<String, Object> map, String key) {
        if (map == null || key == null) return null;
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    private Integer extractIntegerFromMap(Map<String, Object> map, String key) {
        if (map == null || key == null) return null;
        Object value = map.get(key);
        return value instanceof Number ? ((Number) value).intValue() : null;
    }

    private Long extractLongFromMap(Map<String, Object> map, String key) {
        if (map == null || key == null) return null;
        Object value = map.get(key);
        return value instanceof Number ? ((Number) value).longValue() : null;
    }

    private Boolean extractBooleanFromMap(Map<String, Object> map, String key) {
        if (map == null || key == null) return null;
        Object value = map.get(key);
        return value instanceof Boolean ? (Boolean) value : null;
    }

    private List<String> extractStringListFromResults(Map<String, Object> results, String key) {
        if (results == null || key == null) return new ArrayList<>();
        Object value = results.get(key);
        if (value instanceof List) {
            return ((List<?>) value).stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    private List<ImprovementRecordDto.GeneratedTestDto> extractGeneratedTests(Map<String, Object> results) {
        if (results == null) return new ArrayList<>();
        Object value = results.get("generatedTests");
        if (value instanceof List) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> testMaps = (List<Map<String, Object>>) value;
            return testMaps.stream()
                    .filter(Objects::nonNull)
                    .map(testMap -> ImprovementRecordDto.GeneratedTestDto.builder()
                            .testMethodName(extractStringFromMap(testMap, "testMethodName"))
                            .testClass(extractStringFromMap(testMap, "testClass"))
                            .description(extractStringFromMap(testMap, "description"))
                            .coveredMethods(extractStringListFromMap(testMap, "coveredMethods"))
                            .estimatedCoverageContribution(extractIntegerFromMap(testMap, "estimatedCoverageContribution"))
                            .build())
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    private List<String> extractStringListFromMap(Map<String, Object> map, String key) {
        if (map == null || key == null) return new ArrayList<>();
        Object value = map.get(key);
        if (value instanceof List) {
            return ((List<?>) value).stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    private DashboardSummaryDto getDefaultDashboardSummary() {
        return DashboardSummaryDto.builder()
                .totalSessions(createDefaultCard("Total Sessions", "0", "No sessions yet", "fas fa-chart-line", "blue"))
                .successRate(createDefaultCard("Success Rate", "0%", "No data", "fas fa-check-circle", "green"))
                .averageCoverageIncrease(createDefaultCard("Coverage Increase", "0%", "No improvements", "fas fa-arrow-up", "red"))
                .timeSaved(createDefaultCard("Time Saved", "0 hrs", "No time saved", "fas fa-clock", "yellow"))
                .recentActivity(new ArrayList<>())
                .analytics(DashboardSummaryDto.AnalyticsData.builder()
                        .coverageTrends(new ArrayList<>())
                        .successRateTrends(new ArrayList<>())
                        .improvementsByCategory(new ArrayList<>())
                        .complexityAnalysis(new ArrayList<>())
                        .build())
                .activeSessions(new ArrayList<>())
                .build();
    }

    private DashboardSummaryDto.MetricCard createDefaultCard(String title, String value, String subtitle, String icon, String color) {
        return DashboardSummaryDto.MetricCard.builder()
                .title(title)
                .value(value)
                .subtitle(subtitle)
                .icon(icon)
                .color(color)
                .changePercentage(0.0)
                .changeDirection("stable")
                .build();
    }
}
