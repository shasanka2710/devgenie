package com.org.devgenie.model.coverage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectCharacteristics {
    private long javaFileCount;
    private boolean multiModule;
    private List<String> architecturePatterns;
    private boolean hasDocumentation;
    private List<String> cicdTools;
    private String projectComplexity; // LOW, MEDIUM, HIGH
    private LocalDateTime analyzedAt;
}
