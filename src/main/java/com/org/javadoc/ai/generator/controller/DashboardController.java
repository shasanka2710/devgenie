package com.org.javadoc.ai.generator.controller;

import com.fasterxml.jackson.databind.JsonNode;
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

    private final double dollarValuePerMinute;

    public DashboardController(@Value("${developer.dollarValuePerMinute}") double dollarValuePerMinute) {
        this.dollarValuePerMinute = dollarValuePerMinute;
    }

    @GetMapping
    public String getDashboard(Model model, SonarService sonarService) throws IOException {
        int totalSonarIssues = 0;
        int totalEffortMinutes = 0;
        JsonNode rootNode = sonarService.getRootNode();
        int totalClasses = 100;
        // Replace with actual logic to count methods
        int totalMethods = 500;
        if (rootNode != null) {
            totalSonarIssues = rootNode.path("total").asInt();
            totalEffortMinutes = rootNode.path("effort").asInt();
        }
        double totalDollarValueSave = totalEffortMinutes * dollarValuePerMinute;
        model.addAttribute("totalClasses", totalClasses);
        model.addAttribute("totalMethods", totalMethods);
        model.addAttribute("totalSonarIssues", totalSonarIssues);
        model.addAttribute("totalEffortMinutes", totalEffortMinutes);
        model.addAttribute("totalDollarValueSave", totalDollarValueSave);
        return "dashboard";
    }
}
