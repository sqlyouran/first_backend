package com.mooc.app.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public class ConversationItemResponse {
    @JsonProperty("conversation_id")
    private final String conversationId;
    @JsonProperty("other_user")
    private final OtherUserInfo otherUser;
    @JsonProperty("last_message")
    private final String lastMessage;
    @JsonProperty("last_message_at")
    private final Instant lastMessageAt;
    @JsonProperty("unread_count")
    private final long unreadCount;

    public ConversationItemResponse(String conversationId, OtherUserInfo otherUser,
                                     String lastMessage, Instant lastMessageAt, long unreadCount) {
        this.conversationId = conversationId;
        this.otherUser = otherUser;
        this.lastMessage = lastMessage;
        this.lastMessageAt = lastMessageAt;
        this.unreadCount = unreadCount;
    }

    public String getConversationId() { return conversationId; }
    public OtherUserInfo getOtherUser() { return otherUser; }
    public String getLastMessage() { return lastMessage; }
    public Instant getLastMessageAt() { return lastMessageAt; }
    public long getUnreadCount() { return unreadCount; }

    public static class OtherUserInfo {
        @JsonProperty("user_id")
        private final String userId;
        private final String username;
        private final String nickname;
        @JsonProperty("avatar_url")
        private final String avatarUrl;
        private final boolean deleted;

        public OtherUserInfo(String userId, String username, String nickname, String avatarUrl, boolean deleted) {
            this.userId = userId;
            this.username = username;
            this.nickname = nickname;
            this.avatarUrl = avatarUrl;
            this.deleted = deleted;
        }
        public String getUserId() { return userId; }
        public String getUsername() { return username; }
        public String getNickname() { return nickname; }
        public String getAvatarUrl() { return avatarUrl; }
        public boolean isDeleted() { return deleted; }
    }
}
