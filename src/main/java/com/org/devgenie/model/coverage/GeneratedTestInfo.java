package com.org.devgenie.model.coverage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedTestInfo {
    private String testMethodName;
    private String testClass;
    private String description;
    private List<String> coveredMethods;
    private Double estimatedCoverageContribution;
    private String testCode;
    private String testFilePath;
}
