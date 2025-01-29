package com.org.javadoc.ai.generator.ai;

import com.org.javadoc.ai.generator.ai.client.AiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * TODO: Add class description here.
 */
@Component
public class SpringAiCommentGenerator {

    private static final Logger logger = LoggerFactory.getLogger(SpringAiCommentGenerator.class);

    @Autowired
    private AiClient aiClient;

    public String generateClassComment(String classBody, String className) {
        logger.info("Calling AI Client for class description");
        String systemPrompt = "As an expert in Java, follow proper java document standards and summarize the purpose of the class and its key features.";
        return aiClient.callApi(systemPrompt, className);
    }

    public String generateMethodComment(String code, String className) {
        logger.info("Calling AI Client for method description for class: {}", className);
        String systemPrompt = "As an expert in Java, explain me the purpose of the below and its key features in bulleted points.";
        return aiClient.callApi(systemPrompt, code);
    }

    public String fixSonarIssue(String className, String classBody, String description) {
        logger.info("Calling AI Client for fixing sonar issue for class: {}", className);
        String systemPrompt = String.format(
                "You are an expert Java developer and code quality analyzer. Your task is to analyze and fix the provided Java code based on the class name and issue description given by SonarQube. Follow these strict guidelines:%n" +

                        "1. **Scope of Analysis**:%n" +
                        "    - Focus exclusively on the class **%s**.%n" +
                        "    - Do not assume or modify external classes, dependencies, or configurations unless explicitly provided.%n" +

                        "2. **SonarQube Issue**:%n" +
                        "    - The reported SonarQube issue is: **%s**.%n" +
                        "    - Accurately identify and fix the issue within the provided code snippet.%n" +

                        "3. **Code Review & Fixing Guidelines**:%n" +
                        "    - Review the provided Java class thoroughly.%n" +
                        "    - Apply best practices to resolve the issue effectively.%n" +
                        "    - Ensure code correctness, maintainability, and adherence to Java standards.%n" +

                        "4. **Output Requirements**:%n" +
                        "    - Return **only a valid, compilable Java class**.%n" +
                        "    - Ensure the output conforms to Java best practices and follows industry standards.%n" +
                        "    - **Strictly avoid** including backticks, fences (```), pseudo-code, or incomplete code snippets.%n" +

                        "5. **Commenting Guidelines**:%n" +
                        "    - Add **concise and relevant comments** only where necessary to explain changes.%n" +
                        "    - Avoid excessive commenting or redundant explanations.%n" +

                        "6. **Error Handling & Insufficient Information**:%n" +
                        "    - If the issue cannot be fully resolved due to missing context, return the original code unchanged.%n" +
                        "    - Include explanatory comments within the code, highlighting why the issue could not be fixed and what additional details are required.%n" +

                        "7. **Output Format**:%n" +
                        "    - Provide **only** the corrected Java class with proper formatting.%n" +
                        "    - **Do not include any additional text, instructions, or explanations outside the Java class definition.**%n" +

                        "8. **Java Version Compatibility**:%n" +
                        "    - Ensure the code is compatible with **Java 8 and above**, unless stated otherwise.%n" +
                        "    - Avoid deprecated APIs unless explicitly necessary.%n" +
                        "    - Follow modern Java practices while maintaining backward compatibility where required.%n",
                className, description
        );
        return aiClient.callApi(systemPrompt, classBody);
    }
}
