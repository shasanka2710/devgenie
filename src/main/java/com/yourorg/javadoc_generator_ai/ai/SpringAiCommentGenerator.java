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
        String systemPrompt = "As an expert in Java, follow proper java document standards and summarize the purpose of the class and its key features.";
        return aiClient.callApi(systemPrompt, className);
    }

    /**
     * TODO: Add method description here.
     */
    public String generateMethodComment(String code, String className) {
        logger.info("Calling AI Client for method description for class: " + className);
        String systemPrompt = "As an expert in Java, explain me the purpose of the below and its key features in bulleted points.";
        return aiClient.callApi(systemPrompt, code);
    }
}
