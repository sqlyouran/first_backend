package com.mooc.app.repository;

import com.mooc.app.entity.NotificationEntity;
import com.mooc.app.entity.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<NotificationEntity, UUID> {

    Page<NotificationEntity> findByRecipientIdAndDeletedFalse(UUID recipientId, Pageable pageable);

    List<NotificationEntity> findByRecipientIdAndReadFalseAndDeletedFalse(UUID recipientId);

    long countByRecipientIdAndReadFalseAndDeletedFalse(UUID recipientId);

    Optional<NotificationEntity> findByRecipientIdAndActorIdAndTypeAndEntityIdAndDeletedFalse(
            UUID recipientId, UUID actorId, NotificationType type, UUID entityId);

    Optional<NotificationEntity> findByIdAndRecipientIdAndDeletedFalse(UUID id, UUID recipientId);

    @Modifying
    @Query("UPDATE NotificationEntity n SET n.read = true WHERE n.recipientId = :recipientId AND n.read = false AND n.deleted = false")
    int markAllReadByRecipientId(@Param("recipientId") UUID recipientId);
}
