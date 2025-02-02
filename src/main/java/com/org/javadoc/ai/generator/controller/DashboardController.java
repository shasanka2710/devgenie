package com.org.javadoc.ai.generator.controller;

import com.org.javadoc.ai.generator.service.DashboardService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/docs/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public String getDashboard(Model model) { // Removed throws IOException as it's not thrown from the method body
        model.addAttribute("dashboardMetrics", dashboardService.getDashBoardMetrics());
        return "dashboard";
    }
}