package com.org.devgenie.model.coverage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnhancedUncoveredCodePath {
    private String location;
    private String description;
    private String priority;
    private String suggestedTestType;
    private String frameworkConsiderations;
}
