package com.mooc.app.repository;

import com.mooc.app.entity.SpotPostEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SpotPostRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private SpotPostRepository spotPostRepository;

    private static final UUID SPOT_A = UUID.fromString("b1111111-1111-1111-1111-111111111111");
    private static final UUID SPOT_B = UUID.fromString("b2222222-2222-2222-2222-222222222222");
    private static final UUID POST_X = UUID.fromString("d1111111-1111-1111-1111-111111111111");
    private static final UUID POST_Y = UUID.fromString("d2222222-2222-2222-2222-222222222222");
    private static final UUID POST_Z = UUID.fromString("d3333333-3333-3333-3333-333333333333");

    @Test
    void findBySpotIdAndDeletedFalse_returnsAllActiveAssociations() {
        persistAssociation(SPOT_A, POST_X);
        persistAssociation(SPOT_A, POST_Y);
        persistAssociation(SPOT_A, POST_Z);
        persistAssociation(SPOT_B, POST_X);
        em.flush();
        em.clear();

        Page<SpotPostEntity> result = spotPostRepository.findBySpotIdAndDeletedFalse(SPOT_A, PageRequest.of(0, 20));

        assertEquals(3, result.getTotalElements());
    }

    @Test
    void findBySpotIdAndDeletedFalse_excludesDeletedAssociations() {
        SpotPostEntity active1 = persistAssociation(SPOT_A, POST_X);
        SpotPostEntity active2 = persistAssociation(SPOT_A, POST_Y);
        SpotPostEntity deleted = persistAssociation(SPOT_A, POST_Z);
        em.flush();

        deleted.markDeleted();
        em.merge(deleted);
        em.flush();
        em.clear();

        Page<SpotPostEntity> result = spotPostRepository.findBySpotIdAndDeletedFalse(SPOT_A, PageRequest.of(0, 20));

        assertEquals(2, result.getTotalElements());
    }

    @Test
    void findByPostIdAndDeletedFalse_returnsSpotAssociations() {
        persistAssociation(SPOT_A, POST_X);
        persistAssociation(SPOT_B, POST_X);
        persistAssociation(SPOT_A, POST_Y);
        em.flush();
        em.clear();

        List<SpotPostEntity> result = spotPostRepository.findByPostIdAndDeletedFalse(POST_X);

        assertEquals(2, result.size());
    }

    @Test
    void findByPostIdAndDeletedFalse_excludesDeleted() {
        SpotPostEntity active = persistAssociation(SPOT_A, POST_X);
        SpotPostEntity deleted = persistAssociation(SPOT_B, POST_X);
        em.flush();

        deleted.markDeleted();
        em.merge(deleted);
        em.flush();
        em.clear();

        List<SpotPostEntity> result = spotPostRepository.findByPostIdAndDeletedFalse(POST_X);

        assertEquals(1, result.size());
    }

    @Test
    void existsBySpotIdAndPostIdAndDeletedFalse_returnsTrue() {
        persistAssociation(SPOT_A, POST_X);
        em.flush();
        em.clear();

        assertTrue(spotPostRepository.existsBySpotIdAndPostIdAndDeletedFalse(SPOT_A, POST_X));
        assertFalse(spotPostRepository.existsBySpotIdAndPostIdAndDeletedFalse(SPOT_A, POST_Y));
    }

    private SpotPostEntity persistAssociation(UUID spotId, UUID postId) {
        SpotPostEntity sp = new SpotPostEntity();
        sp.setSpotId(spotId);
        sp.setPostId(postId);
        em.persist(sp);
        return sp;
    }
}
