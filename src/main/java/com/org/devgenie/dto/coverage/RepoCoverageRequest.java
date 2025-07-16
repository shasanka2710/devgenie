package com.org.devgenie.dto.coverage;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RepoCoverageRequest {
    private String repoPath;
    private String branch = "main";
    private Double targetCoverageIncrease;
    private List<String> excludeFiles;
    private boolean forceRefresh;
}
