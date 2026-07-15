package com.mooc.app.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.mooc.app.dto.response.PostListResponse;
import com.mooc.app.dto.response.PostResponse;
import com.mooc.app.entity.PostEntity;
import com.mooc.app.entity.PostSortBy;
import com.mooc.app.entity.PostStatus;
import com.mooc.app.entity.UserEntity;
import com.mooc.app.repository.BookmarkRepository;
import com.mooc.app.repository.PostRepository;
import com.mooc.app.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.lang.reflect.Field;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceCacheTest {

    @Mock private PostRepository postRepository;
    @Mock private UserRepository userRepository;
    @Mock private BookmarkRepository bookmarkRepository;
    @Mock private GenericCacheService cacheService;

    private PostService postService;
    private final UUID authorId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        postService = new PostService(postRepository, userRepository, bookmarkRepository, cacheService);
    }

    @Test
    void listPosts_cacheHit_returnsReconstructedResponse() {
        PostResponse cachedItem = new PostResponse(null, "id1", "Title", "slug-1", null,
                null, List.of(), "published", authorId.toString(), "user1",
                "2026-01-01T00:00:00Z", "2026-01-01T00:00:00Z", 5, 10, 2);
        PostService.PostListCacheEntry entry = new PostService.PostListCacheEntry(
                List.of(cachedItem), 1L, null, false);

        when(cacheService.get(anyString(), any(TypeReference.class))).thenReturn(entry);

        PostListResponse result = postService.listPosts(1, 20, null, PostSortBy.LATEST, "req-1");

        assertNotNull(result);
        assertEquals(1, result.getItems().size());
        assertEquals("req-1", result.getItems().get(0).getRequestId());
        assertEquals("id1", result.getItems().get(0).getId());
        verifyNoInteractions(postRepository);
    }

    @Test
    void listPosts_cacheMiss_queriesDbAndCaches() {
        when(cacheService.get(anyString(), any(TypeReference.class))).thenReturn(null);

        PostEntity post = createPost("Test Post", PostStatus.PUBLISHED);
        Page<PostEntity> page = new PageImpl<>(List.of(post));
        when(postRepository.findByStatusAndDeletedFalse(eq(PostStatus.PUBLISHED), any(PageRequest.class)))
                .thenReturn(page);
        when(userRepository.findAllById(anyList())).thenReturn(List.of());
        when(bookmarkRepository.batchCountBookmarks(anyList(), any())).thenReturn(List.of());

        PostListResponse result = postService.listPosts(1, 20, null, PostSortBy.LATEST, "req-1");

        assertNotNull(result);
        verify(cacheService).put(anyString(), any(), eq(Duration.ofMinutes(2)));
    }

    @Test
    void getPost_bySlug_cacheHit_returnsCachedResponse() {
        String slug = "test-post-abc12345";
        PostResponse cached = new PostResponse(null, "id1", "Title", slug, "content",
                null, List.of(), "published", authorId.toString(), "user1",
                "2026-01-01T00:00:00Z", "2026-01-01T00:00:00Z", 5, 10, 2);

        when(cacheService.get(contains("detail:" + slug), any(TypeReference.class))).thenReturn(cached);

        PostResponse result = postService.getPost(slug, "req-1");

        assertNotNull(result);
        assertEquals("req-1", result.getRequestId());
        assertEquals(slug, result.getSlug());
        verifyNoInteractions(postRepository);
    }

    @Test
    void getPost_bySlug_cacheMiss_queriesDbAndCaches() {
        String slug = "test-post-abc12345";
        when(cacheService.get(anyString(), any(TypeReference.class))).thenReturn(null);

        PostEntity post = createPost("Test Post", PostStatus.PUBLISHED);
        post.setSlug(slug);
        when(postRepository.findBySlugAndDeletedFalse(slug)).thenReturn(Optional.of(post));
        when(userRepository.findById(any())).thenReturn(Optional.of(createUser()));
        when(bookmarkRepository.batchCountBookmarks(anyList(), any())).thenReturn(List.of());

        PostResponse result = postService.getPost(slug, "req-1");

        assertNotNull(result);
        verify(cacheService).put(contains("detail:" + slug), any(), eq(Duration.ofMinutes(10)));
    }

    @Test
    void createPost_evictsCache() {
        when(userRepository.existsById(any())).thenReturn(true);
        PostEntity saved = createPost("New Post", PostStatus.DRAFT);
        when(postRepository.save(any(PostEntity.class))).thenReturn(saved);

        postService.createPost(new com.mooc.app.dto.CreatePostRequest(
                "New Post", "content", null, null, null), authorId, "req-1");

        verify(cacheService).evict(contains("cache:posts:"));
    }

    @Test
    void deletePost_evictsCache() {
        PostEntity post = createPost("Test", PostStatus.PUBLISHED);
        when(postRepository.findByIdAndDeletedFalse(any())).thenReturn(Optional.of(post));

        postService.deletePost(post.getId(), authorId);

        verify(cacheService).evict(contains("cache:posts:"));
    }

    private PostEntity createPost(String title, PostStatus status) {
        PostEntity post = new PostEntity();
        post.setId(UUID.randomUUID());
        post.setTitle(title);
        post.setSlug(title.toLowerCase().replaceAll(" ", "-") + "-abc12345");
        post.setContent("content");
        post.setStatus(status);
        post.setAuthorId(authorId);
        setTimestamp(post);
        return post;
    }

    private UserEntity createUser() {
        UserEntity user = new UserEntity();
        user.setId(authorId);
        user.setUsername("user1");
        return user;
    }

    private void setTimestamp(PostEntity post) {
        try {
            Field f = post.getClass().getSuperclass().getDeclaredField("createdAt");
            f.setAccessible(true);
            f.set(post, Instant.now());
            Field f2 = post.getClass().getSuperclass().getDeclaredField("updatedAt");
            f2.setAccessible(true);
            f2.set(post, Instant.now());
        } catch (Exception ignored) {}
    }
}
