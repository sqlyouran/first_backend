package com.mooc.app.service;

import com.mooc.app.dto.response.CityListResponse;
import com.mooc.app.dto.response.CityResponse;
import com.mooc.app.entity.CityEntity;
import com.mooc.app.exception.CityException;
import com.mooc.app.repository.CityRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CityServiceTest {

    @Mock
    private CityRepository cityRepository;

    @Mock
    private GenericCacheService cacheService;

    @InjectMocks
    private CityService cityService;

    @Test
    void listCities_convertsOneBasedPageToZeroBased() {
        Page<CityEntity> page = new PageImpl<>(List.of(createCity("Beijing", "beijing")));
        when(cityRepository.findByDeletedFalse(any(PageRequest.class))).thenReturn(page);

        CityListResponse response = cityService.listCities(1, 20, "req-id");

        verify(cityRepository).findByDeletedFalse(PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "name")));
        assertEquals(1, response.getPage());
        assertEquals(20, response.getSize());
    }

    @Test
    void listCities_sizeExceedsMax_throwsCityException() {
        CityException ex = assertThrows(CityException.class,
                () -> cityService.listCities(1, 200, "req-id"));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("validation_error", ex.getErrorCode());
    }

    @Test
    void listCities_returnsSortedByname() {
        List<CityEntity> cities = List.of(
                createCity("Beijing", "beijing"),
                createCity("Chengdu", "chengdu"),
                createCity("Shanghai", "shanghai")
        );
        Page<CityEntity> page = new PageImpl<>(cities);
        when(cityRepository.findByDeletedFalse(any(PageRequest.class))).thenReturn(page);

        CityListResponse response = cityService.listCities(1, 20, "req-id");

        assertEquals(3, response.getItems().size());
        assertEquals("Beijing", response.getItems().get(0).getName());
        assertEquals("Chengdu", response.getItems().get(1).getName());
        assertEquals("Shanghai", response.getItems().get(2).getName());
        assertEquals(3, response.getTotal());
    }

    @Test
    void getCity_exists_returnsCityResponse() {
        UUID id = UUID.randomUUID();
        CityEntity city = createCity("Beijing", "beijing");
        city.setId(id);
        when(cityRepository.findByIdAndDeletedFalse(id)).thenReturn(Optional.of(city));

        CityResponse response = cityService.getCity(id, "req-id");

        assertEquals("Beijing", response.getName());
        assertEquals("beijing", response.getSlug());
        assertEquals("req-id", response.getRequestId());
    }

    @Test
    void getCity_notFound_throwsCityException() {
        UUID id = UUID.randomUUID();
        when(cityRepository.findByIdAndDeletedFalse(id)).thenReturn(Optional.empty());

        CityException ex = assertThrows(CityException.class,
                () -> cityService.getCity(id, "req-id"));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertEquals("not_found", ex.getErrorCode());
    }

    private CityEntity createCity(String name, String slug) {
        CityEntity city = new CityEntity();
        city.setId(UUID.randomUUID());
        city.setName(name);
        city.setSlug(slug);
        city.setCoverImage("https://example.com/" + slug + ".jpg");
        city.setDescription("Description of " + name);
        city.setBestSeason("Autumn");
        // BaseEntity fields - set via reflection or manually for test
        return city;
    }
}
