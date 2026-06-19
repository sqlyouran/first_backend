package com.mooc.app.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UnreadCountResponse extends BaseResponse {
    @JsonProperty("unread_count")
    private final long unreadCount;

    public UnreadCountResponse(String requestId, long unreadCount) {
        super(requestId);
        this.unreadCount = unreadCount;
    }
    public long getUnreadCount() { return unreadCount; }
}
