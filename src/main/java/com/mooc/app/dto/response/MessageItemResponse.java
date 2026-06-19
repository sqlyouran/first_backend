package com.mooc.app.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public class MessageItemResponse {
    @JsonProperty("message_id")
    private final String messageId;
    @JsonProperty("sender_id")
    private final String senderId;
    private final String content;
    private final boolean read;
    @JsonProperty("created_at")
    private final Instant createdAt;

    public MessageItemResponse(String messageId, String senderId, String content,
                                boolean read, Instant createdAt) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.content = content;
        this.read = read;
        this.createdAt = createdAt;
    }
    public String getMessageId() { return messageId; }
    public String getSenderId() { return senderId; }
    public String getContent() { return content; }
    public boolean isRead() { return read; }
    public Instant getCreatedAt() { return createdAt; }
}
