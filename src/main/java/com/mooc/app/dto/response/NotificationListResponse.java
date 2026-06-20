package com.mooc.app.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class NotificationListResponse extends BaseResponse {

    private final List<NotificationItemResponse> items;
    private final long total;
    private final int page;
    private final int size;

    public NotificationListResponse(String requestId, List<NotificationItemResponse> items, long total, int page, int size) {
        super(requestId);
        this.items = items;
        this.total = total;
        this.page = page;
        this.size = size;
    }

    public List<NotificationItemResponse> getItems() { return items; }
    public long getTotal() { return total; }
    public int getPage() { return page; }
    public int getSize() { return size; }

    public static class NotificationItemResponse {

        @JsonProperty("id")
        private final String id;

        @JsonProperty("type")
        private final String type;

        @JsonProperty("actor_id")
        private final String actorId;

        @JsonProperty("actor_nickname")
        private final String actorNickname;

        @JsonProperty("actor_avatar_url")
        private final String actorAvatarUrl;

        @JsonProperty("actor_username")
        private final String actorUsername;

        @JsonProperty("entity_id")
        private final String entityId;

        @JsonProperty("entity_type")
        private final String entityType;

        @JsonProperty("target_title")
        private final String targetTitle;

        @JsonProperty("content_preview")
        private final String contentPreview;

        @JsonProperty("read")
        private final boolean read;

        @JsonProperty("created_at")
        private final String createdAt;

        public NotificationItemResponse(String id, String type,
                                         String actorId, String actorNickname,
                                         String actorAvatarUrl, String actorUsername,
                                         String entityId, String entityType,
                                         String targetTitle, String contentPreview,
                                         boolean read, String createdAt) {
            this.id = id;
            this.type = type;
            this.actorId = actorId;
            this.actorNickname = actorNickname;
            this.actorAvatarUrl = actorAvatarUrl;
            this.actorUsername = actorUsername;
            this.entityId = entityId;
            this.entityType = entityType;
            this.targetTitle = targetTitle;
            this.contentPreview = contentPreview;
            this.read = read;
            this.createdAt = createdAt;
        }

        public String getId() { return id; }
        public String getType() { return type; }
        public String getActorId() { return actorId; }
        public String getActorNickname() { return actorNickname; }
        public String getActorAvatarUrl() { return actorAvatarUrl; }
        public String getActorUsername() { return actorUsername; }
        public String getEntityId() { return entityId; }
        public String getEntityType() { return entityType; }
        public String getTargetTitle() { return targetTitle; }
        public String getContentPreview() { return contentPreview; }
        public boolean isRead() { return read; }
        public String getCreatedAt() { return createdAt; }
    }
}
