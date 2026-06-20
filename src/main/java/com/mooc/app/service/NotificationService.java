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

    public String resolveActorNickname(UUID actorId) {
        return userRepository.findById(actorId)
                .map(u -> u.getNickname() != null ? u.getNickname() : u.getEmail())
                .orElse("Unknown");
    }

    public String resolveActorAvatarUrl(UUID actorId) {
        return userRepository.findById(actorId)
                .map(UserEntity::getAvatarUrl)
                .orElse(null);
    }

    public String resolveActorUsername(UUID actorId) {
        return userRepository.findById(actorId)
                .map(UserEntity::getUsername)
                .orElse(null);
    }

    public String resolveTargetTitle(UUID entityId) {
        if (entityId == null) return null;
        return postRepository.findByIdAndDeletedFalse(entityId)
                .map(PostEntity::getTitle)
                .orElse(null);
    }
}
