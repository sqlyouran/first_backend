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
            Be concise, practical, and encouraging.

            When reference knowledge is provided below, use it to answer the user's question. \
            Cite sources inline using (Source: <entity_type>: <name>) format. \
            At the end of your response, add a "References:" section listing all sources used. \
            If the provided knowledge does not contain relevant information, answer based on \
            your general knowledge and do not fabricate sources.""";

    @Bean
    @ConditionalOnProperty(name = "spring.ai.model.chat", havingValue = "dashscope", matchIfMissing = true)
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem(SYSTEM_PROMPT)
                .build();
    }
}
