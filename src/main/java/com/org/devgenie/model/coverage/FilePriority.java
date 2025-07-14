package com.org.devgenie.model.coverage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FilePriority {
    private String filePath;
    private int priority;
    private double impactScore;
    private double effortScore;
    private double impactEffortRatio;
    private String reasoning;
    private double estimatedCoverageGain;
    private String frameworkConsiderations;
    private String testingApproach;
}
