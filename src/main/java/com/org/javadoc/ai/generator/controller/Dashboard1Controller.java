package com.org.javadoc.ai.generator.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@RequestMapping("/docs")
@Controller
public class Dashboard1Controller {

    // DTO for Pull Request details
    public record PullRequest(
            String title,
            String repo,
            String status,
            LocalDateTime createdAt,
            int filesChanged
    ) {}

    @GetMapping("/dashboard1")
    public String getDashboardMetrics(Model model) {
        // SonarQube Metrics
        model.addAttribute("totalSonarIssues", 1425);
        model.addAttribute("totalDollarValueSave", "184,500");

        // Automation Metrics
        model.addAttribute("prCount", 78);
        model.addAttribute("timeSavedHours", 320);

        // Pull Requests from MongoDB (example data)
        List<PullRequest> pullRequests = Arrays.asList(
                new PullRequest(
                        "Fix security vulnerabilities in auth service",
                        "core-banking-api",
                        "merged",
                        LocalDateTime.now().minusDays(2),
                        15
                ),
                new PullRequest(
                        "Automated code quality improvements",
                        "payment-processing",
                        "open",
                        LocalDateTime.now().minusHours(5),
                        8
                ),
                new PullRequest(
                        "Update compliance checks",
                        "risk-assessment-service",
                        "merged",
                        LocalDateTime.now().minusDays(7),
                        22
                )
        );

        model.addAttribute("pullRequests", pullRequests);

        return "dashboard1";
    }
}
