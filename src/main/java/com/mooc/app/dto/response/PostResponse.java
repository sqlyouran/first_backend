package com.mooc.app.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class PostResponse extends BaseResponse {

    private final String id;
    private final String title;
    private final String slug;

    @JsonProperty("cover_image")
    private final String coverImage;

    private final List<String> tags;
    private final String status;

    @JsonProperty("author_id")
    private final String authorId;

    @JsonProperty("author_username")
    private final String authorUsername;

    @JsonProperty("created_at")
    private final String createdAt;

    @JsonProperty("updated_at")
    private final String updatedAt;

    private final String content;

    @JsonProperty("comment_count")
    private final long commentCount;

    @JsonProperty("up_vote_count")
    private final long upVoteCount;

    @JsonProperty("bookmark_count")
    private final long bookmarkCount;

    public PostResponse(String requestId, String id, String title, String slug, String content, String coverImage,
                        List<String> tags, String status, String authorId, String authorUsername,
                        String createdAt, String updatedAt,
                        long commentCount, long upVoteCount, long bookmarkCount) {
        super(requestId);
        this.id = id;
        this.title = title;
        this.slug = slug;
        this.content = content;
        this.coverImage = coverImage;
        this.tags = tags;
        this.status = status;
        this.authorId = authorId;
        this.authorUsername = authorUsername;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.commentCount = commentCount;
        this.upVoteCount = upVoteCount;
        this.bookmarkCount = bookmarkCount;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getSlug() { return slug; }
    public String getContent() { return content; }
    public String getCoverImage() { return coverImage; }
    public List<String> getTags() { return tags; }
    public String getStatus() { return status; }
    public String getAuthorId() { return authorId; }
    public String getAuthorUsername() { return authorUsername; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public long getCommentCount() { return commentCount; }
    public long getUpVoteCount() { return upVoteCount; }
    public long getBookmarkCount() { return bookmarkCount; }
}
