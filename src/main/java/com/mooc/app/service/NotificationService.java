package com.mooc.app.service;

import com.mooc.app.entity.NotificationEntity;
import com.mooc.app.entity.NotificationType;
import com.mooc.app.entity.PostEntity;
import com.mooc.app.entity.UserEntity;
import com.mooc.app.exception.NotificationException;
import com.mooc.app.repository.NotificationRepository;
import com.mooc.app.repository.PostRepository;
import com.mooc.app.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;

    public NotificationService(NotificationRepository notificationRepository,
                                UserRepository userRepository,
                                PostRepository postRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.postRepository = postRepository;
    }

    @Transactional
    public void createNotification(UUID recipientId, UUID actorId, NotificationType type,
                                    UUID entityId, String entityType, String contentPreview) {
        if (recipientId.equals(actorId)) {
            return;
        }

        Optional<NotificationEntity> existing = notificationRepository
                .findByRecipientIdAndActorIdAndTypeAndEntityIdAndDeletedFalse(recipientId, actorId, type, entityId);

        if (existing.isPresent()) {
            NotificationEntity notification = existing.get();
            if (contentPreview != null) {
                notification.setContentPreview(contentPreview);
            }
            notificationRepository.save(notification);
            notificationRepository.refreshCreatedAt(notification.getId(), Instant.now());
            return;
        }

        NotificationEntity notification = new NotificationEntity();
        notification.setRecipientId(recipientId);
        notification.setActorId(actorId);
        notification.setType(type);
        notification.setEntityId(entityId);
        notification.setEntityType(entityType);
        notification.setContentPreview(contentPreview);
        notificationRepository.save(notification);
    }

    public void deleteNotification(UUID recipientId, UUID actorId, NotificationType type, UUID entityId) {
        notificationRepository
                .findByRecipientIdAndActorIdAndTypeAndEntityIdAndDeletedFalse(recipientId, actorId, type, entityId)
                .ifPresent(notificationRepository::delete);
    }

    public Page<NotificationEntity> listNotifications(UUID recipientId, int page, int size) {
        if (size > 100) {
            throw new NotificationException(HttpStatus.BAD_REQUEST, "validation_error",
                    "Page size must not exceed 100");
        }
        return notificationRepository.findByRecipientIdAndDeletedFalse(recipientId, PageRequest.of(page - 1, size));
    }

    @Transactional
    public void markAsRead(UUID notificationId, UUID userId) {
        NotificationEntity notification = notificationRepository
                .findByIdAndRecipientIdAndDeletedFalse(notificationId, userId)
                .orElseThrow(() -> new NotificationException(HttpStatus.NOT_FOUND, "not_found",
                        "Notification not found"));
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public int markAllAsRead(UUID userId) {
        return notificationRepository.markAllReadByRecipientId(userId);
    }

    public long countUnread(UUID userId) {
        return notificationRepository.countByRecipientIdAndReadFalseAndDeletedFalse(userId);
    }

    public Map<UUID, UserEntity> batchResolveActors(List<NotificationEntity> notifications) {
        List<UUID> actorIds = notifications.stream()
                .map(NotificationEntity::getActorId)
                .distinct()
                .toList();
        if (actorIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, UserEntity> result = new HashMap<>();
        userRepository.findAllById(actorIds).forEach(u -> result.put(u.getId(), u));
        return result;
    }

    public Map<UUID, String> batchResolveTargetTitles(List<NotificationEntity> notifications) {
        List<UUID> entityIds = notifications.stream()
                .map(NotificationEntity::getEntityId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        if (entityIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, String> result = new HashMap<>();
        postRepository.findAllById(entityIds).stream()
                .filter(p -> !p.isDeleted())
                .forEach(p -> result.put(p.getId(), p.getTitle()));
        return result;
    }
}
