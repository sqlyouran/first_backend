package com.mooc.app.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Entity
@Table(name = "spots_posts",
        uniqueConstraints = @UniqueConstraint(columnNames = {"spot_id", "post_id"}))
public class SpotPostEntity extends BaseEntity {

    @NotNull(message = "Spot ID must not be null")
    @Column(name = "spot_id", nullable = false)
    private UUID spotId;

    @NotNull(message = "Post ID must not be null")
    @Column(name = "post_id", nullable = false)
    private UUID postId;

    public UUID getSpotId() { return spotId; }
    public void setSpotId(UUID spotId) { this.spotId = spotId; }

    public UUID getPostId() { return postId; }
    public void setPostId(UUID postId) { this.postId = postId; }
}
