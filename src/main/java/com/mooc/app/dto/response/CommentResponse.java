package com.mooc.app.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CommentResponse extends BaseResponse {

    @JsonProperty("id")
    private final String id;

    @JsonProperty("entity_id")
    private final String entityId;

    @JsonProperty("entity_type")
    private final String entityType;

    @JsonProperty("user_id")
    private final String userId;

    @JsonProperty("content")
    private final String content;

    @JsonProperty("parent_comment_id")
    private final String parentCommentId;

    @JsonProperty("created_at")
    private final String createdAt;

    @JsonProperty("deleted")
    private final boolean deleted;

    public CommentResponse(String requestId, String id, String entityId, String entityType, String userId,
                           String content, String parentCommentId, String createdAt, boolean deleted) {
        super(requestId);
        this.id = id;
        this.entityId = entityId;
        this.entityType = entityType;
        this.userId = userId;
        this.content = content;
        this.parentCommentId = parentCommentId;
        this.createdAt = createdAt;
        this.deleted = deleted;
    }

    public String getId() { return id; }
    public String getEntityId() { return entityId; }
    public String getEntityType() { return entityType; }
    public String getUserId() { return userId; }
    public String getContent() { return content; }
    public String getParentCommentId() { return parentCommentId; }
    public String getCreatedAt() { return createdAt; }
    public boolean isDeleted() { return deleted; }
}
