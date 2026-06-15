package com.mooc.app.repository;

import com.mooc.app.entity.SpotEntity;
import com.mooc.app.entity.SpotStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class SpotRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private SpotRepository spotRepository;

    private static final UUID BEIJING_ID = UUID.fromString("a1111111-1111-1111-1111-111111111111");
    private static final UUID SHANGHAI_ID = UUID.fromString("a2222222-2222-2222-2222-222222222222");

    @Test
    void findByDeletedFalse_returnsAllActiveSpots() {
        createSpot("Forbidden City", "forbidden-city", BEIJING_ID, SpotStatus.PUBLISHED);
        createSpot("Temple of Heaven", "temple-of-heaven", BEIJING_ID, SpotStatus.PUBLISHED);
        em.flush();
        em.clear();

        Page<SpotEntity> result = spotRepository.findByDeletedFalse(PageRequest.of(0, 20));

        assertEquals(2, result.getTotalElements());
    }

    @Test
    void findByDeletedFalse_excludesDeletedSpots() {
        SpotEntity active = createSpot("Forbidden City", "forbidden-city", BEIJING_ID, SpotStatus.PUBLISHED);
        SpotEntity deleted = createSpot("Temple of Heaven", "temple-of-heaven", BEIJING_ID, SpotStatus.PUBLISHED);
        em.flush();

        deleted.markDeleted();
        em.merge(deleted);
        em.flush();
        em.clear();

        Page<SpotEntity> result = spotRepository.findByDeletedFalse(PageRequest.of(0, 20));

        assertEquals(1, result.getTotalElements());
        assertEquals("Forbidden City", result.getContent().get(0).getName());
    }

    @Test
    void findByCityIdAndDeletedFalse_returnsCitySpots() {
        createSpot("Forbidden City", "forbidden-city", BEIJING_ID, SpotStatus.PUBLISHED);
        createSpot("The Bund", "the-bund", SHANGHAI_ID, SpotStatus.PUBLISHED);
        em.flush();
        em.clear();

        Page<SpotEntity> result = spotRepository.findByCityIdAndDeletedFalse(BEIJING_ID, PageRequest.of(0, 20));

        assertEquals(1, result.getTotalElements());
        assertEquals("Forbidden City", result.getContent().get(0).getName());
    }

    @Test
    void findByIdAndDeletedFalse_returnsActiveSpot() {
        SpotEntity spot = createSpot("Forbidden City", "forbidden-city", BEIJING_ID, SpotStatus.PUBLISHED);
        em.flush();
        em.clear();

        Optional<SpotEntity> result = spotRepository.findByIdAndDeletedFalse(spot.getId());

        assertTrue(result.isPresent());
        assertEquals("Forbidden City", result.get().getName());
    }

    @Test
    void findByIdAndDeletedFalse_returnsEmptyForDeletedSpot() {
        SpotEntity spot = createSpot("Forbidden City", "forbidden-city", BEIJING_ID, SpotStatus.PUBLISHED);
        em.flush();

        spot.markDeleted();
        em.merge(spot);
        em.flush();
        em.clear();

        Optional<SpotEntity> result = spotRepository.findByIdAndDeletedFalse(spot.getId());

        assertFalse(result.isPresent());
    }

    @Test
    void findByStatusAndDeletedFalseOrderByRatingDesc_returnsSortedSpots() {
        SpotEntity low = createSpot("Low", "low", BEIJING_ID, SpotStatus.PUBLISHED);
        low.setRating(new BigDecimal("3.0"));
        SpotEntity high = createSpot("High", "high", BEIJING_ID, SpotStatus.PUBLISHED);
        high.setRating(new BigDecimal("4.9"));
        createSpot("Draft", "draft", BEIJING_ID, SpotStatus.DRAFT);
        em.flush();
        em.clear();

        List<SpotEntity> result = spotRepository.findByStatusAndDeletedFalse(
                SpotStatus.PUBLISHED, PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "rating"))).getContent();

        assertEquals(2, result.size());
        assertEquals("High", result.get(0).getName());
        assertEquals("Low", result.get(1).getName());
    }

    private SpotEntity createSpot(String name, String slug, UUID cityId, SpotStatus status) {
        SpotEntity spot = new SpotEntity();
        spot.setName(name);
        spot.setSlug(slug);
        spot.setCityId(cityId);
        spot.setCityName("City");
        spot.setStatus(status);
        em.persist(spot);
        return spot;
    }
}
