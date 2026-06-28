package com.mooc.app.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "ai_messages", indexes = {
    @Index(name = "idx_ai_messages_conv_created", columnList = "conversation_id, created_at")
})
public class AiMessage extends BaseEntity {

    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AiMessageRole role;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    public UUID getConversationId() { return conversationId; }
    public void setConversationId(UUID conversationId) { this.conversationId = conversationId; }

    public AiMessageRole getRole() { return role; }
    public void setRole(AiMessageRole role) { this.role = role; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
