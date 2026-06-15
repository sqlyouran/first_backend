package com.mooc.app.repository;

import com.mooc.app.entity.CityEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class CityRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private CityRepository cityRepository;

    @Test
    void findByDeletedFalse_returnsAllActiveCities() {
        createCity("Beijing", "beijing");
        createCity("Shanghai", "shanghai");
        em.flush();
        em.clear();

        Page<CityEntity> result = cityRepository.findByDeletedFalse(PageRequest.of(0, 20));

        assertEquals(2, result.getTotalElements());
    }

    @Test
    void findByDeletedFalse_excludesDeletedCities() {
        CityEntity active = createCity("Beijing", "beijing");
        CityEntity deleted = createCity("Shanghai", "shanghai");
        em.flush();

        deleted.markDeleted();
        em.merge(deleted);
        em.flush();
        em.clear();

        Page<CityEntity> result = cityRepository.findByDeletedFalse(PageRequest.of(0, 20));

        assertEquals(1, result.getTotalElements());
        assertEquals("Beijing", result.getContent().get(0).getName());
    }

    @Test
    void findByIdAndDeletedFalse_returnsActiveCity() {
        CityEntity city = createCity("Beijing", "beijing");
        em.flush();
        em.clear();

        Optional<CityEntity> result = cityRepository.findByIdAndDeletedFalse(city.getId());

        assertTrue(result.isPresent());
        assertEquals("Beijing", result.get().getName());
    }

    @Test
    void findByIdAndDeletedFalse_returnsEmptyForDeletedCity() {
        CityEntity city = createCity("Beijing", "beijing");
        em.flush();

        city.markDeleted();
        em.merge(city);
        em.flush();
        em.clear();

        Optional<CityEntity> result = cityRepository.findByIdAndDeletedFalse(city.getId());

        assertFalse(result.isPresent());
    }

    @Test
    void findByIdAndDeletedFalse_returnsEmptyForNonExistentId() {
        Optional<CityEntity> result = cityRepository.findByIdAndDeletedFalse(UUID.randomUUID());

        assertFalse(result.isPresent());
    }

    private CityEntity createCity(String name, String slug) {
        CityEntity city = new CityEntity();
        city.setName(name);
        city.setSlug(slug);
        em.persist(city);
        return city;
    }
}
