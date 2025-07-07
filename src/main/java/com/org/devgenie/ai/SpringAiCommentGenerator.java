package com.org.devgenie.ai;

import com.org.devgenie.ai.client.AiClient;
import com.org.devgenie.util.LoggerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * The type Spring ai comment generator.
 */
@Component
public class SpringAiCommentGenerator {

    private static final Logger logger = LoggerFactory.getLogger(SpringAiCommentGenerator.class);
    private final AiClient aiClient;

    /**
     * Instantiates a new Spring ai comment generator.
     *
     * @param aiClient the ai client
     */
    public SpringAiCommentGenerator(AiClient aiClient) {
        this.aiClient = aiClient;
    }

    /**
     * Generates a Javadoc comment for a Java class.
     *
     * @param classBody the body of the class
     * @param className the name of the class
     * @return the generated Javadoc comment
     */
    public String generateClassComment(String classBody, String className) {
        logger.info("Calling AI Client for class description");
        String systemPrompt = String.format(
                "You are an intelligent Java documentation assistant. Your task is to generate precise and clean Javadoc-style comments for the provided Java class.\n"
                        + "Instructions:\n"
                        + "1. Class-Level Comments: Provide a concise overview of the class, describing its purpose, functionality, and key features. Avoid unnecessary verbosity or repetition.\n"
                        + "2. Include Existing Code: Maintain the exact structure and syntax of the provided Java class, ensuring the code remains intact and error-free.\n"
                        + "3. No Compilation Errors: Ensure the class, along with the generated comments, is syntactically correct and will not cause any compilation issues.\n"
                        + "4. Documentation Style: Use proper Javadoc syntax for all comments. Avoid over-documentation—focus on clarity and precision.\n"
                        + "Output Format:\n"
                        + "/**\n"
                        + " * [Briefly describe the purpose of the class, its functionality, and key features.]\n"
                        + " * [Mention any specific use cases or relevant details if applicable.]\n"
                        + " */\n"
                        + "public class [ClassName] {\n"
                        + "    // Preserve the original class structure and code here.\n"
                        + "}\n"
                        + "Note: Always prioritize clarity and maintain the existing code structure.\n"
        );
        return aiClient.callApi(systemPrompt, classBody);
    }

    /**
     * Generates a Javadoc comment for a Java method.
     *
     * @param code      the method code
     * @param className the class name
     * @return the generated Javadoc comment
     */
    public String generateMethodComment(String code, String className) {
        logger.info("Calling AI Client for method description for class: {}", LoggerUtil.maskSensitive(className));
        String systemPrompt = String.format(
                "You are an intelligent Java documentation assistant. Your task is to generate precise and clean Javadoc-style comments for the provided Java method.\n"
                        + "Instructions:\n"
                        + "1. Method-Level Comments: Provide a concise description of the method, explaining its purpose, functionality, and any important details.\n"
                        + "2. Parameters: Document all parameters using @param, briefly explaining their purpose and significance.\n"
                        + "3. Return Value: If the method has a return value, describe it using @return.\n"
                        + "4. Exceptions: If the method throws exceptions, document them using @throws, explaining when they may occur.\n"
                        + "5. Include Existing Code: Maintain the exact structure and syntax of the provided method, ensuring the code remains intact and error-free.\n"
                        + "6. No Compilation Errors: Ensure the method, along with the generated comments, is syntactically correct and will not cause any compilation issues.\n"
                        + "7. Documentation Style: Use proper Javadoc syntax for all comments. Avoid over-documentation—focus on clarity and precision.\n"
                        + "Output Format:\n"
                        + "/**\n"
                        + " * [Describe the purpose of the method, its functionality, and key details.]\n"
                        + " * \n"
                        + " * @param [parameterName] [Brief description of the parameter.]\n"
                        + " * @return [Brief description of the return value, if applicable.]\n"
                        + " * @throws [ExceptionName] [Brief description of the exception, if applicable.]\n"
                        + " */\n"
                        + "public [ReturnType] [MethodName](...) {\n"
                        + "    // Preserve the original method structure and code here.\n"
                        + "}\n"
        );
        return aiClient.callApi(systemPrompt, code);
    }

    /**
     * Fixes multiple SonarQube issues for a class.
     *
     * @param className   the class name
     * @param classBody   the class body
     * @param descriptions the set of issue descriptions
     * @return the fixed class code
     */
    public String fixSonarIssues(String className, String classBody, Set<String> descriptions) {
        logger.info("Calling AI Client for fixing multiple SonarQube issues for class: {}", LoggerUtil.maskSensitive(className));

        // Joining multiple issue descriptions into a formatted bullet list
        String formattedIssues = descriptions.stream()
                .map(desc -> "- " + desc)
                .collect(Collectors.joining("\n"));

        String systemPrompt = String.format(
                "You are an expert Java developer and code quality analyzer. Your task is to analyze and fix the provided Java class **%s** based on the reported SonarQube issues. "
                        + "All issue descriptions for this class are provided below. Ensure that all issues are **accurately identified and fixed** in a single pass.\n\n"
                        + "**SonarQube Issues to Fix:**\n%s\n\n"
                        + "### **Strict Guidelines:**\n"
                        + "1. **Scope of Analysis:**\n"
                        + "    - Focus exclusively on the class **%s**.\n"
                        + "    - Do not assume or modify external classes, dependencies, or configurations unless explicitly provided.\n"
                        + "2. **Issue Resolution Requirements:**\n"
                        + "    - Carefully review and fix **each** issue listed above.\n"
                        + "    - Apply best practices to resolve the issues effectively.\n"
                        + "    - Ensure the fixes do not introduce new bugs or change the intended functionality.\n"
                        + "3. **Code Quality & Compliance:**\n"
                        + "    - Follow Java coding standards, maintainability, and readability best practices.\n"
                        + "    - Ensure the final class is fully **compilable and functional**.\n"
                        + "    - Avoid unnecessary code modifications beyond the reported issues.\n"
                        + "4. **Commenting Guidelines:**\n"
                        + "    - Add **very concise comments** only where necessary to explain changes.\n"
                        + "    - Do not over-comment or include redundant explanations.\n"
                        + "5. **Error Handling & Insufficient Information:**\n"
                        + "    - If some issues cannot be fully resolved due to missing context, return the original code **with explanatory comments** highlighting what additional details are required.\n"
                        + "6. **Output Format:**\n"
                        + "    - Return **only the corrected Java class with proper formatting**.\n"
                        + "    - **Do not include any additional text, explanations, or formatting outside the class definition.**\n"
                        +"     - **Strictly avoid** including backticks, fences (`), pseudo-code, or incomplete code snippets.\n"
                        + "7. **Java Version Compatibility:**\n"
                        + "    - Ensure compatibility with **Java 8 and above**.\n"
                        + "    - Avoid deprecated APIs unless explicitly necessary.\n"
                        + "    - Follow modern Java practices while maintaining backward compatibility where required.\n",
                className, formattedIssues, className
        );

        return aiClient.callApi(systemPrompt, classBody);
    }
}
