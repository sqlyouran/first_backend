package com.mooc.app.service;

import com.mooc.app.dto.response.ConversationListResponse;
import com.mooc.app.dto.response.UnreadCountResponse;
import com.mooc.app.entity.ConversationEntity;
import com.mooc.app.entity.MessageEntity;
import com.mooc.app.entity.UserEntity;
import com.mooc.app.repository.ConversationRepository;
import com.mooc.app.repository.MessageRepository;
import com.mooc.app.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ConversationService conversationService;

    private final UUID userId = UUID.randomUUID();
    private final UUID otherUserId = UUID.randomUUID();

    @Test
    void listConversations_batchFetchesUsers() {
        ConversationEntity conv = createConversation(userId, otherUserId);
        Page<ConversationEntity> page = new PageImpl<>(List.of(conv));
        when(conversationRepository.findByUser(eq(userId), any())).thenReturn(page);

        UserEntity otherUser = new UserEntity();
        otherUser.setId(otherUserId);
        otherUser.setUsername("otheruser");
        otherUser.setNickname("Other");
        when(userRepository.findAllById(List.of(otherUserId))).thenReturn(List.of(otherUser));
        when(messageRepository.findLatestMessagesByConversationIds(anyList())).thenReturn(List.of());
        when(messageRepository.batchCountUnread(anyList(), anyList())).thenReturn(List.of());

        ConversationListResponse response = conversationService.listConversations(userId, 1, 20, "req-1");

        assertEquals(1, response.getItems().size());
        assertEquals("otheruser", response.getItems().get(0).getOtherUser().getUsername());
        verify(userRepository).findAllById(List.of(otherUserId));
        // Verify no per-conversation findById calls
        verify(userRepository, never()).findById(any());
    }

    @Test
    void getUnreadCount_usesAggregateQuery() {
        when(messageRepository.countTotalUnread(userId)).thenReturn(5L);

        UnreadCountResponse response = conversationService.getUnreadCount(userId, "req-1");

        assertEquals(5L, response.getCount());
        verify(messageRepository).countTotalUnread(userId);
        // Verify no full-load of conversations
        verify(conversationRepository, never()).findByUser(eq(userId), any());
    }

    private ConversationEntity createConversation(UUID userAId, UUID userBId) {
        ConversationEntity conv = new ConversationEntity();
        conv.setId(UUID.randomUUID());
        conv.setUserAId(userAId);
        conv.setUserBId(userBId);
        conv.setLastMessageAt(Instant.now());
        return conv;
    }
}
