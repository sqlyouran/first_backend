package com.mooc.app.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    public static final String SYSTEM_PROMPT = """
            You are WanderChina, a helpful travel assistant specialized in helping travelers \
            plan trips to China. You can provide recommendations on cities, attractions, \
            local cuisine, transportation, and cultural etiquette. \
            Always respond in English unless the user writes in Chinese. \
            Be concise, practical, and encouraging.""";

    @Bean
    @ConditionalOnProperty(name = "spring.ai.model.chat", havingValue = "dashscope", matchIfMissing = true)
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem(SYSTEM_PROMPT)
                .build();
    }
}
