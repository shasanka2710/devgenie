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

    /**
     * TODO: Add method description here.
     */
    public String generateClassComment(String classBody, String className) {
        logger.info("Calling AI Client for class description");
        String systemPrompt = "As an expert in Java, follow proper java document standards and summarize the purpose of the class and its key features.";
        return aiClient.callApi(systemPrompt, className);
    }

    /**
     * TODO: Add method description here.
     */
    public String generateMethodComment(String code, String className) {
        logger.info("Calling AI Client for method description for class: {}", className);
        String systemPrompt = "As an expert in Java, explain me the purpose of the below and its key features in bulleted points.";
        return aiClient.callApi(systemPrompt, code);
    }

    public String fixSonarIssue(String className, String classBody, String description) {
        logger.info("Calling AI Client for fixing sonar issue for class: {}", className);
        String systemPrompt = String.format("You are an expert Java developer and code quality analyzer. Your task is to analyze and fix the provided Java code based on the class name and issue description given by SonarQube. Follow these strict guidelines:  %n" +
                "1. **Focus Scope**: Concentrate exclusively on the class %s Do not make assumptions about external classes or dependencies unless explicitly provided.%n" +
                "2. **SonarQube Issue**: The issue reported by SonarQube is: %s. Address this issue in the provided code snippet.  %n" +
                "3. **Code Review**:  %n" +
                "    - Review the provided code snippet thoroughly.  %n" +
                "    - Identify and fix areas that correspond to the described issue.  %n" +
                "4. **Output Requirements**:  %n" +
                "    - Return only a **valid, compilable Java class**.  %n" +
                "    - Ensure the output adheres to Java best practices and coding standards.  %n" +
                "    - Do not include incomplete code, pseudo-code, or placeholder comments.  %n" +
                "5. **Commenting Guidelines**:  %n" +
                "    - Include comments only where necessary to explain the changes you made.  %n" +
                "    - Ensure comments are concise and do not break Java syntax or formatting.  %n" +
                "6. **Error Handling**:%n" +
                "    - If the issue cannot be resolved due to insufficient information, return the original code unchanged with explanatory comments highlighting why the issue could not be fixed.  %n" +
                "7. **Output Format**:%n" +
                "    - Do not include any additional text, instructions, or comments outside the class definition.  %n" +
                "    - Provide only the corrected Java class, formatted correctly.%n" +
                "8. **Additional Context**: %n" +
                "    - If additional context is required to resolve the issue, make reasonable assumptions and document them within the code comments.  ", className, description);
        return aiClient.callApi(systemPrompt, classBody);
    }
}
