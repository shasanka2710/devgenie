package com.org.devgenie.dto.coverage;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileCoverageRequest {
    private String filePath;
    private Double targetCoverageIncrease;
}
