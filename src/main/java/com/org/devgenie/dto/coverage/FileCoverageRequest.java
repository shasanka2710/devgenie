package com.org.devgenie.dto.coverage;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileCoverageRequest {
    private String filePath;
    private Double targetCoverageIncrease;
}
