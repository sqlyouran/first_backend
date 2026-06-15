package com.mooc.app.service;

import com.mooc.app.dto.response.CityListResponse;
import com.mooc.app.dto.response.CityResponse;
import com.mooc.app.entity.CityEntity;
import com.mooc.app.exception.CityException;
import com.mooc.app.repository.CityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class CityService {

    private static final Logger log = LoggerFactory.getLogger(CityService.class);
    private static final int MAX_PAGE_SIZE = 100;

    private final CityRepository cityRepository;

    public CityService(CityRepository cityRepository) {
        this.cityRepository = cityRepository;
    }

    public CityListResponse listCities(int page, int size, String requestId) {
        if (size > MAX_PAGE_SIZE) {
            throw new CityException(HttpStatus.BAD_REQUEST, "validation_error",
                    "Page size must not exceed " + MAX_PAGE_SIZE);
        }

        int zeroBasedPage = Math.max(0, page - 1);
        PageRequest pageable = PageRequest.of(zeroBasedPage, size, Sort.by(Sort.Direction.ASC, "name"));
        Page<CityEntity> result = cityRepository.findByDeletedFalse(pageable);

        List<CityResponse> items = result.getContent().stream()
                .map(city -> toCityResponse(city, requestId))
                .toList();

        return new CityListResponse(requestId, items, result.getTotalElements(), page, size);
    }

    public CityResponse getCity(UUID id, String requestId) {
        CityEntity city = cityRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new CityException(HttpStatus.NOT_FOUND, "not_found", "City not found"));

        return toCityResponse(city, requestId);
    }

    private CityResponse toCityResponse(CityEntity city, String requestId) {
        return new CityResponse(
                requestId,
                city.getId().toString(),
                city.getName(),
                city.getNameZh(),
                city.getSlug(),
                city.getCoverImage(),
                city.getDescription(),
                city.getBestSeason(),
                city.getCreatedAt() != null ? city.getCreatedAt().toString() : null,
                city.getUpdatedAt() != null ? city.getUpdatedAt().toString() : null
        );
    }
}
