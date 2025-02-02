package com.org.javadoc.ai.generator.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SonarMetricsModel {
    private int totalIssuesCount;
    private String techDebtTime;
    private double dollarImpact;
}
