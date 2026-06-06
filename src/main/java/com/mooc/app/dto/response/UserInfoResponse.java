package com.mooc.app.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UserInfoResponse extends BaseResponse {

    private final String id;
    private final String email;
    private final String state;

    @JsonProperty("created_at")
    private final String createdAt;

    public UserInfoResponse(String requestId, String id, String email, String state, String createdAt) {
        super(requestId);
        this.id = id;
        this.email = email;
        this.state = state;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getState() {
        return state;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}
