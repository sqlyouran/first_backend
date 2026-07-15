package com.mooc.app.service;

import com.mooc.app.dto.response.VoteResponse;
import com.mooc.app.dto.response.VoteStatsResponse;
import com.mooc.app.entity.NotificationType;
import com.mooc.app.entity.PostEntity;
import com.mooc.app.entity.VoteEntity;
import com.mooc.app.entity.VoteType;
import com.mooc.app.exception.PostException;
import com.mooc.app.repository.PostRepository;
import com.mooc.app.repository.VoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.anyInt;

@ExtendWith(MockitoExtension.class)
class VoteServiceTest {

    @Mock private VoteRepository voteRepository;
    @Mock private PostRepository postRepository;
    @Mock private NotificationService notificationService;
    @Mock private GenericCacheService cacheService;

    private VoteService voteService;

    private final UUID postId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID authorId = UUID.randomUUID();
    private PostEntity post;

    @BeforeEach
    void setUp() {
        voteService = new VoteService(voteRepository, postRepository, notificationService, cacheService);
        post = new PostEntity();
        post.setId(postId);
        post.setAuthorId(authorId);
    }

    // ======================== vote ========================

    @Nested
    class VoteTests {

        @Test
        void voteUp_createsVoteAndNotification() {
            when(postRepository.findByIdAndDeletedFalse(postId)).thenReturn(Optional.of(post));
            when(voteRepository.findByPostIdAndUserId(postId, userId)).thenReturn(Optional.empty());

            VoteResponse response = voteService.vote(postId, userId, "up", "req-1");

            assertEquals("up", response.getVoteType());
            verify(voteRepository).save(argThat(v ->
                    v.getVoteType() == VoteType.UP &&
                    v.getPostId().equals(postId) &&
                    v.getUserId().equals(userId)));
            verify(notificationService).createNotification(authorId, userId,
                    NotificationType.POST_LIKED, postId, "post", null);
            verify(postRepository).incrementUpVoteCount(postId, 1);
        }

        @Test
        void voteDown_createsVoteNoNotification() {
            when(postRepository.findByIdAndDeletedFalse(postId)).thenReturn(Optional.of(post));
            when(voteRepository.findByPostIdAndUserId(postId, userId)).thenReturn(Optional.empty());

            VoteResponse response = voteService.vote(postId, userId, "down", "req-1");

            assertEquals("down", response.getVoteType());
            verify(voteRepository).save(argThat(v -> v.getVoteType() == VoteType.DOWN));
            verify(notificationService, never()).createNotification(any(), any(), any(), any(), any(), any());
            verify(postRepository, never()).incrementUpVoteCount(any(), anyInt());
        }

        @Test
        void sameUpVote_cancelsAndDeletesNotification() {
            VoteEntity existingVote = new VoteEntity();
            existingVote.setVoteType(VoteType.UP);
            when(postRepository.findByIdAndDeletedFalse(postId)).thenReturn(Optional.of(post));
            when(voteRepository.findByPostIdAndUserId(postId, userId)).thenReturn(Optional.of(existingVote));

            VoteResponse response = voteService.vote(postId, userId, "up", "req-1");

            assertNull(response.getVoteType());
            verify(voteRepository).delete(existingVote);
            verify(notificationService).deleteNotification(authorId, userId,
                    NotificationType.POST_LIKED, postId);
            verify(postRepository).incrementUpVoteCount(postId, -1);
        }

        @Test
        void sameDownVote_cancelsNoNotification() {
            VoteEntity existingVote = new VoteEntity();
            existingVote.setVoteType(VoteType.DOWN);
            when(postRepository.findByIdAndDeletedFalse(postId)).thenReturn(Optional.of(post));
            when(voteRepository.findByPostIdAndUserId(postId, userId)).thenReturn(Optional.of(existingVote));

            VoteResponse response = voteService.vote(postId, userId, "down", "req-1");

            assertNull(response.getVoteType());
            verify(voteRepository).delete(existingVote);
            verify(notificationService, never()).deleteNotification(any(), any(), any(), any());
        }

