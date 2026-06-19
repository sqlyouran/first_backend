package com.mooc.app.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CreateConversationResponse extends BaseResponse {
    @JsonProperty("conversation_id")
    private final String conversationId;
    @JsonProperty("message_id")
    private final String messageId;

    public CreateConversationResponse(String requestId, String conversationId, String messageId) {
        super(requestId);
        this.conversationId = conversationId;
        this.messageId = messageId;
    }
    public String getConversationId() { return conversationId; }
    public String getMessageId() { return messageId; }
}
