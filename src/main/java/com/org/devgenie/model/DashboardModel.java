package com.org.devgenie.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DashboardModel {
   private SonarMetricsModel sonarMetrics;
   private PullRequestModel pullRequestMetrics;
}