        @Test
        void switchUpToDown_deletesNotification() {
            VoteEntity existingVote = new VoteEntity();
            existingVote.setVoteType(VoteType.UP);
            when(postRepository.findByIdAndDeletedFalse(postId)).thenReturn(Optional.of(post));
            when(voteRepository.findByPostIdAndUserId(postId, userId)).thenReturn(Optional.of(existingVote));

            VoteResponse response = voteService.vote(postId, userId, "down", "req-1");

            assertEquals("down", response.getVoteType());
            assertEquals(VoteType.DOWN, existingVote.getVoteType());
            verify(notificationService).deleteNotification(authorId, userId,
                    NotificationType.POST_LIKED, postId);
            verify(postRepository).incrementUpVoteCount(postId, -1);
        }

        @Test
        void switchDownToUp_createsNotification() {
            VoteEntity existingVote = new VoteEntity();
            existingVote.setVoteType(VoteType.DOWN);
            when(postRepository.findByIdAndDeletedFalse(postId)).thenReturn(Optional.of(post));
            when(voteRepository.findByPostIdAndUserId(postId, userId)).thenReturn(Optional.of(existingVote));

            VoteResponse response = voteService.vote(postId, userId, "up", "req-1");

            assertEquals("up", response.getVoteType());
            assertEquals(VoteType.UP, existingVote.getVoteType());
            verify(notificationService).createNotification(authorId, userId,
                    NotificationType.POST_LIKED, postId, "post", null);
            verify(postRepository).incrementUpVoteCount(postId, 1);
        }

        @Test
        void invalidVoteType_throwsError() {
            when(postRepository.findByIdAndDeletedFalse(postId)).thenReturn(Optional.of(post));

            PostException ex = assertThrows(PostException.class,
                    () -> voteService.vote(postId, userId, "invalid", "req-1"));
            assertEquals("validation_error", ex.getErrorCode());
        }

        @Test
        void postNotFound_throwsError() {
            when(postRepository.findByIdAndDeletedFalse(postId)).thenReturn(Optional.empty());

            PostException ex = assertThrows(PostException.class,
                    () -> voteService.vote(postId, userId, "up", "req-1"));
            assertEquals("not_found", ex.getErrorCode());
        }
    }

    // ======================== removeVote ========================

    @Nested
    class RemoveVoteTests {

        @Test
        void removeVote_deletesExisting() {
            VoteEntity vote = new VoteEntity();
            when(voteRepository.findByPostIdAndUserId(postId, userId)).thenReturn(Optional.of(vote));

            voteService.removeVote(postId, userId);

            verify(voteRepository).delete(vote);
        }

        @Test
        void removeVote_noVote_noException() {
            when(voteRepository.findByPostIdAndUserId(postId, userId)).thenReturn(Optional.empty());

            assertDoesNotThrow(() -> voteService.removeVote(postId, userId));
            verify(voteRepository, never()).delete(any());
        }
    }

    // ======================== getVoteStats ========================

    @Nested
    class GetVoteStatsTests {

        @Test
        void getVoteStats_withUser_returnsCountsAndVote() {
            VoteEntity userVote = new VoteEntity();
            userVote.setVoteType(VoteType.UP);
            when(postRepository.findByIdAndDeletedFalse(postId)).thenReturn(Optional.of(post));
            when(voteRepository.countByPostIdAndVoteType(postId, VoteType.UP)).thenReturn(3L);
            when(voteRepository.countByPostIdAndVoteType(postId, VoteType.DOWN)).thenReturn(1L);
            when(voteRepository.findByPostIdAndUserId(postId, userId)).thenReturn(Optional.of(userVote));

            VoteStatsResponse response = voteService.getVoteStats(postId, Optional.of(userId), "req-1");

            assertEquals(3L, response.getUpCount());
            assertEquals(1L, response.getDownCount());
            assertEquals("up", response.getUserVote());
        }

        @Test
        void getVoteStats_noUser_returnsNullUserVote() {
            when(postRepository.findByIdAndDeletedFalse(postId)).thenReturn(Optional.of(post));
            when(voteRepository.countByPostIdAndVoteType(postId, VoteType.UP)).thenReturn(2L);
            when(voteRepository.countByPostIdAndVoteType(postId, VoteType.DOWN)).thenReturn(0L);

            VoteStatsResponse response = voteService.getVoteStats(postId, Optional.empty(), "req-1");

            assertEquals(2L, response.getUpCount());
            assertEquals(0L, response.getDownCount());
            assertNull(response.getUserVote());
        }
    }
}
