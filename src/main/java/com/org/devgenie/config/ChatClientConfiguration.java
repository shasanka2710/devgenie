package com.org.devgenie.config;

@Configuration
public class ChatClientConfiguration {

    @Bean
    @ConditionalOnProperty(name = "spring.ai.openai.api-key")
    public ChatClient openAiChatClient(OpenAiChatClient openAiChatClient) {
        return ChatClient.builder(openAiChatClient)
                .defaultSystem("You are an expert Java developer and test automation specialist. " +
                        "Provide accurate, detailed, and actionable responses. " +
                        "Always format your responses as valid JSON when requested.")
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "spring.ai.ollama.base-url")
    public ChatClient ollamaChatClient(OllamaChatClient ollamaChatClient) {
        return ChatClient.builder(ollamaChatClient)
                .defaultSystem("You are an expert Java developer and test automation specialist. " +
                        "Provide accurate, detailed, and actionable responses. " +
                        "Always format your responses as valid JSON when requested.")
                .build();
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
