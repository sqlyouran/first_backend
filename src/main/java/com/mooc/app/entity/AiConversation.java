package com.mooc.app.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_conversations", indexes = {
    @Index(name = "idx_ai_conversations_user_id", columnList = "user_id")
})
public class AiConversation extends BaseEntity {

    @Column(name = "user_id")
    private UUID userId;

    @Column(length = 100)
    private String title;

    @Column(name = "last_message_at")
    private Instant lastMessageAt;

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Instant getLastMessageAt() { return lastMessageAt; }
    public void setLastMessageAt(Instant lastMessageAt) { this.lastMessageAt = lastMessageAt; }
}
