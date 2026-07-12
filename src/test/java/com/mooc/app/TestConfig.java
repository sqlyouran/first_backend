package com.mooc.app;

import org.mockito.Mockito;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class TestConfig {

    @Bean
    @Primary
    @ConditionalOnMissingBean(ChatModel.class)
    public ChatModel chatModel() {
        return Mockito.mock(ChatModel.class);
    }
}
