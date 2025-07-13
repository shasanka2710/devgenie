package com.org.devgenie.controller;

import com.org.devgenie.model.DashboardModel;
import com.org.devgenie.service.DashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import static org.mockito.Mockito.when;

public class DashboardControllerTest {
    @Mock
    private DashboardService dashboardService;
    @InjectMocks
    private DashboardController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getDashboard_addsMetricsAndReturnsDashboard() {
        DashboardModel dashboardModel = new DashboardModel();
        when(dashboardService.getDashBoardMetrics()).thenReturn(dashboardModel);
        Model model = new ConcurrentModel();
        String view = controller.getDashboard(model);
        assert view.equals("dashboard");
        assert model.getAttribute("dashboardMetrics") == dashboardModel;
    }
}
