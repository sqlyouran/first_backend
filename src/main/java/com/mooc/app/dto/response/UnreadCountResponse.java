package com.mooc.app.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UnreadCountResponse extends BaseResponse {

    @JsonProperty("unread_count")
    private final long count;

    public UnreadCountResponse(String requestId, long count) {
        super(requestId);
        this.count = count;
    }

    public long getCount() { return count; }
}
