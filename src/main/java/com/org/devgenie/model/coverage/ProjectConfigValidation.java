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
public class ProjectConfigValidation {
    private boolean valid;
    private List<String> warnings;
    private List<String> recommendations;
    private LocalDateTime validatedAt;
}
