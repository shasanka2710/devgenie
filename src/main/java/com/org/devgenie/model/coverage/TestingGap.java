package com.org.devgenie.model.coverage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestingGap {
    private String category;
    private String description;
    private String priority; // LOW, MEDIUM, HIGH
    private String estimatedEffort; // LOW, MEDIUM, HIGH
}
