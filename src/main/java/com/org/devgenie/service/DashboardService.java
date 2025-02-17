package com.org.devgenie.service;

import com.org.devgenie.model.DashboardModel;
import com.org.devgenie.model.PullRequestModel;
import com.org.devgenie.model.SonarMetricsModel;
import com.org.devgenie.mongo.PullRequestMetrics;
import com.org.devgenie.mongo.PullRequestMetricsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import static com.org.devgenie.util.ConverterUtil.convertToHours;
import static com.org.devgenie.util.ConverterUtil.roundToTwoDecimalPlaces;

@Slf4j
@Service
public class DashboardService {

    private final PullRequestMetricsRepository pullRequestMetricsRepository;

    private final SonarService sonarService;

    @Autowired
    public DashboardService(PullRequestMetricsRepository pullRequestMetricsRepository, SonarService sonarService) {
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
        int totalEngineeringTimeSaved = 0;
        double totalCostSavings = 0.0;
        // Iterate through the list and consolidate data
        for (PullRequestMetrics metrics : allMetrics) {
            totalIssuesResolved += metrics.getIssuesResolved();
            totalEngineeringTimeSaved = addTime(totalEngineeringTimeSaved, metrics.getEngineeringTimeSaved());
            totalCostSavings = addCost(totalCostSavings, metrics.getCostSavings());
        }
        PullRequestModel pullRequestModel = new PullRequestModel();
        pullRequestModel.setPrCreatedCount(totalPRs);
        pullRequestModel.setIssuesResolved(totalIssuesResolved);
        pullRequestModel.setEngineeringTimeSaved(convertToHours(totalEngineeringTimeSaved) + " hours");
        pullRequestModel.setCostSavings("$" + roundToTwoDecimalPlaces(totalCostSavings));
        // Format the consolidated data
        String result = String.format("Total Pull Requests Created: %d%nTotal Issues Resolved: %d%nTotal Engineering Time Saved: %s%nTotal Cost Savings: %s", totalPRs, totalIssuesResolved, totalEngineeringTimeSaved, totalCostSavings);
        log.info("Consolidated Metrics: " + result);
        return pullRequestModel;
    }

    // Helper method to add engineering time saved
    private int addTime(int totalTime, int newTime) {
        return totalTime + newTime;
    }

    // Helper method to add cost savings
    private double addCost(double totalCost, double newCost) {
        return totalCost + newCost;
    }
}