package com.mooc.app.service;

import com.mooc.app.dto.CreatePostRequest;
import com.mooc.app.dto.UpdatePostRequest;
import com.mooc.app.dto.response.PostListResponse;
import com.mooc.app.dto.response.PostResponse;
import com.mooc.app.entity.EntityType;
import com.mooc.app.entity.PostEntity;
import com.mooc.app.entity.PostSortBy;
import com.mooc.app.entity.PostStatus;
import com.mooc.app.exception.PostException;
import com.mooc.app.repository.BookmarkRepository;
import com.mooc.app.repository.CommentRepository;
import com.mooc.app.repository.PostRepository;
import com.mooc.app.repository.UserRepository;
import com.mooc.app.repository.VoteRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final VoteRepository voteRepository;
    private final BookmarkRepository bookmarkRepository;
    private final CommentRepository commentRepository;

    public PostService(PostRepository postRepository, UserRepository userRepository,
                       VoteRepository voteRepository, BookmarkRepository bookmarkRepository,
                       CommentRepository commentRepository) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.voteRepository = voteRepository;
        this.bookmarkRepository = bookmarkRepository;
        this.commentRepository = commentRepository;
    }

    public PostResponse createPost(CreatePostRequest request, UUID authorId, String requestId) {
        if (!userRepository.existsById(authorId)) {
            throw new PostException(HttpStatus.NOT_FOUND, "not_found", "User not found");
        }

        PostEntity post = new PostEntity();
        post.setTitle(request.title());
        post.setContent(request.content());
        post.setCoverImage(request.coverImage());
        post.setTags(request.tags() != null ? new ArrayList<>(request.tags()) : new ArrayList<>());
        post.setAuthorId(authorId);

        if (request.status() != null && !request.status().isBlank()) {
            try {
                post.setStatus(PostStatus.valueOf(request.status().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new PostException(HttpStatus.UNPROCESSABLE_ENTITY, "validation_error",
                        "Invalid status: " + request.status());
            }
        }

        // Set preliminary slug before first save to satisfy validation
        String preliminarySlug = post.getTitle().toLowerCase()
                .replaceAll("[^a-z0-9\\u4e00-\\u9fa5]+", "-")
                .replaceAll("^-|-$", "");
        if (preliminarySlug.length() > 50) {
            preliminarySlug = preliminarySlug.substring(0, 50).replaceAll("-$", "");
        }
        post.setSlug(preliminarySlug + "-temp");

        PostEntity saved = postRepository.save(post);
        // Generate final slug with ID suffix for uniqueness
        saved.setSlug(generateSlug(saved.getTitle(), saved.getId()));
        saved = postRepository.save(saved);
        return toPostResponse(saved, requestId, true, 0, 0, 0);
    }

    public PostResponse updatePost(UUID postId, UpdatePostRequest request, UUID authorId, String requestId) {
        PostEntity post = postRepository.findByIdAndDeletedFalse(postId)
                .orElseThrow(() -> new PostException(HttpStatus.NOT_FOUND, "not_found", "Post not found"));

        if (!post.getAuthorId().equals(authorId)) {
            throw new PostException(HttpStatus.FORBIDDEN, "access_denied", "You are not the author of this post");
        }

        if (request.title() != null) {
            post.setTitle(request.title());
        }
        if (request.content() != null) {
            post.setContent(request.content());
        }
        if (request.coverImage() != null) {
            post.setCoverImage(request.coverImage());
        }
        if (request.tags() != null) {
            post.setTags(new ArrayList<>(request.tags()));
        }
        if (request.status() != null && !request.status().isBlank()) {
            try {
                post.setStatus(PostStatus.valueOf(request.status().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new PostException(HttpStatus.UNPROCESSABLE_ENTITY, "validation_error",
                        "Invalid status: " + request.status());
            }
        }

        PostEntity saved = postRepository.save(post);
        return toPostResponse(saved, requestId, true, 0, 0, 0);
    }

    public void deletePost(UUID postId, UUID authorId) {
        PostEntity post = postRepository.findByIdAndDeletedFalse(postId)
                .orElseThrow(() -> new PostException(HttpStatus.NOT_FOUND, "not_found", "Post not found"));

        if (!post.getAuthorId().equals(authorId)) {
            throw new PostException(HttpStatus.FORBIDDEN, "access_denied", "You are not the author of this post");
        }

        post.markDeleted();
        postRepository.save(post);
    }

    public PostResponse getPost(String idOrSlug, String requestId) {
        PostEntity post = findPostByIdOrSlug(idOrSlug);

        if (post.getStatus() != PostStatus.PUBLISHED) {
            throw new PostException(HttpStatus.NOT_FOUND, "not_found", "Post not found");
        }

        Map<UUID, long[]> stats = batchFetchStats(List.of(post.getId()));
        long[] s = stats.getOrDefault(post.getId(), new long[]{0, 0, 0});
        return toPostResponse(post, requestId, true, s[0], s[1], s[2]);
    }

    private PostEntity findPostByIdOrSlug(String idOrSlug) {
        // Try UUID first
        try {
            UUID id = UUID.fromString(idOrSlug);
            return postRepository.findByIdAndDeletedFalse(id)
                    .orElseThrow(() -> new PostException(HttpStatus.NOT_FOUND, "not_found", "Post not found"));
        } catch (IllegalArgumentException e) {
            // Not a valid UUID, try slug
            return postRepository.findBySlugAndDeletedFalse(idOrSlug)
                    .orElseThrow(() -> new PostException(HttpStatus.NOT_FOUND, "not_found", "Post not found"));
        }
    }

    private String generateSlug(String title, UUID id) {
        // Convert title to lowercase and replace spaces/special chars with hyphens
        String slug = title.toLowerCase()
                .replaceAll("[^a-z0-9\\u4e00-\\u9fa5]+", "-")
                .replaceAll("^-|-$", "");
        // Truncate to 50 chars and append first 8 chars of ID for uniqueness
        if (slug.length() > 50) {
            slug = slug.substring(0, 50).replaceAll("-$", "");
        }
        String idSuffix = id.toString().substring(0, 8);
        return slug + "-" + idSuffix;
    }

    public PostListResponse listPosts(int page, int size, String cursor, PostSortBy sort, String requestId) {
        int safeSize = Math.min(size, 100);

        // Cursor mode: all sorts supported
        if (cursor != null && !cursor.isBlank()) {
            PostSortBy.CursorValue cv = sort.decodeCursor(cursor);
            Pageable pageable = PageRequest.of(0, safeSize + 1);
            List<PostEntity> entities = findPostsCursor(sort, cv, pageable);
            boolean hasMore = entities.size() > safeSize;
            List<PostEntity> trimmed = hasMore ? entities.subList(0, safeSize) : entities;
            Map<UUID, long[]> stats = batchFetchStats(trimmed.stream().map(PostEntity::getId).toList());
            List<PostResponse> items = buildItemsWithStats(trimmed, stats, requestId, false);
            String nextCursor = trimmed.isEmpty() ? null : buildCursor(trimmed.get(trimmed.size() - 1), sort, stats);
            long total = postRepository.findByStatusAndDeletedFalse(PostStatus.PUBLISHED, PageRequest.of(0, 1)).getTotalElements();
            return new PostListResponse(requestId, items, total, null, safeSize, nextCursor, hasMore);
        }

        // Offset mode (first load)
        int safePage = Math.max(page - 1, 0);
        Pageable pageable = PageRequest.of(safePage, safeSize);
        Page<PostEntity> result = findPostsBySort(sort, pageable);
        List<PostEntity> posts = result.getContent();
        Map<UUID, long[]> stats = batchFetchStats(posts.stream().map(PostEntity::getId).toList());
        List<PostResponse> items = buildItemsWithStats(posts, stats, requestId, false);
        boolean hasMore = (long) (safePage + 1) * safeSize < result.getTotalElements();
        String nextCursor = posts.isEmpty() ? null : buildCursor(posts.get(posts.size() - 1), sort, stats);
        return new PostListResponse(requestId, items, result.getTotalElements(), page, safeSize, nextCursor, hasMore);
    }

    public PostListResponse listUserPosts(UUID authorId, int page, int size, String cursor, PostSortBy sort, String requestId) {
        int safeSize = Math.min(size, 100);

        // Cursor mode: all sorts supported
        if (cursor != null && !cursor.isBlank()) {
            PostSortBy.CursorValue cv = sort.decodeCursor(cursor);
            Pageable pageable = PageRequest.of(0, safeSize + 1);
            List<PostEntity> entities = findUserPostsCursor(authorId, sort, cv, pageable);
            boolean hasMore = entities.size() > safeSize;
            List<PostEntity> trimmed = hasMore ? entities.subList(0, safeSize) : entities;
            Map<UUID, long[]> stats = batchFetchStats(trimmed.stream().map(PostEntity::getId).toList());
            List<PostResponse> items = buildItemsWithStats(trimmed, stats, requestId, false);
            String nextCursor = trimmed.isEmpty() ? null : buildCursor(trimmed.get(trimmed.size() - 1), sort, stats);
            long total = postRepository.findByAuthorIdAndStatusAndDeletedFalse(authorId, PostStatus.PUBLISHED, PageRequest.of(0, 1)).getTotalElements();
            return new PostListResponse(requestId, items, total, null, safeSize, nextCursor, hasMore);
        }

        // Offset mode (first load)
        int safePage = Math.max(page - 1, 0);
        Pageable pageable = PageRequest.of(safePage, safeSize);
        Page<PostEntity> result = findUserPostsBySort(authorId, sort, pageable);
        List<PostEntity> posts = result.getContent();
        Map<UUID, long[]> stats = batchFetchStats(posts.stream().map(PostEntity::getId).toList());
        List<PostResponse> items = buildItemsWithStats(posts, stats, requestId, false);
        boolean hasMore = (long) (safePage + 1) * safeSize < result.getTotalElements();
        String nextCursor = posts.isEmpty() ? null : buildCursor(posts.get(posts.size() - 1), sort, stats);
        return new PostListResponse(requestId, items, result.getTotalElements(), page, safeSize, nextCursor, hasMore);
    }

    private List<PostEntity> findPostsCursor(PostSortBy sort, PostSortBy.CursorValue cv, Pageable pageable) {
        return switch (sort) {
            case LATEST -> postRepository.findByCreatedAtCursor(PostStatus.PUBLISHED, cv.timestamp(), pageable);
            case MOST_UPVOTED -> postRepository.findByUpVoteCountCursor(PostStatus.PUBLISHED, cv.count(), cv.timestamp(), pageable);
            case MOST_COMMENTED -> postRepository.findByCommentCountCursor(PostStatus.PUBLISHED, cv.count(), cv.timestamp(), pageable);
        };
    }

    private List<PostEntity> findUserPostsCursor(UUID authorId, PostSortBy sort, PostSortBy.CursorValue cv, Pageable pageable) {
        return switch (sort) {
            case LATEST -> postRepository.findByAuthorIdAndCreatedAtCursor(authorId, PostStatus.PUBLISHED, cv.timestamp(), pageable);
            case MOST_UPVOTED -> postRepository.findByAuthorIdAndUpVoteCountCursor(authorId, PostStatus.PUBLISHED, cv.count(), cv.timestamp(), pageable);
            case MOST_COMMENTED -> postRepository.findByAuthorIdAndCommentCountCursor(authorId, PostStatus.PUBLISHED, cv.count(), cv.timestamp(), pageable);
        };
    }

    private String buildCursor(PostEntity lastItem, PostSortBy sort, Map<UUID, long[]> stats) {
        long[] s = stats.getOrDefault(lastItem.getId(), new long[]{0, 0, 0});
        long count = switch (sort) {
            case LATEST -> 0;
            case MOST_UPVOTED -> s[1];
            case MOST_COMMENTED -> s[0];
        };
        return sort.encodeCursor(count, lastItem.getCreatedAt());
    }

    private Page<PostEntity> findPostsBySort(PostSortBy sort, Pageable pageable) {
        return switch (sort) {
            case MOST_UPVOTED -> postRepository.findByUpVoteCount(pageable, PostStatus.PUBLISHED);
            case MOST_COMMENTED -> postRepository.findByCommentCount(pageable, PostStatus.PUBLISHED);
            default -> postRepository.findByStatusAndDeletedFalse(PostStatus.PUBLISHED,
                    PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                            org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt")));
        };
    }

    private Page<PostEntity> findUserPostsBySort(UUID authorId, PostSortBy sort, Pageable pageable) {
        return switch (sort) {
            case MOST_UPVOTED -> postRepository.findByAuthorIdAndUpVoteCount(pageable, authorId, PostStatus.PUBLISHED);
            case MOST_COMMENTED -> postRepository.findByAuthorIdAndCommentCount(pageable, authorId, PostStatus.PUBLISHED);
            default -> postRepository.findByAuthorIdAndStatusAndDeletedFalse(authorId, PostStatus.PUBLISHED,
                    PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                            org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt")));
        };
    }

    private List<PostResponse> buildItems(List<PostEntity> posts, String requestId, boolean includeContent) {
        List<UUID> postIds = posts.stream().map(PostEntity::getId).toList();
        Map<UUID, long[]> stats = batchFetchStats(postIds);
        return buildItemsWithStats(posts, stats, requestId, includeContent);
    }

    private List<PostResponse> buildItemsWithStats(List<PostEntity> posts, Map<UUID, long[]> stats, String requestId, boolean includeContent) {
        return posts.stream().map(p -> {
            long[] s = stats.getOrDefault(p.getId(), new long[]{0, 0, 0});
            return toPostResponse(p, requestId, includeContent, s[0], s[1], s[2]);
        }).toList();
    }

    private Map<UUID, long[]> batchFetchStats(List<UUID> postIds) {
        if (postIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, long[]> result = new HashMap<>();
        for (UUID id : postIds) {
            result.put(id, new long[]{0, 0, 0});
        }

        for (Object[] row : voteRepository.batchCountUpVotes(postIds)) {
            UUID postId = (UUID) row[0];
            long count = (Long) row[1];
            result.get(postId)[1] = count;
        }
        for (Object[] row : bookmarkRepository.batchCountBookmarks(postIds, EntityType.POST)) {
            UUID postId = (UUID) row[0];
            long count = (Long) row[1];
            result.get(postId)[2] = count;
        }
        for (Object[] row : commentRepository.batchCountActiveComments(postIds, EntityType.POST)) {
            UUID postId = (UUID) row[0];
            long count = (Long) row[1];
            result.get(postId)[0] = count;
        }
        return result;
    }

    private PostResponse toPostResponse(PostEntity post, String requestId, boolean includeContent,
                                        long commentCount, long upVoteCount, long bookmarkCount) {
        return new PostResponse(
                requestId,
                post.getId().toString(),
                post.getTitle(),
                post.getSlug(),
                includeContent ? post.getContent() : null,
                post.getCoverImage(),
                post.getTags(),
                post.getStatus().name().toLowerCase(),
                post.getAuthorId().toString(),
                post.getCreatedAt().toString(),
                post.getUpdatedAt().toString(),
                commentCount,
                upVoteCount,
                bookmarkCount
        );
    }
}
