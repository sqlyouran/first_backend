package com.mooc.app.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

@Entity
@Table(name = "comments")
public class CommentEntity extends BaseEntity {

    @NotNull(message = "Post ID must not be null")
    @Column(name = "post_id", nullable = false)
    private UUID postId;

    @NotNull(message = "User ID must not be null")
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @NotBlank(message = "Content must not be blank")
    @Size(max = 10000, message = "Content must not exceed 10000 characters")
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "parent_comment_id")
    private UUID parentCommentId;

    public UUID getPostId() { return postId; }
    public void setPostId(UUID postId) { this.postId = postId; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public UUID getParentCommentId() { return parentCommentId; }
    public void setParentCommentId(UUID parentCommentId) { this.parentCommentId = parentCommentId; }
}
