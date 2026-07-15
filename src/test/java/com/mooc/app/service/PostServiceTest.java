package com.mooc.app.service;

import com.mooc.app.dto.CreatePostRequest;
import com.mooc.app.dto.UpdatePostRequest;
import com.mooc.app.dto.response.PostListResponse;
import com.mooc.app.dto.response.PostResponse;
import com.mooc.app.entity.PostEntity;
import com.mooc.app.entity.PostSortBy;
import com.mooc.app.entity.PostStatus;
import com.mooc.app.entity.UserEntity;
import com.mooc.app.exception.PostException;
import com.mooc.app.repository.BookmarkRepository;
import com.mooc.app.repository.PostRepository;
import com.mooc.app.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock private PostRepository postRepository;
    @Mock private UserRepository userRepository;
    @Mock private BookmarkRepository bookmarkRepository;
    @Mock private GenericCacheService cacheService;

    private PostService postService;

    private final UUID authorId = UUID.randomUUID();
    private final UUID postId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        postService = new PostService(postRepository, userRepository, bookmarkRepository, cacheService);
    }

    private PostEntity createTestPost(UUID id, String title, PostStatus status) {
        PostEntity post = new PostEntity();
        post.setId(id);
        post.setTitle(title);
        post.setSlug("test-slug-" + id.toString().substring(0, 8));
        post.setContent("test content");
        post.setStatus(status);
        post.setAuthorId(authorId);
        setTimestamp(post, Instant.now(), Instant.now());
        return post;
    }

    private void setTimestamp(PostEntity entity, Instant createdAt, Instant updatedAt) {
        try {
            Field createdAtField = entity.getClass().getSuperclass().getDeclaredField("createdAt");
            createdAtField.setAccessible(true);
            createdAtField.set(entity, createdAt);
            Field updatedAtField = entity.getClass().getSuperclass().getDeclaredField("updatedAt");
            updatedAtField.setAccessible(true);
            updatedAtField.set(entity, updatedAt);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void mockAuthorLookup() {
        UserEntity author = new UserEntity();
        author.setId(authorId);
        author.setUsername("testauthor");
        when(userRepository.findById(authorId)).thenReturn(Optional.of(author));
    }

    private void mockPostSave() {
        when(postRepository.save(any(PostEntity.class))).thenAnswer(invocation -> {
            PostEntity p = invocation.getArgument(0);
            if (p.getId() == null) p.setId(postId);
            if (p.getCreatedAt() == null) setTimestamp(p, Instant.now(), Instant.now());
            return p;
        });
    }

    private void mockEmptyStats() {
        when(bookmarkRepository.batchCountBookmarks(anyList(), any())).thenReturn(List.of());
    }

    // ======================== createPost ========================

    @Nested
    class CreatePost {

        @Test
        void createPost_generatesSlug() {
            when(userRepository.existsById(authorId)).thenReturn(true);
            mockAuthorLookup();
            mockPostSave();

            CreatePostRequest request = new CreatePostRequest("My Test Post", "content", null, null, "PUBLISHED");
            PostResponse response = postService.createPost(request, authorId, "req-1");

            assertNotNull(response.getSlug());
            assertTrue(response.getSlug().contains("my-test-post"));
            assertTrue(response.getSlug().contains(postId.toString().substring(0, 8)));
            verify(postRepository, times(2)).save(any(PostEntity.class));
        }

        @Test
        void createPost_userNotFound_throwsError() {
            when(userRepository.existsById(authorId)).thenReturn(false);

            CreatePostRequest request = new CreatePostRequest("title", "content", null, null, null);
            PostException ex = assertThrows(PostException.class,
                    () -> postService.createPost(request, authorId, "req-1"));
            assertEquals("not_found", ex.getErrorCode());
        }

        @Test
        void createPost_invalidStatus_throwsError() {
            when(userRepository.existsById(authorId)).thenReturn(true);

            CreatePostRequest request = new CreatePostRequest("title", "content", null, null, "invalid_status");
            PostException ex = assertThrows(PostException.class,
                    () -> postService.createPost(request, authorId, "req-1"));
            assertEquals("validation_error", ex.getErrorCode());
        }

        @Test
        void createPost_nullTags_defaultsToEmptyList() {
            when(userRepository.existsById(authorId)).thenReturn(true);
            mockAuthorLookup();
            mockPostSave();

            CreatePostRequest request = new CreatePostRequest("title", "content", null, null, null);
            PostResponse response = postService.createPost(request, authorId, "req-1");

            assertNotNull(response);
            // tags should be empty list, not null (verified via saved entity in createPost)
            verify(postRepository, atLeast(1)).save(argThat(p ->
                    p.getTags() != null && p.getTags().isEmpty()));
        }
    }

    // ======================== updatePost ========================

    @Nested
    class UpdatePost {

        private PostEntity existingPost;

        @BeforeEach
        void setUp() {
            existingPost = createTestPost(postId, "Original Title", PostStatus.PUBLISHED);
        }

        @Test
        void updatePost_byAuthor_updatesFields() {
            when(postRepository.findByIdAndDeletedFalse(postId)).thenReturn(Optional.of(existingPost));
            when(postRepository.save(any(PostEntity.class))).thenAnswer(inv -> inv.getArgument(0));
            mockAuthorLookup();

            UpdatePostRequest request = new UpdatePostRequest("New Title", null, null, null, null);
            PostResponse response = postService.updatePost(postId, request, authorId, "req-1");

            assertEquals("New Title", existingPost.getTitle());
        }

        @Test
        void updatePost_nonAuthor_throwsForbidden() {
            when(postRepository.findByIdAndDeletedFalse(postId)).thenReturn(Optional.of(existingPost));

            UUID otherUser = UUID.randomUUID();
            UpdatePostRequest request = new UpdatePostRequest("New Title", null, null, null, null);
            PostException ex = assertThrows(PostException.class,
                    () -> postService.updatePost(postId, request, otherUser, "req-1"));
            assertEquals("access_denied", ex.getErrorCode());
        }

        @Test
        void updatePost_notFound_throwsError() {
            when(postRepository.findByIdAndDeletedFalse(postId)).thenReturn(Optional.empty());

            UpdatePostRequest request = new UpdatePostRequest("title", null, null, null, null);
            PostException ex = assertThrows(PostException.class,
                    () -> postService.updatePost(postId, request, authorId, "req-1"));
            assertEquals("not_found", ex.getErrorCode());
        }
    }

    // ======================== deletePost ========================

    @Nested
    class DeletePost {

        @Test
        void deletePost_byAuthor_marksDeleted() {
            PostEntity post = createTestPost(postId, "title", PostStatus.PUBLISHED);
            when(postRepository.findByIdAndDeletedFalse(postId)).thenReturn(Optional.of(post));
            when(postRepository.save(any(PostEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            postService.deletePost(postId, authorId);

            assertTrue(post.isDeleted());
            verify(postRepository).save(post);
        }

        @Test
        void deletePost_nonAuthor_throwsForbidden() {
            PostEntity post = createTestPost(postId, "title", PostStatus.PUBLISHED);
            when(postRepository.findByIdAndDeletedFalse(postId)).thenReturn(Optional.of(post));

            UUID otherUser = UUID.randomUUID();
            PostException ex = assertThrows(PostException.class,
                    () -> postService.deletePost(postId, otherUser));
            assertEquals("access_denied", ex.getErrorCode());
        }
    }

    // ======================== getPost ========================

    @Nested
    class GetPost {

        @Test
        void getPost_byUuid_returnsPublished() {
            PostEntity post = createTestPost(postId, "title", PostStatus.PUBLISHED);
            when(postRepository.findByIdAndDeletedFalse(postId)).thenReturn(Optional.of(post));
            mockAuthorLookup();
            mockEmptyStats();

            PostResponse response = postService.getPost(postId.toString(), "req-1");

            assertEquals(postId.toString(), response.getId());
        }

        @Test
        void getPost_bySlug_returnsPublished() {
            PostEntity post = createTestPost(postId, "title", PostStatus.PUBLISHED);
            post.setSlug("my-slug");
            when(postRepository.findBySlugAndDeletedFalse("my-slug")).thenReturn(Optional.of(post));
            mockAuthorLookup();
            mockEmptyStats();

            PostResponse response = postService.getPost("my-slug", "req-1");

            assertEquals(postId.toString(), response.getId());
        }

        @Test
        void getPost_nonPublished_throwsNotFound() {
            PostEntity draft = createTestPost(postId, "title", PostStatus.DRAFT);
            when(postRepository.findByIdAndDeletedFalse(postId)).thenReturn(Optional.of(draft));

            PostException ex = assertThrows(PostException.class,
                    () -> postService.getPost(postId.toString(), "req-1"));
            assertEquals("not_found", ex.getErrorCode());
        }

        @Test
        void getPost_notFound_throwsError() {
            when(postRepository.findByIdAndDeletedFalse(postId)).thenReturn(Optional.empty());

            PostException ex = assertThrows(PostException.class,
                    () -> postService.getPost(postId.toString(), "req-1"));
            assertEquals("not_found", ex.getErrorCode());
        }
    }

    // ======================== listPosts ========================

    @Nested
    class ListPosts {

        @Test
        void listPosts_firstLoad_returnsCursor() {
            PostEntity post = createTestPost(postId, "title", PostStatus.PUBLISHED);
            Page<PostEntity> page = new PageImpl<>(List.of(post), PageRequest.of(0, 20), 1);
            when(postRepository.findByStatusAndDeletedFalse(eq(PostStatus.PUBLISHED), any(PageRequest.class)))
                    .thenReturn(page);
            mockEmptyStats();
            when(userRepository.findAllById(anyList())).thenReturn(List.of());

            PostListResponse response = postService.listPosts(1, 20, null, PostSortBy.LATEST, "req-1");

            assertEquals(1, response.getItems().size());
            assertEquals(1L, response.getTotal());
            assertFalse(response.isHasMore());
        }

        @Test
        void listPosts_sizeCappedAt100() {
            Page<PostEntity> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 100), 0);
            when(postRepository.findByStatusAndDeletedFalse(eq(PostStatus.PUBLISHED), any(PageRequest.class)))
                    .thenReturn(emptyPage);

            postService.listPosts(1, 500, null, PostSortBy.LATEST, "req-1");

            verify(postRepository).findByStatusAndDeletedFalse(eq(PostStatus.PUBLISHED),
                    argThat(p -> p.getPageSize() == 100));
        }

        @Test
        void listPosts_cursorMode_paginates() {
            Instant now = Instant.now();
            PostEntity post = createTestPost(postId, "title", PostStatus.PUBLISHED);
            String cursor = PostSortBy.LATEST.encodeCursor(0, now);
            when(postRepository.findByCreatedAtCursor(eq(PostStatus.PUBLISHED), any(Instant.class), any(PageRequest.class)))
                    .thenReturn(List.of(post));
            when(postRepository.findByStatusAndDeletedFalse(eq(PostStatus.PUBLISHED), any(PageRequest.class)))
                    .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 1), 1));
            mockEmptyStats();
            when(userRepository.findAllById(anyList())).thenReturn(List.of());

            PostListResponse response = postService.listPosts(1, 20, cursor, PostSortBy.LATEST, "req-1");

            assertEquals(1, response.getItems().size());
            assertNull(response.getPage()); // cursor mode has no page number
        }

        @Test
        void listUserPosts_filtersByAuthor() {
            PostEntity post = createTestPost(postId, "title", PostStatus.PUBLISHED);
            Page<PostEntity> page = new PageImpl<>(List.of(post), PageRequest.of(0, 20), 1);
            when(postRepository.findByAuthorIdAndStatusAndDeletedFalse(eq(authorId), eq(PostStatus.PUBLISHED), any(PageRequest.class)))
                    .thenReturn(page);
            mockEmptyStats();
            when(userRepository.findAllById(anyList())).thenReturn(List.of());

            PostListResponse response = postService.listUserPosts(authorId, 1, 20, null, PostSortBy.LATEST, "req-1");

            assertEquals(1, response.getItems().size());
        }
    }

    // ======================== slug generation ========================

    @Nested
    class SlugGeneration {

        @BeforeEach
        void setUpMocks() {
            when(userRepository.existsById(authorId)).thenReturn(true);
            mockAuthorLookup();
            when(postRepository.save(any(PostEntity.class))).thenAnswer(invocation -> {
                PostEntity p = invocation.getArgument(0);
                if (p.getId() == null) p.setId(UUID.randomUUID());
                if (p.getCreatedAt() == null) setTimestamp(p, Instant.now(), Instant.now());
                return p;
            });
        }

        @Test
        void slug_specialCharsCleaned() {
            CreatePostRequest request = new CreatePostRequest("Hello!@#$ World", "content", null, null, null);
            PostResponse response = postService.createPost(request, authorId, "req-1");

            assertTrue(response.getSlug().startsWith("hello-world"));
        }

        @Test
        void slug_longTitleTruncated() {
            String longTitle = "a".repeat(100);
            CreatePostRequest request = new CreatePostRequest(longTitle, "content", null, null, null);
            PostResponse response = postService.createPost(request, authorId, "req-1");

            // Slug title part should be ≤50 chars + "-" + 8-char ID suffix = max 59 chars
            String slugWithoutSuffix = response.getSlug().substring(0, response.getSlug().lastIndexOf('-'));
            assertTrue(slugWithoutSuffix.length() <= 50);
        }
    }
}
