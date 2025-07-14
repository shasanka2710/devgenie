package com.org.devgenie.model.coverage;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SonarQubeMeasure {
    private String metric;
    private String value;
}
