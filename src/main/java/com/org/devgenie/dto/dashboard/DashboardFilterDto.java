package com.org.devgenie.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardFilterDto {
    private String category; // ALL, COVERAGE, ISSUES
    private String subCategory; // FILE_LEVEL, REPOSITORY_LEVEL
    private String status; // ALL, COMPLETED, IN_PROGRESS, FAILED
    private String timeRange; // LAST_7_DAYS, LAST_30_DAYS, LAST_90_DAYS, ALL_TIME
    private String sortBy; // DATE_DESC, DATE_ASC, COVERAGE_DESC, COVERAGE_ASC
    private Integer page;
    private Integer size;
}
