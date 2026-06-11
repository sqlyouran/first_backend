package com.mooc.app.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Entity
@Table(name = "bookmarks", uniqueConstraints = @UniqueConstraint(columnNames = {"post_id", "user_id"}))
public class BookmarkEntity extends BaseEntity {

    @NotNull(message = "Post ID must not be null")
    @Column(name = "post_id", nullable = false)
    private UUID postId;

    @NotNull(message = "User ID must not be null")
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    public UUID getPostId() { return postId; }
    public void setPostId(UUID postId) { this.postId = postId; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
}
