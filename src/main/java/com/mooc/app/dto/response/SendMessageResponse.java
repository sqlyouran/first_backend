package com.mooc.app.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SendMessageResponse extends BaseResponse {
    @JsonProperty("message_id")
    private final String messageId;

    public SendMessageResponse(String requestId, String messageId) {
        super(requestId);
        this.messageId = messageId;
    }
    public String getMessageId() { return messageId; }
}
