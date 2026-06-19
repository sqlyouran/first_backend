package com.mooc.app.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "conversations")
public class ConversationEntity extends BaseEntity {

    @Column(name = "user_a_id", nullable = false)
    private UUID userAId;

    @Column(name = "user_b_id", nullable = false)
    private UUID userBId;

    @Column(name = "last_message_at")
    private Instant lastMessageAt;

    public UUID getUserAId() { return userAId; }
    public void setUserAId(UUID userAId) { this.userAId = userAId; }

    public UUID getUserBId() { return userBId; }
    public void setUserBId(UUID userBId) { this.userBId = userBId; }

    public Instant getLastMessageAt() { return lastMessageAt; }
    public void setLastMessageAt(Instant lastMessageAt) { this.lastMessageAt = lastMessageAt; }
}
