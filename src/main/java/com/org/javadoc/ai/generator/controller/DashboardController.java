package com.org.javadoc.ai.generator.controller;

import com.org.javadoc.ai.generator.service.SonarService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import java.io.IOException;

@Controller
@RequestMapping("/docs/dashboard")
public class DashboardController {

    private final SonarService sonarService;
    private final double dollarValuePerMinute;

    public DashboardController(SonarService sonarService, @Value("${developer.dollarValuePerMinute}") double dollarValuePerMinute) {
        this.sonarService = sonarService;
        this.dollarValuePerMinute = dollarValuePerMinute;
    }

    @GetMapping
    public String getDashboard(Model model) throws IOException {
        // Fetch metrics (dummy values used here, replace with actual logic)
        // Replace with actual logic to count classes
        int totalClasses = 100;
        // Replace with actual logic to count methods
        int totalMethods = 500;
        // Replace with actual logic to fetch sonar issues
        int totalSonarIssues = sonarService.getRootNode().get("total").asInt();
        // Replace with actual logic to fetch effort in minutes
        int totalEffortMinutes = sonarService.getRootNode().get("effortTotal").asInt();
        double totalDollarValueSave = totalEffortMinutes * dollarValuePerMinute;
        model.addAttribute("totalClasses", totalClasses);
        model.addAttribute("totalMethods", totalMethods);
        model.addAttribute("totalSonarIssues", totalSonarIssues);
        model.addAttribute("totalEffortMinutes", totalEffortMinutes);
        model.addAttribute("totalDollarValueSave", totalDollarValueSave);
        return "dashboard";
    }
}
