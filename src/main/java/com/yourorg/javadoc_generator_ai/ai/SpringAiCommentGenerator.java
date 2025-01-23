package com.yourorg.javadoc_generator_ai.ai;

import com.yourorg.javadoc_generator_ai.ai.client.AiClient;
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
        String systemPrompt = "As an expert in Java, your task is to provide a concise and informative description. Highlight the class's main responsibilities and its role within the application. " + "Focus on delivering a clear overview that adheres to standard documentation practices.";
        return aiClient.callApi(systemPrompt, className);
    }

    /**
     * TODO: Add method description here.
     */
    public String generateMethodComment(String code, String className) {
        logger.info("Calling AI Client for method description for class: " + className);
        String systemPrompt = "As an expert in Java, your task is to craft comprehensive Javadoc documentation for the provided method." + "Describe the method's purpose, its parameters, and the values it returns. " + "Provide examples of how to use the method and any additional information that would be helpful to developers.";
        return aiClient.callApi(systemPrompt, code);
    }
}
