package com.mooc.app.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public class SendMessageResponse extends BaseResponse {
    @JsonProperty("message_id")
    private final String messageId;
    @JsonProperty("sender_id")
    private final String senderId;
    private final String content;
    private final boolean read;
    @JsonProperty("created_at")
    private final Instant createdAt;

    public SendMessageResponse(String requestId, MessageItemResponse message) {
        super(requestId);
        this.messageId = message.getMessageId();
        this.senderId = message.getSenderId();
        this.content = message.getContent();
        this.read = message.isRead();
        this.createdAt = message.getCreatedAt();
    }

    public String getMessageId() { return messageId; }
    public String getSenderId() { return senderId; }
    public String getContent() { return content; }
    public boolean isRead() { return read; }
    public Instant getCreatedAt() { return createdAt; }
}
