package com.mooc.app.repository;

import com.mooc.app.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PostRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private PostRepository postRepository;

    private UUID authorId;

    @BeforeEach
    void setup() {
        authorId = UUID.randomUUID();
    }

    // === findByUpVoteCountCursor ===

    @Test
    void findByUpVoteCountCursor_excludesHigherVoteCount() {
        // post1: 3 votes, post2: 5 votes (both auto-timestamped ~same instant)
        PostEntity post1 = createPost("Post1", authorId, 3, 0);
        PostEntity post2 = createPost("Post2", authorId, 5, 0);
        em.flush();
        em.clear();

        // Cursor at 3 votes, current time → includes only those strictly below
        List<PostEntity> result = postRepository.findByUpVoteCountCursor(
                PostStatus.PUBLISHED, 3L, Instant.now().truncatedTo(ChronoUnit.MILLIS),
                PageRequest.of(0, 10));

        // post1 has exactly 3 votes = cursorVotes, but post1.createdAt > now? No.
        // post2 has 5 votes > cursorVotes. Not included.
        // Result: empty or just post1 depending on timing
        // Since post1.createdAt <= now, condition is: createdAt < cursorTime → post1.createdAt < now ✓
        // But voteCount = 3 = cursorVotes, so second condition: createdAt < cursorTime
        // post1.createdAt < now → true → post1 IS included
        // post2: 5 < 3? No. 5 = 3? No. Excluded.
        assertEquals(1, result.size());
        assertEquals("Post1", result.get(0).getTitle());
    }

    @Test
    void findByUpVoteCountCursor_tiebreaker_returnsOlder() {
        // Both have 5 votes, created sequentially (post1 first, post2 second)
        PostEntity post1 = createPost("Post1", authorId, 5, 0);
        PostEntity post2 = createPost("Post2", authorId, 5, 0);
        em.flush();
        em.clear();

        // Sort order: DESC votes, DESC createdAt → post2(newer) comes first, post1(older) comes second
        // Cursor at post2 (newer) → should return post1 (older = after in DESC order)
        List<PostEntity> result = postRepository.findByUpVoteCountCursor(
                PostStatus.PUBLISHED, 5L, post2.getCreatedAt(),
                PageRequest.of(0, 10));

        assertEquals(1, result.size());
        assertEquals("Post1", result.get(0).getTitle());
    }

    @Test
    void findByUpVoteCountCursor_emptyResult() {
        PostEntity post = createPost("OnlyPost", authorId, 10, 0);
        em.flush();
        em.clear();

        // Cursor exactly at this post → nothing after it
        List<PostEntity> result = postRepository.findByUpVoteCountCursor(
                PostStatus.PUBLISHED, 10L, post.getCreatedAt(),
                PageRequest.of(0, 10));

        assertTrue(result.isEmpty());
    }

    @Test
    void findByUpVoteCountCursor_multiTier() {
        PostEntity post1 = createPost("Post1", authorId, 1, 0);
        PostEntity post2 = createPost("Post2", authorId, 3, 0);
        PostEntity post3 = createPost("Post3", authorId, 5, 0);
        em.flush();
        em.clear();

        // Cursor at post3 (5 votes, post3.createdAt)
        // post2: 3 < 5 → ✓ included
        // post1: 1 < 5 → ✓ included
        List<PostEntity> result = postRepository.findByUpVoteCountCursor(
                PostStatus.PUBLISHED, 5L, post3.getCreatedAt(),
                PageRequest.of(0, 10));

        assertEquals(2, result.size());
        assertEquals("Post2", result.get(0).getTitle());
        assertEquals("Post1", result.get(1).getTitle());
    }

    // === findByCommentCountCursor ===

    @Test
    void findByCommentCountCursor_tiebreaker() {
        PostEntity post1 = createPost("Post1", authorId, 0, 3);
        PostEntity post2 = createPost("Post2", authorId, 0, 3);
        em.flush();
        em.clear();

        // DESC order: post2(newer) first, post1(older) second
        // Cursor at post2 → returns post1
        List<PostEntity> result = postRepository.findByCommentCountCursor(
                PostStatus.PUBLISHED, 3L, post2.getCreatedAt(),
                PageRequest.of(0, 10));

        assertEquals(1, result.size());
        assertEquals("Post1", result.get(0).getTitle());
    }

    // === findByAuthorIdAndUpVoteCountCursor ===

    @Test
    void findByAuthorIdAndUpVoteCountCursor_filtersByAuthor() {
        UUID otherAuthor = UUID.randomUUID();
        PostEntity myPost = createPost("MyPost", authorId, 2, 0);
        PostEntity otherPost = createPost("OtherPost", otherAuthor, 10, 0);
        em.flush();
        em.clear();

        // Cursor: high enough to include myPost (but otherPost filtered by authorId)
        List<PostEntity> result = postRepository.findByAuthorIdAndUpVoteCountCursor(
                authorId, PostStatus.PUBLISHED, 10L, otherPost.getCreatedAt(),
                PageRequest.of(0, 10));

        assertEquals(1, result.size());
        assertEquals("MyPost", result.get(0).getTitle());
    }

    // === Helpers ===

    private PostEntity createPost(String title, UUID author, int votes, int comments) {
        PostEntity post = new PostEntity();
        post.setTitle(title);
        post.setContent("Content for " + title);
        post.setAuthorId(author);
        post.setStatus(PostStatus.PUBLISHED);
        post = em.persist(post);

        for (int i = 0; i < votes; i++) {
            VoteEntity vote = new VoteEntity();
            vote.setPostId(post.getId());
            vote.setUserId(UUID.randomUUID());
            vote.setVoteType(VoteType.UP);
            em.persist(vote);
        }

        for (int i = 0; i < comments; i++) {
            CommentEntity comment = new CommentEntity();
            comment.setPostId(post.getId());
            comment.setUserId(UUID.randomUUID());
            comment.setContent("Comment " + i);
            em.persist(comment);
        }

        return post;
    }
}
