package com.org.devgenie.service;

import com.org.devgenie.mongo.PullRequestMetricsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.*;

public class DashboardServiceTest {
    @Mock private PullRequestMetricsRepository pullRequestMetricsRepository;
    @Mock private SonarService sonarService;
    @InjectMocks private DashboardService dashboardService;

    @BeforeEach
    void setUp() { MockitoAnnotations.openMocks(this); }

    @Test
    void getDashBoardMetrics_noException() {
        dashboardService.getDashBoardMetrics();
        // No exception = pass
    }
}
