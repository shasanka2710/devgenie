package com.org.devgenie.model.coverage;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestableComponent {
    private String methodName;
    private String complexity;
    private boolean currentlyCovered;
    private String riskLevel;
    private String description;
}
