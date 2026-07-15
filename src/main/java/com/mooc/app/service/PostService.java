package com.mooc.app.service;

import com.fasterxml.jackson.core.type.TypeReference;
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
import com.mooc.app.repository.PostRepository;
import com.mooc.app.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PostService {

    private static final Logger log = LoggerFactory.getLogger(PostService.class);
    private static final String CACHE_KEY_PREFIX = "cache:posts:";
    private static final Duration LIST_TTL = Duration.ofMinutes(2);
    private static final Duration DETAIL_TTL = Duration.ofMinutes(10);
    private static final String EVICT_PATTERN = CACHE_KEY_PREFIX + "*";

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final BookmarkRepository bookmarkRepository;
    private final GenericCacheService cacheService;

    public PostService(PostRepository postRepository, UserRepository userRepository,
                       BookmarkRepository bookmarkRepository, GenericCacheService cacheService) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.bookmarkRepository = bookmarkRepository;
        this.cacheService = cacheService;
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
        cacheService.evict(EVICT_PATTERN);
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
        cacheService.evict(EVICT_PATTERN);
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
        cacheService.evict(EVICT_PATTERN);
    }

    public PostResponse getPost(String idOrSlug, String requestId) {
        // Try cache first (only for slug lookups)
        boolean isSlug = !isUuid(idOrSlug);
        String cacheKey = CACHE_KEY_PREFIX + "detail:" + idOrSlug;
        if (isSlug) {
            PostResponse cached = cacheService.get(cacheKey, new TypeReference<>() {});
            if (cached != null) {
                log.debug("Post detail cache hit for key={}", cacheKey);
                return new PostResponse(requestId, cached.getId(), cached.getTitle(), cached.getSlug(),
                        cached.getContent(), cached.getCoverImage(), cached.getTags(), cached.getStatus(),
                        cached.getAuthorId(), cached.getAuthorUsername(), cached.getCreatedAt(), cached.getUpdatedAt(),
                        cached.getCommentCount(), cached.getUpVoteCount(), cached.getBookmarkCount());
            }
        }

        PostEntity post = findPostByIdOrSlug(idOrSlug);

        if (post.getStatus() != PostStatus.PUBLISHED) {
            throw new PostException(HttpStatus.NOT_FOUND, "not_found", "Post not found");
        }

        Map<UUID, long[]> stats = batchFetchStats(List.of(post));
        long[] s = stats.getOrDefault(post.getId(), new long[]{0, 0, 0});
        PostResponse response = toPostResponse(post, requestId, true, s[0], s[1], s[2]);

        if (isSlug) {
            cacheService.put(cacheKey, response, DETAIL_TTL);
        }
        return response;
    }

    private static boolean isUuid(String s) {
        try { UUID.fromString(s); return true; } catch (IllegalArgumentException e) { return false; }
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

        // Cursor mode: no caching (cursor values are too granular)
        if (cursor != null && !cursor.isBlank()) {
            return listPostsUncached(page, safeSize, cursor, sort, requestId);
        }

        // Offset mode: try cache
        String cacheKey = CACHE_KEY_PREFIX + "list:" + sort.name().toLowerCase() + ":" + page + ":" + safeSize;
        TypeReference<PostListCacheEntry> typeRef = new TypeReference<>() {};
        PostListCacheEntry cached = cacheService.get(cacheKey, typeRef);
        if (cached != null) {
            log.debug("Post list cache hit for key={}", cacheKey);
            List<PostResponse> items = cached.items().stream()
                    .map(r -> new PostResponse(requestId, r.getId(), r.getTitle(), r.getSlug(), null,
                            r.getCoverImage(), r.getTags(), r.getStatus(), r.getAuthorId(), r.getAuthorUsername(),
                            r.getCreatedAt(), r.getUpdatedAt(), r.getCommentCount(), r.getUpVoteCount(), r.getBookmarkCount()))
                    .toList();
            return new PostListResponse(requestId, items, cached.total(), page, safeSize, cached.nextCursor(), cached.hasMore());
        }

        PostListResponse response = listPostsUncached(page, safeSize, null, sort, requestId);
        cacheService.put(cacheKey, new PostListCacheEntry(response.getItems(), response.getTotal(),
                response.getNextCursor(), response.isHasMore()), LIST_TTL);
        return response;
    }

    private PostListResponse listPostsUncached(int page, int safeSize, String cursor, PostSortBy sort, String requestId) {
        // Cursor mode
        if (cursor != null && !cursor.isBlank()) {
            PostSortBy.CursorValue cv = sort.decodeCursor(cursor);
            Pageable pageable = PageRequest.of(0, safeSize + 1);
            List<PostEntity> entities = findPostsCursor(sort, cv, pageable);
            boolean hasMore = entities.size() > safeSize;
            List<PostEntity> trimmed = hasMore ? entities.subList(0, safeSize) : entities;
            Map<UUID, long[]> stats = batchFetchStats(trimmed);
            List<PostResponse> items = buildItemsWithStats(trimmed, stats, requestId, false);
            String nextCursor = trimmed.isEmpty() ? null : buildCursor(trimmed.get(trimmed.size() - 1), sort, stats);
            long total = postRepository.findByStatusAndDeletedFalse(PostStatus.PUBLISHED, PageRequest.of(0, 1)).getTotalElements();
            return new PostListResponse(requestId, items, total, null, safeSize, nextCursor, hasMore);
        }

        // Offset mode
        int safePage = Math.max(page - 1, 0);
        Pageable pageable = PageRequest.of(safePage, safeSize);
        Page<PostEntity> result = findPostsBySort(sort, pageable);
        List<PostEntity> posts = result.getContent();
        Map<UUID, long[]> stats = batchFetchStats(posts);
        List<PostResponse> items = buildItemsWithStats(posts, stats, requestId, false);
        boolean hasMore = (long) (safePage + 1) * safeSize < result.getTotalElements();
        String nextCursor = posts.isEmpty() ? null : buildCursor(posts.get(posts.size() - 1), sort, stats);
        return new PostListResponse(requestId, items, result.getTotalElements(), page, safeSize, nextCursor, hasMore);
    }

    record PostListCacheEntry(List<PostResponse> items, long total, String nextCursor, boolean hasMore) {}

    public PostListResponse listUserPosts(UUID authorId, int page, int size, String cursor, PostSortBy sort, String requestId) {
        int safeSize = Math.min(size, 100);

        // Cursor mode: all sorts supported
        if (cursor != null && !cursor.isBlank()) {
            PostSortBy.CursorValue cv = sort.decodeCursor(cursor);
            Pageable pageable = PageRequest.of(0, safeSize + 1);
            List<PostEntity> entities = findUserPostsCursor(authorId, sort, cv, pageable);
            boolean hasMore = entities.size() > safeSize;
            List<PostEntity> trimmed = hasMore ? entities.subList(0, safeSize) : entities;
            Map<UUID, long[]> stats = batchFetchStats(trimmed);
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
        Map<UUID, long[]> stats = batchFetchStats(posts);
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
        Map<UUID, long[]> stats = batchFetchStats(posts);
        return buildItemsWithStats(posts, stats, requestId, includeContent);
    }

    private List<PostResponse> buildItemsWithStats(List<PostEntity> posts, Map<UUID, long[]> stats, String requestId, boolean includeContent) {
        // Batch-fetch author usernames
        List<UUID> authorIds = posts.stream().map(PostEntity::getAuthorId).distinct().toList();
        Map<UUID, String> usernameMap = new HashMap<>();
        if (!authorIds.isEmpty()) {
            userRepository.findAllById(authorIds).forEach(u -> usernameMap.put(u.getId(), u.getUsername()));
        }
        return posts.stream().map(p -> {
            long[] s = stats.getOrDefault(p.getId(), new long[]{0, 0, 0});
            String username = usernameMap.getOrDefault(p.getAuthorId(), null);
            return toPostResponse(p, requestId, includeContent, s[0], s[1], s[2], username);
        }).toList();
    }

    private Map<UUID, long[]> batchFetchStats(List<PostEntity> posts) {
        if (posts.isEmpty()) {
            return Map.of();
        }
        Map<UUID, long[]> result = new HashMap<>();
        List<UUID> postIds = posts.stream().map(PostEntity::getId).toList();
        for (PostEntity p : posts) {
            result.put(p.getId(), new long[]{p.getCachedCommentCount(), p.getCachedUpVoteCount(), 0});
        }

        // Only bookmark count still needs a batch query
        for (Object[] row : bookmarkRepository.batchCountBookmarks(postIds, EntityType.POST)) {
            UUID postId = (UUID) row[0];
            long count = (Long) row[1];
            result.get(postId)[2] = count;
        }
        return result;
    }

    private PostResponse toPostResponse(PostEntity post, String requestId, boolean includeContent,
                                        long commentCount, long upVoteCount, long bookmarkCount) {
        String username = userRepository.findById(post.getAuthorId())
                .map(u -> u.getUsername()).orElse(null);
        return toPostResponse(post, requestId, includeContent, commentCount, upVoteCount, bookmarkCount, username);
    }

    private PostResponse toPostResponse(PostEntity post, String requestId, boolean includeContent,
                                        long commentCount, long upVoteCount, long bookmarkCount, String authorUsername) {
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
                authorUsername,
                post.getCreatedAt().toString(),
                post.getUpdatedAt().toString(),
                commentCount,
                upVoteCount,
                bookmarkCount
        );
    }
}
