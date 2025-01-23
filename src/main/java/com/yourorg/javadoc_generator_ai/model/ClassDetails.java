package com.yourorg.javadoc_generator_ai.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class ClassDetails {
    private String name;
    private String description;
    private List<String> fields;
    private List<String> constructors;
    private List<MethodDetails> methods;
}