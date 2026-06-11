package com.mooc.app.entity;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PostEntityTest {

    @Autowired
    private TestEntityManager em;

    @Test
    void baseEntityFieldsAutoFilled() {
        PostEntity post = createSamplePost();
        em.persist(post);
        em.flush();

        assertNotNull(post.getId());
        assertNotNull(post.getCreatedAt());
        assertNotNull(post.getUpdatedAt());
        assertFalse(post.isDeleted());
    }

    @Test
    void markDeletedSetsDeletedTrue() {
        PostEntity post = createSamplePost();
        em.persist(post);
        em.flush();

        post.markDeleted();
        em.merge(post);
        em.flush();

        assertTrue(post.isDeleted());
    }

    @Test
    void tagsPersistedAsJsonAndRestored() {
        PostEntity post = createSamplePost();
        post.setTags(List.of("travel", "food", "culture"));
        em.persist(post);
        em.flush();
        em.clear();

        PostEntity loaded = em.find(PostEntity.class, post.getId());
        assertNotNull(loaded);
        assertEquals(List.of("travel", "food", "culture"), loaded.getTags());
    }

    @Test
    void defaultStatusIsPublished() {
        PostEntity post = createSamplePost();
        em.persist(post);
        em.flush();

        assertEquals(PostStatus.PUBLISHED, post.getStatus());
    }

    private PostEntity createSamplePost() {
        PostEntity post = new PostEntity();
        post.setTitle("Test Post");
        post.setContent("# Hello\nThis is markdown content.");
        post.setCoverImage("https://example.com/cover.jpg");
        post.setAuthorId(UUID.randomUUID());
        return post;
    }
}
