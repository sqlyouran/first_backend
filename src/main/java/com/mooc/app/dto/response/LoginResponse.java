package com.mooc.app.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LoginResponse extends BaseResponse {

    @JsonProperty("access_token")
    private final String accessToken;

    @JsonProperty("expires_in")
    private final long expiresIn;

    public LoginResponse(String requestId, String accessToken, long expiresIn) {
        super(requestId);
        this.accessToken = accessToken;
        this.expiresIn = expiresIn;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public long getExpiresIn() {
        return expiresIn;
    }
}
