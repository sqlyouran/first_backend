package com.mooc.app.service;

import com.mooc.app.dto.CreateConversationRequest;
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
public class ConversationService {

    private static final Logger log = LoggerFactory.getLogger(ConversationService.class);

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    public ConversationService(ConversationRepository conversationRepository,
                                MessageRepository messageRepository,
                                UserRepository userRepository) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public CreateConversationResponse createConversation(CreateConversationRequest request,
                                                          UUID senderId, String requestId) {
        UserEntity recipient = userRepository.findByUsername(request.recipient_username())
                .orElseThrow(() -> new MessageException(HttpStatus.NOT_FOUND, "not_found",
                        "Recipient not found"));

        UUID recipientId = recipient.getId();

        if (senderId.equals(recipientId)) {
            throw new MessageException(HttpStatus.UNPROCESSABLE_ENTITY, "validation_error",
                    "Cannot create conversation with yourself");
        }

        if (recipient.getState() == UserEntity.State.deleted) {
            throw new MessageException(HttpStatus.UNPROCESSABLE_ENTITY, "recipient_unavailable",
                    "Recipient account has been deleted");
        }

        ConversationEntity conversation = conversationRepository
                .findBetweenUsers(senderId, recipientId)
                .orElseGet(() -> {
                    ConversationEntity newConv = new ConversationEntity();
                    newConv.setUserAId(senderId);
                    newConv.setUserBId(recipientId);
                    newConv.setLastMessageAt(Instant.now());
                    return conversationRepository.save(newConv);
                });

        MessageEntity message = new MessageEntity();
        message.setConversationId(conversation.getId());
        message.setSenderId(senderId);
        message.setContent(request.content());
        message = messageRepository.save(message);

        conversation.setLastMessageAt(message.getCreatedAt());
        conversationRepository.save(conversation);

        log.info("Conversation created/updated [conversationId={}, senderId={}, recipientId={}]",
                conversation.getId(), senderId, recipientId);

        return new CreateConversationResponse(requestId,
                conversation.getId().toString(), message.getId().toString());
    }

    @Transactional(readOnly = true)
    public ConversationListResponse listConversations(UUID userId, int page, int size, String requestId) {
        if (size > 100) size = 100;
        if (size < 1) size = 20;
        if (page < 1) page = 1;

        Pageable pageable = PageRequest.of(page - 1, size);
        Page<ConversationEntity> conversationPage = conversationRepository.findByUser(userId, pageable);

        List<ConversationItemResponse> items = new ArrayList<>();
        for (ConversationEntity conv : conversationPage.getContent()) {
            UUID otherUserId = conv.getUserAId().equals(userId) ? conv.getUserBId() : conv.getUserAId();
            UserEntity otherUser = userRepository.findById(otherUserId).orElse(null);

            ConversationItemResponse.OtherUserInfo otherUserInfo;
            if (otherUser != null) {
                otherUserInfo = new ConversationItemResponse.OtherUserInfo(
                        otherUser.getId().toString(),
                        otherUser.getUsername(),
                        otherUser.getNickname(),
                        otherUser.getAvatarUrl(),
                        otherUser.getState() == UserEntity.State.deleted
                );
            } else {
                otherUserInfo = new ConversationItemResponse.OtherUserInfo(
                        otherUserId.toString(), null, null, null, true);
            }

            Page<MessageEntity> lastMessagePage = messageRepository.findByConversation(
                    conv.getId(), PageRequest.of(0, 1));
            String lastMessageContent = lastMessagePage.hasContent()
                    ? lastMessagePage.getContent().get(0).getContent() : "";

            long unreadCount = messageRepository.countByConversationIdAndSenderIdAndReadFalse(
                    conv.getId(), otherUserId);

            items.add(new ConversationItemResponse(
                    conv.getId().toString(), otherUserInfo,
                    lastMessageContent, conv.getLastMessageAt(), unreadCount));
        }

        return new ConversationListResponse(requestId, items,
                conversationPage.getTotalElements(), page, size);
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCount(UUID userId, String requestId) {
        Page<ConversationEntity> allConversations = conversationRepository.findByUser(
                userId, PageRequest.of(0, Integer.MAX_VALUE));

        long totalUnread = 0;
        for (ConversationEntity conv : allConversations.getContent()) {
            UUID otherUserId = conv.getUserAId().equals(userId) ? conv.getUserBId() : conv.getUserAId();
            totalUnread += messageRepository.countByConversationIdAndSenderIdAndReadFalse(
                    conv.getId(), otherUserId);
        }

        return new UnreadCountResponse(requestId, totalUnread);
    }
}
