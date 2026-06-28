package com.mooc.app.service;

import com.mooc.app.dto.response.AiConversationResponse;
import com.mooc.app.entity.AiConversation;
import com.mooc.app.entity.AiMessage;
import com.mooc.app.entity.AiMessageRole;
import com.mooc.app.exception.AiChatException;
import com.mooc.app.repository.AiConversationRepository;
import com.mooc.app.repository.AiMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class AiChatService {

    private static final Logger log = LoggerFactory.getLogger(AiChatService.class);

    private static final int CONTEXT_WINDOW_ROUNDS = 10;
    private static final long SSE_TIMEOUT_MS = 60_000L;
    private static final int TITLE_MAX_LENGTH = 100;

    private final AiConversationRepository conversationRepository;
    private final AiMessageRepository messageRepository;
    private final ChatClient chatClient;

    public AiChatService(AiConversationRepository conversationRepository,
                        AiMessageRepository messageRepository,
                        ChatClient chatClient) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.chatClient = chatClient;
    }

    public AiConversationResponse createConversation(UUID userId, String requestId) {
        AiConversation conversation = new AiConversation();
        conversation.setUserId(userId);
        conversation.setLastMessageAt(Instant.now());
        conversationRepository.save(conversation);

        return new AiConversationResponse(
                requestId,
                conversation.getId().toString(),
                conversation.getCreatedAt().toString());
    }

    public SseEmitter sendMessage(UUID conversationId, String message) {
        AiConversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new AiChatException(HttpStatus.NOT_FOUND, "not_found",
                        "Conversation not found"));

        AiMessage userMessage = new AiMessage();
        userMessage.setConversationId(conversationId);
        userMessage.setRole(AiMessageRole.USER);
        userMessage.setContent(message);
        messageRepository.save(userMessage);

        if (conversation.getTitle() == null) {
            String title = message.length() > TITLE_MAX_LENGTH
                    ? message.substring(0, TITLE_MAX_LENGTH) : message;
            conversation.setTitle(title);
        }
        conversation.setLastMessageAt(Instant.now());
        conversationRepository.save(conversation);

        List<Message> contextMessages = buildContextMessages(conversationId);

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        StringBuilder fullResponse = new StringBuilder();

        chatClient.prompt()
                .messages(contextMessages)
                .stream()
                .content()
                .subscribe(
                        token -> {
                            fullResponse.append(token);
                            try {
                                emitter.send(SseEmitter.event().name("token").data(token));
                            } catch (Exception e) {
                                log.warn("Failed to send SSE token event [conversationId={}]", conversationId, e);
                            }
                        },
                        error -> {
                            log.error("AI chat stream error [conversationId={}]", conversationId, error);
                            try {
                                emitter.send(SseEmitter.event().name("error").data(error.getMessage()));
                            } catch (Exception e) {
                                log.warn("Failed to send SSE error event", e);
                            }
                            emitter.complete();
                        },
                        () -> {
                            AiMessage assistantMessage = new AiMessage();
                            assistantMessage.setConversationId(conversationId);
                            assistantMessage.setRole(AiMessageRole.ASSISTANT);
                            assistantMessage.setContent(fullResponse.toString());
                            messageRepository.save(assistantMessage);

                            try {
                                emitter.send(SseEmitter.event().name("done").data(""));
                            } catch (Exception e) {
                                log.warn("Failed to send SSE done event", e);
                            }
                            emitter.complete();
                        }
                );

        return emitter;
    }

    private List<Message> buildContextMessages(UUID conversationId) {
        List<AiMessage> allMessages = messageRepository
                .findByConversationIdAndDeletedFalseOrderByCreatedAtAsc(conversationId);

        int maxMessages = CONTEXT_WINDOW_ROUNDS * 2 + 1;
        List<AiMessage> windowedMessages;
        if (allMessages.size() > maxMessages) {
            windowedMessages = allMessages.subList(allMessages.size() - maxMessages, allMessages.size());
        } else {
            windowedMessages = allMessages;
        }

        List<Message> contextMessages = new ArrayList<>(windowedMessages.size());
        for (AiMessage msg : windowedMessages) {
            if (msg.getRole() == AiMessageRole.USER) {
                contextMessages.add(new UserMessage(msg.getContent()));
            } else {
                contextMessages.add(new AssistantMessage(msg.getContent()));
            }
        }
        return contextMessages;
    }
}
