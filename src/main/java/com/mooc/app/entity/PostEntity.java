package com.mooc.app.entity;

import com.mooc.app.converter.StringListConverter;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "posts")
public class PostEntity extends BaseEntity {

    @NotBlank(message = "Title must not be blank")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    @Column(nullable = false)
    private String title;

    @NotBlank(message = "Slug must not be blank")
    @Size(max = 220, message = "Slug must not exceed 220 characters")
    @Column(nullable = false, unique = true)
    private String slug;

    @NotBlank(message = "Content must not be blank")
    @Size(max = 50000, message = "Content must not exceed 50000 characters")
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Size(max = 2048, message = "Cover image URL must not exceed 2048 characters")
    @Column(name = "cover_image")
    private String coverImage;

    @Convert(converter = StringListConverter.class)
    @Column(columnDefinition = "TEXT")
    private List<String> tags = new ArrayList<>();

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PostStatus status = PostStatus.PUBLISHED;

    @NotNull(message = "Author ID must not be null")
    @Column(name = "author_id", nullable = false)
    private UUID authorId;

    @Column(name = "cached_up_vote_count", nullable = false)
    private int cachedUpVoteCount = 0;

    @Column(name = "cached_comment_count", nullable = false)
    private int cachedCommentCount = 0;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getCoverImage() { return coverImage; }
    public void setCoverImage(String coverImage) { this.coverImage = coverImage; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags != null ? tags : new ArrayList<>(); }

    public PostStatus getStatus() { return status; }
    public void setStatus(PostStatus status) { this.status = status; }

    public UUID getAuthorId() { return authorId; }
    public void setAuthorId(UUID authorId) { this.authorId = authorId; }

    public int getCachedUpVoteCount() { return cachedUpVoteCount; }
    public void setCachedUpVoteCount(int cachedUpVoteCount) { this.cachedUpVoteCount = cachedUpVoteCount; }

    public int getCachedCommentCount() { return cachedCommentCount; }
    public void setCachedCommentCount(int cachedCommentCount) { this.cachedCommentCount = cachedCommentCount; }
}
