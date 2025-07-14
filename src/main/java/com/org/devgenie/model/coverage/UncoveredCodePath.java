package com.org.devgenie.model.coverage;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UncoveredCodePath {
    private String location;
    private String description;
    private String priority;
    private String suggestedTestType;
}
