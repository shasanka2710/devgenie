package com.org.devgenie.model.coverage;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuildCommands {
    private String clean;
    private String compile;
    private String test;
    private String coverage;
    private String install;
}
