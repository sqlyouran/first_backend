package com.mooc.app.service;

import com.mooc.app.dto.SendMessageRequest;
import com.mooc.app.dto.response.*;
import com.mooc.app.entity.ConversationEntity;
import com.mooc.app.entity.MessageEntity;
import com.mooc.app.entity.UserEntity;
import com.mooc.app.exception.MessageException;
import com.mooc.app.repository.ConversationRepository;
import com.mooc.app.repository.MessageRepository;
import com.mooc.app.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class MessageService {

    private static final Logger log = LoggerFactory.getLogger(MessageService.class);
    private static final int RATE_LIMIT_MESSAGES = 20;
    private static final long RATE_LIMIT_WINDOW_SECONDS = 60;

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;

    public MessageService(MessageRepository messageRepository,
                           ConversationRepository conversationRepository,
                           UserRepository userRepository) {
        this.messageRepository = messageRepository;
        this.conversationRepository = conversationRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public MessageListResponse listMessages(UUID conversationId, UUID userId,
                                             int page, int size, String requestId) {
        ConversationEntity conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new MessageException(HttpStatus.NOT_FOUND, "not_found",
                        "Conversation not found"));

        if (!isParticipant(conversation, userId)) {
            throw new MessageException(HttpStatus.FORBIDDEN, "access_denied",
                    "You are not a participant of this conversation");
        }

        if (size > 100) size = 100;
        if (size < 1) size = 50;
        if (page < 1) page = 1;

        Pageable pageable = PageRequest.of(page - 1, size);
        Page<MessageEntity> messagePage = messageRepository.findByConversation(conversationId, pageable);

        List<MessageItemResponse> items = new ArrayList<>();
        for (MessageEntity msg : messagePage.getContent()) {
            items.add(new MessageItemResponse(
                    msg.getId().toString(), msg.getSenderId().toString(),
                    msg.getContent(), msg.isRead(), msg.getCreatedAt()));
        }

        return new MessageListResponse(requestId, items,
                messagePage.getTotalElements(), page, size);
    }

    @Transactional
    public SendMessageResponse sendMessage(UUID conversationId, UUID senderId,
                                            SendMessageRequest request, String requestId) {
        ConversationEntity conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new MessageException(HttpStatus.NOT_FOUND, "not_found",
                        "Conversation not found"));

        if (!isParticipant(conversation, senderId)) {
            throw new MessageException(HttpStatus.FORBIDDEN, "access_denied",
                    "You are not a participant of this conversation");
        }

        UUID recipientId = conversation.getUserAId().equals(senderId)
                ? conversation.getUserBId() : conversation.getUserAId();
        UserEntity recipient = userRepository.findById(recipientId).orElse(null);
        if (recipient != null && recipient.getState() == UserEntity.State.deleted) {
            throw new MessageException(HttpStatus.UNPROCESSABLE_ENTITY, "user_unavailable",
                    "Recipient account has been deleted");
        }

        Instant windowStart = Instant.now().minusSeconds(RATE_LIMIT_WINDOW_SECONDS);
        long recentCount = messageRepository.countRecentMessages(conversationId, senderId, windowStart);
        if (recentCount >= RATE_LIMIT_MESSAGES) {
            throw new MessageException(HttpStatus.TOO_MANY_REQUESTS, "rate_limited",
                    "Too many messages. Please wait before sending more.");
        }

        MessageEntity message = new MessageEntity();
        message.setConversationId(conversationId);
        message.setSenderId(senderId);
        message.setContent(request.content());
        message = messageRepository.save(message);

        conversation.setLastMessageAt(message.getCreatedAt());
        conversationRepository.save(conversation);

        log.info("Message sent [conversationId={}, senderId={}, messageId={}]",
                conversationId, senderId, message.getId());

        return new SendMessageResponse(requestId,
                new MessageItemResponse(message.getId().toString(), message.getSenderId().toString(),
                        message.getContent(), message.isRead(), message.getCreatedAt()));
    }

    @Transactional
    public MarkReadResponse markRead(UUID conversationId, UUID userId, String requestId) {
        ConversationEntity conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new MessageException(HttpStatus.NOT_FOUND, "not_found",
                        "Conversation not found"));

        if (!isParticipant(conversation, userId)) {
            throw new MessageException(HttpStatus.FORBIDDEN, "access_denied",
                    "You are not a participant of this conversation");
        }

        UUID otherUserId = conversation.getUserAId().equals(userId)
                ? conversation.getUserBId() : conversation.getUserAId();
        int markedCount = messageRepository.markAllAsRead(conversationId, otherUserId, Instant.now());

        log.info("Messages marked as read [conversationId={}, userId={}, markedCount={}]",
                conversationId, userId, markedCount);

        return new MarkReadResponse(requestId, markedCount);
    }

    private boolean isParticipant(ConversationEntity conversation, UUID userId) {
        return conversation.getUserAId().equals(userId) || conversation.getUserBId().equals(userId);
    }
}
