package com.org.javadoc.ai.generator.controller;

import com.org.javadoc.ai.generator.service.SonarService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/docs/dashboard")
public class DashboardController {
    @Autowired
    private SonarService sonarService;

    @Value("${developer.dollarValuePerMinute}")
    private double dollarValuePerMinute;

    @GetMapping
    public String getDashboard(Model model) {
        // Fetch metrics (dummy values used here, replace with actual logic)
        int totalClasses = 100; // Replace with actual logic to count classes
        int totalMethods = 500; // Replace with actual logic to count methods
        int totalSonarIssues = 50; // Replace with actual logic to fetch sonar issues
        int totalEffortMinutes = 1200; // Replace with actual logic to fetch effort in minutes
        double totalDollarValueSave = totalEffortMinutes * dollarValuePerMinute;

        model.addAttribute("totalClasses", totalClasses);
        model.addAttribute("totalMethods", totalMethods);
        model.addAttribute("totalSonarIssues", totalSonarIssues);
        model.addAttribute("totalEffortMinutes", totalEffortMinutes);
        model.addAttribute("totalDollarValueSave", totalDollarValueSave);

        return "dashboard";
    }
}