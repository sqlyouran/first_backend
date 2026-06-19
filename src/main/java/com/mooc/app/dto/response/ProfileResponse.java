package com.mooc.app.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class ProfileResponse extends BaseResponse {

    private final String id;
    private final String email;
    private final String username;
    private final String nickname;

    @JsonProperty("avatar_url")
    private final String avatarUrl;

    private final String bio;

    @JsonProperty("interest_tags")
    private final List<String> interestTags;

    @JsonProperty("created_at")
    private final String createdAt;

    public ProfileResponse(String requestId, String id, String email, String username, String nickname,
                           String avatarUrl, String bio, List<String> interestTags, String createdAt) {
        super(requestId);
        this.id = id;
        this.email = email;
        this.username = username;
        this.nickname = nickname;
        this.avatarUrl = avatarUrl;
        this.bio = bio;
        this.interestTags = interestTags;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public String getEmail() { return email; }
    public String getUsername() { return username; }
    public String getNickname() { return nickname; }
    public String getAvatarUrl() { return avatarUrl; }
    public String getBio() { return bio; }
    public List<String> getInterestTags() { return interestTags; }
    public String getCreatedAt() { return createdAt; }
}
