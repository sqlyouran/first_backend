package com.mooc.app.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class BaseEntityTest {

    @Entity
    @Table(name = "test_entities")
    static class TestEntity extends BaseEntity {
        // Concrete subclass for testing BaseEntity behavior
    }

    @Autowired
    private EntityManager em;

    @Test
    void persist_setsIdAndTimestamps() {
        TestEntity entity = new TestEntity();
        em.persist(entity);
        em.flush();

        assertThat(entity.getId()).isNotNull();
        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(entity.getUpdatedAt()).isNotNull();
        assertThat(entity.isDeleted()).isFalse();
    }

    @Test
    void update_changesUpdatedAtButNotCreatedAt() throws Exception {
        TestEntity entity = new TestEntity();
        em.persist(entity);
        em.flush();

        Instant originalCreatedAt = entity.getCreatedAt();
        Instant originalUpdatedAt = entity.getUpdatedAt();

        // Small delay to ensure timestamp differs
        Thread.sleep(10);

        em.flush();
        em.clear();

        TestEntity found = em.find(TestEntity.class, entity.getId());
        found.markDeleted(); // trigger a change so @PreUpdate fires
        em.flush();

        assertThat(found.getCreatedAt()).isEqualTo(originalCreatedAt);
        assertThat(found.getUpdatedAt()).isAfter(originalUpdatedAt);
    }

    @Test
    void markDeleted_setsDeletedTrue() {
        TestEntity entity = new TestEntity();
        em.persist(entity);
        em.flush();

        assertThat(entity.isDeleted()).isFalse();

        entity.markDeleted();
        em.flush();

        assertThat(entity.isDeleted()).isTrue();
    }
}
