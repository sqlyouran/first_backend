package com.mooc.app.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MarkAllReadResponse extends BaseResponse {

    @JsonProperty("updated_count")
    private final int updatedCount;

    public MarkAllReadResponse(String requestId, int updatedCount) {
        super(requestId);
        this.updatedCount = updatedCount;
    }

    public int getUpdatedCount() { return updatedCount; }
}
