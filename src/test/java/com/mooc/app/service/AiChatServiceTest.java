package com.mooc.app.service;

import com.mooc.app.dto.response.AiConversationResponse;
import com.mooc.app.entity.AiConversation;
import com.mooc.app.entity.AiMessage;
import com.mooc.app.entity.AiMessageRole;
import com.mooc.app.exception.AiChatException;
import com.mooc.app.repository.AiConversationRepository;
import com.mooc.app.repository.AiMessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AiChatServiceTest {

    @Autowired private AiChatService aiChatService;
    @Autowired private AiConversationRepository conversationRepository;
    @Autowired private AiMessageRepository messageRepository;

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
    void createConversation_anonymous_returnsResponseWithIdAndCreatedAt() {
        AiConversationResponse response = aiChatService.createConversation(null, "req-1");

        assertNotNull(response);
        assertNotNull(response.getId());
        assertNotNull(response.getCreatedAt());

        UUID convId = UUID.fromString(response.getId());
        AiConversation saved = conversationRepository.findById(convId).orElseThrow();
        assertNull(saved.getUserId());
    }

    @Test
    void createConversation_withUserId_persistsUserId() {
        UUID userId = UUID.randomUUID();

        AiConversationResponse response = aiChatService.createConversation(userId, "req-2");

        UUID convId = UUID.fromString(response.getId());
        AiConversation saved = conversationRepository.findById(convId).orElseThrow();
        assertEquals(userId, saved.getUserId());
    }

    @Test
    void sendMessage_persistsUserMessage() {
        UUID convId = createTestConversation(null);

        when(streamResponseSpec.content()).thenReturn(Flux.just("ok"));
        aiChatService.sendMessage(convId, "hello");

        List<AiMessage> messages = messageRepository
                .findByConversationIdAndDeletedFalseOrderByCreatedAtAsc(convId);
        assertTrue(messages.stream().anyMatch(m ->
                m.getRole() == AiMessageRole.USER && "hello".equals(m.getContent())));
    }

    @Test
    void sendMessage_persistsAssistantMessageOnComplete() {
        UUID convId = createTestConversation(null);
        when(streamResponseSpec.content()).thenReturn(Flux.just("Hello", " world"));

        aiChatService.sendMessage(convId, "hi");

        List<AiMessage> messages = messageRepository
                .findByConversationIdAndDeletedFalseOrderByCreatedAtAsc(convId);
        assertTrue(messages.stream().anyMatch(m ->
                m.getRole() == AiMessageRole.ASSISTANT && "Hello world".equals(m.getContent())));
    }

    @Test
    void sendMessage_returnsSseEmitter() {
        UUID convId = createTestConversation(null);
        when(streamResponseSpec.content()).thenReturn(Flux.just("token"));

        SseEmitter emitter = aiChatService.sendMessage(convId, "test");

        assertNotNull(emitter);
    }

    @Test
    void sendMessage_conversationNotFound_throwsAiChatException() {
        AiChatException ex = assertThrows(AiChatException.class,
                () -> aiChatService.sendMessage(UUID.randomUUID(), "hello"));
        assertEquals("not_found", ex.getErrorCode());
    }

    @Test
    void sendMessage_firstMessage_setsConversationTitle() {
        UUID convId = createTestConversation(null);
        when(streamResponseSpec.content()).thenReturn(Flux.just("ok"));

        aiChatService.sendMessage(convId, "Plan a trip to Beijing");

        AiConversation conv = conversationRepository.findById(convId).orElseThrow();
        assertEquals("Plan a trip to Beijing", conv.getTitle());
    }

    @Test
    void sendMessage_contextWindowLimitsHistory() {
        UUID convId = createTestConversation(null);

        for (int i = 0; i < 15; i++) {
            saveMessage(convId, AiMessageRole.USER, "round-" + i + "-user");
            saveMessage(convId, AiMessageRole.ASSISTANT, "round-" + i + "-assistant");
        }
        when(streamResponseSpec.content()).thenReturn(Flux.just("reply"));

        aiChatService.sendMessage(convId, "current message");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Message>> captor = ArgumentCaptor.forClass(List.class);
        verify(requestSpec).messages(captor.capture());

        assertEquals(21, captor.getValue().size());
        assertTrue(captor.getValue().get(0) instanceof UserMessage);
        assertEquals("round-5-user", captor.getValue().get(0).getText());
        assertEquals("current message",
                captor.getValue().get(captor.getValue().size() - 1).getText());
    }

    @Test
    void sendMessage_contextWindowIncludesAllWhenWithinLimit() {
        UUID convId = createTestConversation(null);

        for (int i = 0; i < 2; i++) {
            saveMessage(convId, AiMessageRole.USER, "old-round-" + i + "-user");
            saveMessage(convId, AiMessageRole.ASSISTANT, "old-round-" + i + "-assistant");
        }
        when(streamResponseSpec.content()).thenReturn(Flux.just("reply"));

        aiChatService.sendMessage(convId, "current message");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Message>> captor = ArgumentCaptor.forClass(List.class);
        verify(requestSpec).messages(captor.capture());

        assertEquals(5, captor.getValue().size());
    }

    private UUID createTestConversation(UUID userId) {
        AiConversation conv = new AiConversation();
        conv.setUserId(userId);
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
