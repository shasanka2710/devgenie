package com.org.devgenie.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SonarMetricsModel {
    private int totalIssuesCount;
    private String techDebtTime;
    private String dollarImpact;
}
