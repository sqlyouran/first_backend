package com.mooc.app.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ConversationDetailResponse extends BaseResponse {
    @JsonProperty("conversation_id")
    private final String conversationId;
    @JsonProperty("other_user")
    private final ConversationItemResponse.OtherUserInfo otherUser;

    public ConversationDetailResponse(String requestId, String conversationId,
                                       ConversationItemResponse.OtherUserInfo otherUser) {
        super(requestId);
        this.conversationId = conversationId;
        this.otherUser = otherUser;
    }

    public String getConversationId() { return conversationId; }
    public ConversationItemResponse.OtherUserInfo getOtherUser() { return otherUser; }
}
