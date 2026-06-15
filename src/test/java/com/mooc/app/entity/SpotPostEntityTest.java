package com.mooc.app.entity;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class SpotPostEntityTest {

    @Autowired
    private TestEntityManager em;

    private static final UUID SPOT_ID = UUID.fromString("b1111111-1111-1111-1111-111111111111");
    private static final UUID POST_ID = UUID.fromString("d1111111-1111-1111-1111-111111111111");

    @Test
    void persistValidSpotPost_baseEntityFieldsAutoFilled() {
        SpotPostEntity sp = new SpotPostEntity();
        sp.setSpotId(SPOT_ID);
        sp.setPostId(POST_ID);

        em.persist(sp);
        em.flush();

        assertNotNull(sp.getId());
        assertNotNull(sp.getCreatedAt());
        assertNotNull(sp.getUpdatedAt());
        assertFalse(sp.isDeleted());
        assertEquals(SPOT_ID, sp.getSpotId());
        assertEquals(POST_ID, sp.getPostId());
    }

    @Test
    void duplicateSpotPostPair_triggersUniqueConstraint() {
        SpotPostEntity sp1 = new SpotPostEntity();
        sp1.setSpotId(SPOT_ID);
        sp1.setPostId(POST_ID);
        em.persist(sp1);
        em.flush();

        SpotPostEntity sp2 = new SpotPostEntity();
        sp2.setSpotId(SPOT_ID);
        sp2.setPostId(POST_ID);
        em.persist(sp2);

        assertThrows(ConstraintViolationException.class, () -> em.flush());
    }

    @Test
    void nullSpotId_triggersConstraintViolation() {
        SpotPostEntity sp = new SpotPostEntity();
        sp.setPostId(POST_ID);

        assertThrows(jakarta.validation.ConstraintViolationException.class, () -> {
            em.persist(sp);
            em.flush();
        });
    }

    @Test
    void nullPostId_triggersConstraintViolation() {
        SpotPostEntity sp = new SpotPostEntity();
        sp.setSpotId(SPOT_ID);

        assertThrows(jakarta.validation.ConstraintViolationException.class, () -> {
            em.persist(sp);
            em.flush();
        });
    }
}
