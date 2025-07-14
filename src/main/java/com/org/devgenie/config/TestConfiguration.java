package com.org.devgenie.config;

import com.org.devgenie.service.coverage.JacocoService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class CoverageTestConfiguration {

    @Bean
    @Primary
    public ChatClient mockChatClient() {
        return Mockito.mock(ChatClient.class);
    }

    @Bean
    @Primary
    public JacocoService mockJacocoService() {
        return Mockito.mock(JacocoService.class);
    }

  /*  @Bean
    @Primary
    public GitService mockGitService() {
        return Mockito.mock(GitService.class);
    }*/
}
