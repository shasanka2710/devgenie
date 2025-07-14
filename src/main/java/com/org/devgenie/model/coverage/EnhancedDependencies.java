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
public class EnhancedDependencies {
    private List<String> injectableDependencies;
    private List<String> externalDependencies;
    private List<String> frameworkDependencies;
}
