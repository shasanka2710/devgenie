package com.org.javadoc.ai.generator.service;

import com.org.javadoc.ai.generator.model.DashboardModel;
import com.org.javadoc.ai.generator.model.PullRequestModel;
import com.org.javadoc.ai.generator.model.SonarMetricsModel;
import com.org.javadoc.ai.generator.mongo.PullRequestMetrics;
import com.org.javadoc.ai.generator.mongo.PullRequestMetricsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Slf4j
@Service
public class DashboardService {

    private final PullRequestMetricsRepository pullRequestMetricsRepository;
    private final SonarService sonarService;

    @Autowired
    public DashboardService(PullRequestMetricsRepository pullRequestMetricsRepository,
                            SonarService sonarService) {
        this.pullRequestMetricsRepository = pullRequestMetricsRepository;
        this.sonarService = sonarService;
    }

    public DashboardModel getDashBoardMetrics() {
        DashboardModel dashboardModel = new DashboardModel();
        dashboardModel.setSonarMetrics(getSonarMetrics());
        dashboardModel.setPullRequestMetrics(getConsolidatedPullRequestMetrics());
        return dashboardModel;
    }

    public SonarMetricsModel getSonarMetrics() {
        return sonarService.getSonarMetrics();
    }

    /**
     * Method to fetch and consolidate all pull request metrics for the dashboard.
     * @return Consolidated metrics as a string or a suitable object.
     */
    public PullRequestModel getConsolidatedPullRequestMetrics() {
        List<PullRequestMetrics> allMetrics = pullRequestMetricsRepository.findAll();

        int totalPRs = allMetrics.size();
        int totalIssuesResolved = 0;
        String totalEngineeringTimeSaved = "0 minutes";
        String totalCostSavings = "$0";

        // Iterate through the list and consolidate data
        for (PullRequestMetrics metrics : allMetrics) {
            totalIssuesResolved += metrics.getIssuesResolved();
            totalEngineeringTimeSaved = addTime(totalEngineeringTimeSaved, metrics.getEngineeringTimeSaved());
            totalCostSavings = addCost(totalCostSavings, metrics.getCostSavings());
        }
        PullRequestModel pullRequestModel = new PullRequestModel();
        pullRequestModel.setPrCreatedCount(totalPRs);
        pullRequestModel.setIssuesResolved(totalIssuesResolved);
        pullRequestModel.setEngineeringTimeSaved(totalEngineeringTimeSaved);
        pullRequestModel.setCostSavings(totalCostSavings);

        // Format the consolidated data
        String result = String.format(
                "Total Pull Requests Created: %d\nTotal Issues Resolved: %d\nTotal Engineering Time Saved: %s\nTotal Cost Savings: %s",
                totalPRs, totalIssuesResolved, totalEngineeringTimeSaved, totalCostSavings
        );

        log.info("Consolidated Metrics: " + result);

        return pullRequestModel;
    }

    // Helper method to add engineering time saved
    private String addTime(String totalTime, String newTime) {
        // Example of extracting minutes from "30 minutes" format
        int totalMinutes = extractTimeInMinutes(totalTime);
        int newMinutes = extractTimeInMinutes(newTime);

        int updatedMinutes = totalMinutes + newMinutes;
        return updatedMinutes + " minutes";
    }

    // Helper method to extract time in minutes from string
    private int extractTimeInMinutes(String timeString) {
        try {
            String[] parts = timeString.split(" ");
            return Integer.parseInt(parts[0]);
        } catch (Exception e) {
            return 0;
        }
    }

    // Helper method to add cost savings
    private String addCost(String totalCost, String newCost) {
        // Example of extracting dollars from "$10" format
        int totalAmount = extractCost(totalCost);
        int newAmount = extractCost(newCost);

        int updatedAmount = totalAmount + newAmount;
        return "$" + updatedAmount;
    }

    // Helper method to extract cost from string
    private int extractCost(String costString) {
        try {
            String amount = costString.replace("$", "").trim();
            return Integer.parseInt(amount);
        } catch (Exception e) {
            return 0;
        }
    }
}
