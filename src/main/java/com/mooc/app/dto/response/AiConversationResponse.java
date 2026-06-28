package com.mooc.app.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AiConversationResponse extends BaseResponse {

    @JsonProperty("id")
    private final String id;

    @JsonProperty("created_at")
    private final String createdAt;

    public AiConversationResponse(String requestId, String id, String createdAt) {
        super(requestId);
        this.id = id;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public String getCreatedAt() { return createdAt; }
}
