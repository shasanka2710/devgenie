package com.org.devgenie.model.coverage;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedTest {
    private String methodName;
    private String testType;
    private String description;
    private String targetCodePath;
    private String coverageImpact;
}
