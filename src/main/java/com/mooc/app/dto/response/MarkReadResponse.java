package com.mooc.app.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MarkReadResponse extends BaseResponse {
    @JsonProperty("marked_count")
    private final int markedCount;

    public MarkReadResponse(String requestId, int markedCount) {
        super(requestId);
        this.markedCount = markedCount;
    }
    public int getMarkedCount() { return markedCount; }
}
