package com.mooc.app.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.mockito.Mockito.mock;

@Configuration
public class TestAiConfig {

    @Bean
    @ConditionalOnProperty(name = "spring.ai.model.chat", havingValue = "none")
    public ChatClient mockChatClient() {
        return mock(ChatClient.class);
    }
}
