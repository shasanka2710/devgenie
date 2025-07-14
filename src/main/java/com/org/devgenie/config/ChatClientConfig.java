package com.org.devgenie.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class ChatClientConfig {

    @Bean
    public ChatClient chatClient(ChatModel chatModel) {
        log.info("Creating ChatClient with model: {}", chatModel);
        return ChatClient.builder(chatModel).build();
    }
}
