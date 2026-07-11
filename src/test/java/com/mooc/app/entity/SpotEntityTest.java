package com.mooc.app.entity;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class SpotEntityTest {

    @Autowired
    private TestEntityManager em;

    @Test
    void baseEntityFieldsAutoFilled() {
        SpotEntity spot = createSampleSpot();
        em.persist(spot);
        em.flush();

        assertNotNull(spot.getId());
        assertNotNull(spot.getCreatedAt());
        assertNotNull(spot.getUpdatedAt());
        assertFalse(spot.isDeleted());
        assertEquals("Forbidden City", spot.getName());
        assertEquals("forbidden-city", spot.getSlug());
        assertEquals(SpotStatus.PUBLISHED, spot.getStatus());
        assertEquals(0, spot.getRating().compareTo(new BigDecimal("4.8")));
        assertEquals(1000, spot.getViewCount());
        assertEquals(200, spot.getBookmarkCount());
    }

    @Test
    void nameNotBlankConstraint() {
        SpotEntity spot = createSampleSpot();
        spot.setName("");
        em.persist(spot);

        assertThrows(jakarta.validation.ConstraintViolationException.class, () -> em.flush());
    }

    @Test
    void practicalFields_canBeNull() {
        SpotEntity spot = createSampleSpot();
        spot.setSlug("null-practical-fields");
        // ticketPrice, openingHours, address default to null
        em.persist(spot);
        em.flush();

        SpotEntity found = em.find(SpotEntity.class, spot.getId());
        assertNotNull(found);
        assertNull(found.getTicketPrice());
        assertNull(found.getOpeningHours());
        assertNull(found.getAddress());
    }

    @Test
    void practicalFields_canBeFilled() {
        SpotEntity spot = createSampleSpot();
        spot.setSlug("filled-practical-fields");
        spot.setTicketPrice("旺季60元/淡季40元");
        spot.setOpeningHours("08:30-17:00（4月-10月）");
        spot.setAddress("北京市东城区景山前街4号");
        em.persist(spot);
        em.flush();

        SpotEntity found = em.find(SpotEntity.class, spot.getId());
        assertEquals("旺季60元/淡季40元", found.getTicketPrice());
        assertEquals("08:30-17:00（4月-10月）", found.getOpeningHours());
        assertEquals("北京市东城区景山前街4号", found.getAddress());
    }

    @Test
    void slugUniqueConstraintViolation() {
        SpotEntity spot1 = createSampleSpot();
        em.persist(spot1);
        em.flush();

        SpotEntity spot2 = createSampleSpot();
        em.persist(spot2);

        assertThrows(ConstraintViolationException.class, () -> em.flush());
    }

    static SpotEntity createSampleSpot() {
        SpotEntity spot = new SpotEntity();
        spot.setName("Forbidden City");
        spot.setNameZh("故宫");
        spot.setSlug("forbidden-city");
        spot.setDescription("The world's largest palace complex");
        spot.setDescriptionZh("世界上最大的宫殿建筑群");
        spot.setCoverImage("https://picsum.photos/800/600?random=100");
        spot.setGallery(List.of("https://picsum.photos/800/600?random=101", "https://picsum.photos/800/600?random=102"));
        spot.setTags(List.of("heritage", "history", "architecture"));
        spot.setCityId(UUID.fromString("a1111111-1111-1111-1111-111111111111"));
        spot.setCityName("Beijing");
        spot.setStatus(SpotStatus.PUBLISHED);
        spot.setRating(new BigDecimal("4.8"));
        spot.setViewCount(1000);
        spot.setBookmarkCount(200);
        return spot;
    }
}
