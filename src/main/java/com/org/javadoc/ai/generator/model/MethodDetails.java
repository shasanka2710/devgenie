package com.org.javadoc.ai.generator.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class MethodDetails {
    private String signature;
    private String description;
    private String returnType;
    private String exceptions;
}