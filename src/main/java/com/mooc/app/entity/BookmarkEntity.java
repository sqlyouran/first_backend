package com.mooc.app.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Entity
@Table(name = "bookmarks", uniqueConstraints = @UniqueConstraint(columnNames = {"entity_id", "entity_type", "user_id"}))
public class BookmarkEntity extends BaseEntity {

    @NotNull(message = "Entity ID must not be null")
    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    @NotNull(message = "Entity type must not be null")
    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 20)
    private EntityType entityType;

    @NotNull(message = "User ID must not be null")
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    public UUID getEntityId() { return entityId; }
    public void setEntityId(UUID entityId) { this.entityId = entityId; }

    public EntityType getEntityType() { return entityType; }
    public void setEntityType(EntityType entityType) { this.entityType = entityType; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
}
