package com.org.devgenie.model;


import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class CoverageComponentNode {
    @Id
    private String id;            // Unique identifier for MongoDB
    private String key;           // e.g., devgenie:src/main/java/com/example/MyService.java
    private String path;          // e.g., src/main/java/com/example/MyService.java
    private String type;          // FIL or DIR
    private Map<String, Double> metricsMap;     // null if directory or no coverage
    private List<CoverageComponentNode> children; // For tree rendering (optional)
}
