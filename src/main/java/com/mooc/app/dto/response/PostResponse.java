package com.mooc.app.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class PostResponse extends BaseResponse {

    private final String id;
    private final String title;

    @JsonProperty("cover_image")
    private final String coverImage;

    private final List<String> tags;
    private final String status;

    @JsonProperty("author_id")
    private final String authorId;

    @JsonProperty("created_at")
    private final String createdAt;

    @JsonProperty("updated_at")
    private final String updatedAt;

    private final String content;

    public PostResponse(String requestId, String id, String title, String content, String coverImage,
                        List<String> tags, String status, String authorId, String createdAt, String updatedAt) {
        super(requestId);
        this.id = id;
        this.title = title;
        this.content = content;
        this.coverImage = coverImage;
        this.tags = tags;
        this.status = status;
        this.authorId = authorId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getCoverImage() { return coverImage; }
    public List<String> getTags() { return tags; }
    public String getStatus() { return status; }
    public String getAuthorId() { return authorId; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }
}
