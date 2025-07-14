package com.org.devgenie.model.coverage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnhancedTestableComponent {
    private String methodName;
    private String complexity;
    private boolean currentlyCovered;
    private String riskLevel;
    private String description;
    private String testType;
    private String frameworkSpecific;
}
