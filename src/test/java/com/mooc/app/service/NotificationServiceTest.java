package com.mooc.app.service;

import com.mooc.app.entity.*;
import com.mooc.app.repository.NotificationRepository;
import com.mooc.app.repository.PostRepository;
import com.mooc.app.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PostRepository postRepository;

    @InjectMocks
    private NotificationService notificationService;

    private final UUID actorId = UUID.randomUUID();
    private final UUID recipientId = UUID.randomUUID();
    private final UUID postId = UUID.randomUUID();

    // === Task 2.1: createNotification success ===

    @Test
    void createNotification_postLiked_createsPostLikedNotification() {
        when(notificationRepository.findByRecipientIdAndActorIdAndTypeAndEntityIdAndDeletedFalse(
                recipientId, actorId, NotificationType.POST_LIKED, postId))
                .thenReturn(Optional.empty());

        notificationService.createNotification(recipientId, actorId, NotificationType.POST_LIKED, postId, "post", null);

        ArgumentCaptor<NotificationEntity> captor = ArgumentCaptor.forClass(NotificationEntity.class);
        verify(notificationRepository).save(captor.capture());
        NotificationEntity saved = captor.getValue();
        assertEquals(recipientId, saved.getRecipientId());
        assertEquals(actorId, saved.getActorId());
        assertEquals(NotificationType.POST_LIKED, saved.getType());
        assertEquals(postId, saved.getEntityId());
        assertEquals("post", saved.getEntityType());
        assertFalse(saved.isRead());
    }

    // === Task 2.2: Self-filter ===

    @Test
    void createNotification_selfLike_doesNotCreateNotification() {
        notificationService.createNotification(actorId, actorId, NotificationType.POST_LIKED, postId, "post", null);

        verify(notificationRepository, never()).save(any());
    }

    // === Task 2.3: Deduplication ===

    @Test
    void createNotification_duplicateInteraction_updatesExistingNotification() {
        NotificationEntity existing = new NotificationEntity();
        existing.setId(UUID.randomUUID());
        existing.setRecipientId(recipientId);
        existing.setActorId(actorId);
        existing.setType(NotificationType.POST_LIKED);
        existing.setEntityId(postId);
        when(notificationRepository.findByRecipientIdAndActorIdAndTypeAndEntityIdAndDeletedFalse(
                recipientId, actorId, NotificationType.POST_LIKED, postId))
                .thenReturn(Optional.of(existing));

        notificationService.createNotification(recipientId, actorId, NotificationType.POST_LIKED, postId, "post", null);

        verify(notificationRepository).save(existing);
        verify(notificationRepository, never()).save(argThat(n -> n.getId() == null));
    }

    @Test
    void createNotification_withContentPreview_storesPreview() {
        when(notificationRepository.findByRecipientIdAndActorIdAndTypeAndEntityIdAndDeletedFalse(
                recipientId, actorId, NotificationType.POST_COMMENTED, postId))
                .thenReturn(Optional.empty());

        String preview = "This is a great post!";
        notificationService.createNotification(recipientId, actorId, NotificationType.POST_COMMENTED, postId, "post", preview);

        ArgumentCaptor<NotificationEntity> captor = ArgumentCaptor.forClass(NotificationEntity.class);
        verify(notificationRepository).save(captor.capture());
        assertEquals(preview, captor.getValue().getContentPreview());
    }

    // === Task 2.5: deleteNotification ===

    @Test
    void deleteNotification_existingNotification_marksDeleted() {
        NotificationEntity existing = new NotificationEntity();
        existing.setId(UUID.randomUUID());
        when(notificationRepository.findByRecipientIdAndActorIdAndTypeAndEntityIdAndDeletedFalse(
                recipientId, actorId, NotificationType.POST_LIKED, postId))
                .thenReturn(Optional.of(existing));

        notificationService.deleteNotification(recipientId, actorId, NotificationType.POST_LIKED, postId);

        verify(notificationRepository).delete(existing);
    }

    @Test
    void deleteNotification_noExistingNotification_doesNothing() {
        when(notificationRepository.findByRecipientIdAndActorIdAndTypeAndEntityIdAndDeletedFalse(
                recipientId, actorId, NotificationType.POST_LIKED, postId))
                .thenReturn(Optional.empty());

        notificationService.deleteNotification(recipientId, actorId, NotificationType.POST_LIKED, postId);

        verify(notificationRepository, never()).delete(any());
    }

    // === Batch resolve tests ===

    @Test
    void batchResolveActors_multipleNotifications_singleQuery() {
        UUID actor1 = UUID.randomUUID();
        UUID actor2 = UUID.randomUUID();

        NotificationEntity n1 = new NotificationEntity();
        n1.setActorId(actor1);
        NotificationEntity n2 = new NotificationEntity();
        n2.setActorId(actor2);
        NotificationEntity n3 = new NotificationEntity();
        n3.setActorId(actor1);

        UserEntity user1 = new UserEntity();
        user1.setId(actor1);
        user1.setNickname("Alice");
        user1.setEmail("alice@test.com");
        UserEntity user2 = new UserEntity();
        user2.setId(actor2);
        user2.setNickname("Bob");
        user2.setEmail("bob@test.com");

        when(userRepository.findAllById(List.of(actor1, actor2)))
                .thenReturn(List.of(user1, user2));

        Map<UUID, UserEntity> result = notificationService.batchResolveActors(List.of(n1, n2, n3));

        assertEquals(2, result.size());
        assertEquals("Alice", result.get(actor1).getNickname());
        assertEquals("Bob", result.get(actor2).getNickname());
        verify(userRepository).findAllById(List.of(actor1, actor2));
    }

    @Test
    void batchResolveActors_emptyList_returnsEmptyMap() {
        Map<UUID, UserEntity> result = notificationService.batchResolveActors(List.of());
        assertTrue(result.isEmpty());
        verify(userRepository, never()).findAllById(any());
    }

    @Test
    void batchResolveTargetTitles_filtersNullEntityIds() {
        NotificationEntity n1 = new NotificationEntity();
        n1.setEntityId(postId);
        NotificationEntity n2 = new NotificationEntity();
        n2.setEntityId(null);

        PostEntity post = new PostEntity();
        post.setId(postId);
        post.setTitle("Test Post");
        when(postRepository.findAllById(List.of(postId))).thenReturn(List.of(post));

        Map<UUID, String> result = notificationService.batchResolveTargetTitles(List.of(n1, n2));

        assertEquals(1, result.size());
        assertEquals("Test Post", result.get(postId));
    }

    @Test
    void batchResolveTargetTitles_allNullEntityIds_returnsEmptyMap() {
        NotificationEntity n1 = new NotificationEntity();
        n1.setEntityId(null);

        Map<UUID, String> result = notificationService.batchResolveTargetTitles(List.of(n1));
        assertTrue(result.isEmpty());
        verify(postRepository, never()).findAllById(any());
    }

}
