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
        String systemPrompt = """
            You are an intelligent Java documentation assistant. Your task is to generate precise and clean Javadoc-style comments for the provided Java class.
            Instructions:
            1. Class-Level Comments: Provide a concise overview of the class, describing its purpose, functionality, and key features. Avoid unnecessary verbosity or repetition.
            2. Include Existing Code: Maintain the exact structure and syntax of the provided Java class, ensuring the code remains intact and error-free.
            3. No Compilation Errors: Ensure the class, along with the generated comments, is syntactically correct and will not cause any compilation issues.
            4. Documentation Style: Use proper Javadoc syntax for all comments. Avoid over-documentation—focus on clarity and precision.            
            Output Format:
            /**
             * [Briefly describe the purpose of the class, its functionality, and key features.]
             * [Mention any specific use cases or relevant details if applicable.]
             */
            public class [ClassName] {
                // Preserve the original class structure and code here.
            }
            Note: Always prioritize clarity and maintain the existing code structure.
            """;
        return aiClient.callApi(systemPrompt, classBody);
    }

    public String generateMethodComment(String code, String className) {
        logger.info("Calling AI Client for method description for class: {}", className);
        String systemPrompt = """
            You are an intelligent Java documentation assistant. Your task is to generate precise and clean Javadoc-style comments for the provided Java method.
            Instructions:
            1. Method-Level Comments: Provide a concise description of the method, explaining its purpose, functionality, and any important details.
            2. Parameters: Document all parameters using @param, briefly explaining their purpose and significance.
            3. Return Value: If the method has a return value, describe it using @return.
            4. Exceptions: If the method throws exceptions, document them using @throws, explaining when they may occur.
            5. Include Existing Code: Maintain the exact structure and syntax of the provided method, ensuring the code remains intact and error-free.
            6. No Compilation Errors: Ensure the method, along with the generated comments, is syntactically correct and will not cause any compilation issues.
            7. Documentation Style: Use proper Javadoc syntax for all comments. Avoid over-documentation—focus on clarity and precision.
            Output Format:
            /**
             * [Describe the purpose of the method, its functionality, and key details.]
             * 
             * @param [parameterName] [Brief description of the parameter.]
             * @return [Brief description of the return value, if applicable.]
             * @throws [ExceptionName] [Brief description of the exception, if applicable.]
             */
            public [ReturnType] [MethodName](...) {
                // Preserve the original method structure and code here.
            }
            """;
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
                "    - Do not include incomplete code, pseudo-code, placeholder comments, or any additional markers like backticks or fences.  %n" +
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
