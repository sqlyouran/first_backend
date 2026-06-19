package com.mooc.app.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "messages", indexes = {
    @Index(name = "idx_messages_conversation_created", columnList = "conversation_id, created_at DESC")
})
public class MessageEntity extends BaseEntity {

    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;

    @Column(name = "sender_id", nullable = false)
    private UUID senderId;

    @Column(nullable = false, length = 2000)
    private String content;

    // `read` is a MySQL reserved word, use `is_read`
    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    public UUID getConversationId() { return conversationId; }
    public void setConversationId(UUID conversationId) { this.conversationId = conversationId; }

    public UUID getSenderId() { return senderId; }
    public void setSenderId(UUID senderId) { this.senderId = senderId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public boolean isRead() { return read; }
    public void markRead() { this.read = true; }
}
