package com.mooc.app.entity;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.hibernate.exception.ConstraintViolationException;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class CityEntityTest {

    @Autowired
    private TestEntityManager em;

    @Test
    void baseEntityFieldsAutoFilled() {
        CityEntity city = createSampleCity();
        em.persist(city);
        em.flush();

        assertNotNull(city.getId());
        assertNotNull(city.getCreatedAt());
        assertNotNull(city.getUpdatedAt());
        assertFalse(city.isDeleted());
    }

    @Test
    void slugUniqueConstraintViolation() {
        CityEntity city1 = createSampleCity();
        em.persist(city1);
        em.flush();

        CityEntity city2 = createSampleCity();
        em.persist(city2);

        assertThrows(ConstraintViolationException.class, () -> em.flush());
    }

    private CityEntity createSampleCity() {
        CityEntity city = new CityEntity();
        city.setName("Beijing");
        city.setNameZh("北京");
        city.setSlug("beijing");
        city.setCoverImage("https://picsum.photos/800/600?random=10");
        city.setDescription("Ancient capital with imperial grandeur");
        city.setBestSeason("Autumn");
        return city;
    }
}
