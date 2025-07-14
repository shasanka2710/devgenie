package com.org.devgenie.model.coverage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
