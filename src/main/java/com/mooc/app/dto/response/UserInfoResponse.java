package com.mooc.app.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UserInfoResponse extends BaseResponse {

    private final String id;
    private final String email;
    private final String state;

    @JsonProperty("created_at")
    private final String createdAt;

    private final String username;
    private final String nickname;

    @JsonProperty("avatar_url")
    private final String avatarUrl;

    public UserInfoResponse(String requestId, String id, String email, String state, String createdAt,
                            String username, String nickname, String avatarUrl) {
        super(requestId);
        this.id = id;
        this.email = email;
        this.state = state;
        this.createdAt = createdAt;
        this.username = username;
        this.nickname = nickname;
        this.avatarUrl = avatarUrl;
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

    public String getUsername() {
        return username;
    }

    public String getNickname() {
        return nickname;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }
}
