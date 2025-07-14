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
public class TestingStrategy {
    private List<String> unitTests;
    private List<String> integrationTests;
    private List<String> mockingNeeded;
}
