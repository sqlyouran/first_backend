package com.mooc.app.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public abstract class BaseResponse {

    @JsonProperty("request_id")
    private final String requestId;

    protected BaseResponse(String requestId) {
        this.requestId = requestId;
    }

    public String getRequestId() {
        return requestId;
    }
}
