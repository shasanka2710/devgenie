package com.org.devgenie.model.coverage;

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
}
