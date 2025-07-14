package com.org.devgenie.model.coverage;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SonarQubeComponentTreeResponse {
    private List<SonarQubeComponent> components;
}
