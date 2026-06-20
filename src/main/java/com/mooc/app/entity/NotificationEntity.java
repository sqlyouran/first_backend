package com.mooc.app.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_notifications_recipient_read_created", columnList = "recipient_id, is_read, created_at")
})
public class NotificationEntity extends BaseEntity {

    @NotNull(message = "Recipient ID must not be null")
    @Column(name = "recipient_id", nullable = false)
    private UUID recipientId;

    @NotNull(message = "Actor ID must not be null")
    @Column(name = "actor_id", nullable = false)
    private UUID actorId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    @Column(name = "entity_id")
    private UUID entityId;

    @Column(name = "entity_type", length = 20)
    private String entityType;

    @Column(name = "content_preview", length = 200)
    private String contentPreview;

    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    public UUID getRecipientId() { return recipientId; }
    public void setRecipientId(UUID recipientId) { this.recipientId = recipientId; }

    public UUID getActorId() { return actorId; }
    public void setActorId(UUID actorId) { this.actorId = actorId; }

    public NotificationType getType() { return type; }
    public void setType(NotificationType type) { this.type = type; }

    public UUID getEntityId() { return entityId; }
    public void setEntityId(UUID entityId) { this.entityId = entityId; }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public String getContentPreview() { return contentPreview; }
    public void setContentPreview(String contentPreview) { this.contentPreview = contentPreview; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
}
