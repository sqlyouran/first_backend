package com.mooc.app.service;

import com.mooc.app.dto.CreateCommentRequest;
import com.mooc.app.entity.*;
import com.mooc.app.repository.CommentRepository;
import com.mooc.app.repository.PostRepository;
import com.mooc.app.repository.SpotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock private CommentRepository commentRepository;
    @Mock private PostRepository postRepository;
    @Mock private SpotRepository spotRepository;
    @Mock private NotificationService notificationService;
    @Mock private GenericCacheService cacheService;

    private CommentService commentService;

    private final UUID postId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID authorId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        commentService = new CommentService(commentRepository, postRepository, spotRepository, notificationService, cacheService);
    }

    private void setTimestamp(Object entity, Instant createdAt, Instant updatedAt) {
        try {
            Field f = entity.getClass().getSuperclass().getDeclaredField("createdAt");
            f.setAccessible(true);
            f.set(entity, createdAt);
            Field f2 = entity.getClass().getSuperclass().getDeclaredField("updatedAt");
            f2.setAccessible(true);
            f2.set(entity, updatedAt);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    @Nested
    class CounterMaintenance {

        @Test
        void createComment_onPost_incrementsCachedCommentCount() {
            PostEntity post = new PostEntity();
            post.setId(postId);
            post.setAuthorId(authorId);
            when(postRepository.findByIdAndDeletedFalse(postId)).thenReturn(Optional.of(post));
            when(commentRepository.save(any(CommentEntity.class))).thenAnswer(inv -> {
                CommentEntity c = inv.getArgument(0);
                c.setId(UUID.randomUUID());
                setTimestamp(c, Instant.now(), Instant.now());
                return c;
            });

            commentService.createComment(postId, EntityType.POST, userId,
                    new CreateCommentRequest("nice post", null), "req-1");

            verify(postRepository).incrementCommentCount(postId, 1);
        }

        @Test
        void createReply_onPost_incrementsCachedCommentCount() {
            PostEntity post = new PostEntity();
            post.setId(postId);
            post.setAuthorId(authorId);

            CommentEntity parentComment = new CommentEntity();
            parentComment.setId(UUID.randomUUID());
            parentComment.setUserId(UUID.randomUUID());

            when(postRepository.findByIdAndDeletedFalse(postId)).thenReturn(Optional.of(post));
            when(commentRepository.findByIdAndDeletedFalse(parentComment.getId())).thenReturn(Optional.of(parentComment));
            when(commentRepository.save(any(CommentEntity.class))).thenAnswer(inv -> {
                CommentEntity c = inv.getArgument(0);
                c.setId(UUID.randomUUID());
                setTimestamp(c, Instant.now(), Instant.now());
                return c;
            });

            commentService.createComment(postId, EntityType.POST, userId,
                    new CreateCommentRequest("reply text", parentComment.getId()), "req-1");

            verify(postRepository).incrementCommentCount(postId, 1);
        }

        @Test
        void deleteComment_onPost_decrementsCachedCommentCount() {
            CommentEntity comment = new CommentEntity();
            comment.setId(UUID.randomUUID());
            comment.setEntityId(postId);
            comment.setEntityType(EntityType.POST);
            comment.setUserId(userId);
            comment.setContent("test");

            when(commentRepository.findByIdAndDeletedFalse(comment.getId())).thenReturn(Optional.of(comment));
            when(commentRepository.save(any(CommentEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            commentService.deleteComment(comment.getId(), userId);

            verify(postRepository).incrementCommentCount(postId, -1);
        }
    }
}
