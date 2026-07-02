package com.mooc.app.service;

import com.mooc.app.entity.AiConversation;
import com.mooc.app.entity.AiMessage;
import com.mooc.app.entity.AiMessageRole;
import com.mooc.app.repository.AiConversationRepository;
import com.mooc.app.repository.AiMessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AiChatServiceRagTest {

    @Autowired private AiChatService aiChatService;
    @Autowired private AiConversationRepository conversationRepository;
    @Autowired private AiMessageRepository messageRepository;
    @Autowired private VectorStore vectorStore;

    @MockBean private ChatClient chatClient;

    private ChatClient.ChatClientRequestSpec requestSpec;
    private ChatClient.StreamResponseSpec streamResponseSpec;

    @BeforeEach
    void setupChatClientMock() {
        requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        streamResponseSpec = mock(ChatClient.StreamResponseSpec.class);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.messages(anyList())).thenReturn(requestSpec);
        when(requestSpec.stream()).thenReturn(streamResponseSpec);
    }

    @Test
    void sendMessage_withKnowledgeResults_injectsSystemMessage() {
        // GIVEN - populate vector store with knowledge
        Document doc = new Document("City: Beijing (北京)\nDescription: Ancient capital with imperial grandeur",
                Map.of("entity_type", "city", "name", "Beijing", "name_zh", "北京"));
        vectorStore.add(List.of(doc));

        UUID convId = createTestConversation();
        when(streamResponseSpec.content()).thenReturn(Flux.just("Beijing is great"));

        // WHEN
        aiChatService.sendMessage(convId, "Tell me about Beijing");

        // THEN - verify a SystemMessage with knowledge context was included
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Message>> captor = ArgumentCaptor.forClass(List.class);
        verify(requestSpec).messages(captor.capture());

        List<Message> messages = captor.getValue();
        assertTrue(messages.stream().anyMatch(m -> m instanceof SystemMessage
                && m.getText().contains("Reference knowledge")));
    }

    @Test
    void sendMessage_withNoKnowledgeResults_noSystemMessage() {
        // GIVEN - empty vector store (no documents added)
        UUID convId = createTestConversation();
        when(streamResponseSpec.content()).thenReturn(Flux.just("ok"));

        // WHEN
        aiChatService.sendMessage(convId, "What is the weather");

        // THEN - no SystemMessage should be present
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Message>> captor = ArgumentCaptor.forClass(List.class);
        verify(requestSpec).messages(captor.capture());

        List<Message> messages = captor.getValue();
        assertTrue(messages.stream().noneMatch(m -> m instanceof SystemMessage));
    }

    @Test
    void sendMessage_withKnowledgeResults_includesUserAndAssistantMessages() {
        // GIVEN
        Document doc = new Document("Spot: Forbidden City\nCity: Beijing",
                Map.of("entity_type", "spot", "name", "Forbidden City", "city_name", "Beijing"));
        vectorStore.add(List.of(doc));

        UUID convId = createTestConversation();
        // add some history
        saveMessage(convId, AiMessageRole.USER, "Hello");
        saveMessage(convId, AiMessageRole.ASSISTANT, "Hi there!");
        when(streamResponseSpec.content()).thenReturn(Flux.just("response"));

        // WHEN
        aiChatService.sendMessage(convId, "Recommend Beijing spots");

        // THEN
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Message>> captor = ArgumentCaptor.forClass(List.class);
        verify(requestSpec).messages(captor.capture());

        List<Message> messages = captor.getValue();
        // Should have: SystemMessage + history (user+assistant) + current user message
        assertTrue(messages.stream().anyMatch(m -> m instanceof SystemMessage));
        assertTrue(messages.stream().anyMatch(m -> m instanceof UserMessage
                && m.getText().equals("Recommend Beijing spots")));
        assertTrue(messages.stream().anyMatch(m -> m instanceof UserMessage
                && m.getText().equals("Hello")));
    }

    private UUID createTestConversation() {
        AiConversation conv = new AiConversation();
        conv.setLastMessageAt(Instant.now());
        conversationRepository.save(conv);
        return conv.getId();
    }

    private void saveMessage(UUID convId, AiMessageRole role, String content) {
        AiMessage msg = new AiMessage();
        msg.setConversationId(convId);
        msg.setRole(role);
        msg.setContent(content);
        messageRepository.save(msg);
    }
}
