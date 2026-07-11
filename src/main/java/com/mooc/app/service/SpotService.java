package com.mooc.app.service;

import com.mooc.app.dto.response.SpotListResponse;
import com.mooc.app.dto.response.SpotRankingResponse;
import com.mooc.app.dto.response.SpotResponse;
import com.mooc.app.entity.SpotEntity;
import com.mooc.app.entity.SpotStatus;
import com.mooc.app.exception.SpotException;
import com.mooc.app.repository.SpotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class SpotService {

    private static final Logger log = LoggerFactory.getLogger(SpotService.class);
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_RANKING_TOP = 50;
    private static final Map<String, Sort> SORT_MAP = Map.of(
            "latest", Sort.by(Sort.Direction.DESC, "createdAt"),
            "rating", Sort.by(Sort.Direction.DESC, "rating"),
            "viewCount", Sort.by(Sort.Direction.DESC, "viewCount"),
            "bookmarkCount", Sort.by(Sort.Direction.DESC, "bookmarkCount")
    );
    private static final Map<String, String> RANKING_FIELD_MAP = Map.of(
            "rating", "rating",
            "heat", "viewCount",
            "bookmark", "bookmarkCount"
    );

    private final SpotRepository spotRepository;

    public SpotService(SpotRepository spotRepository) {
        this.spotRepository = spotRepository;
    }

    public SpotListResponse listSpots(int page, int size, UUID cityId, String citySlug, String sort, String requestId) {
        if (size > MAX_PAGE_SIZE) {
            throw new SpotException(HttpStatus.BAD_REQUEST, "validation_error",
                    "Page size must not exceed " + MAX_PAGE_SIZE);
        }

        Sort sortObj = SORT_MAP.get(sort);
        if (sortObj == null) {
            throw new SpotException(HttpStatus.BAD_REQUEST, "validation_error",
                    "Invalid sort option: " + sort);
        }

        int zeroBasedPage = Math.max(0, page - 1);
        PageRequest pageable = PageRequest.of(zeroBasedPage, size, sortObj);

        Page<SpotEntity> result;
        if (citySlug != null && !citySlug.isBlank()) {
            result = spotRepository.findByCitySlugAndDeletedFalse(citySlug, pageable);
        } else if (cityId != null) {
            result = spotRepository.findByCityIdAndDeletedFalse(cityId, pageable);
        } else {
            result = spotRepository.findByStatusAndDeletedFalse(SpotStatus.PUBLISHED, pageable);
        }

        List<SpotResponse> items = result.getContent().stream()
                .map(spot -> toSpotResponse(spot, requestId))
                .toList();

        return new SpotListResponse(requestId, items, result.getTotalElements(), page, size);
    }

    public SpotResponse getSpot(String idOrSlug, String requestId) {
        SpotEntity spot = findSpotByIdOrSlug(idOrSlug);

        if (spot.getStatus() != SpotStatus.PUBLISHED) {
            throw new SpotException(HttpStatus.NOT_FOUND, "not_found", "Spot not found");
        }

        return toSpotResponse(spot, requestId);
    }

    private SpotEntity findSpotByIdOrSlug(String idOrSlug) {
        // Try UUID first
        try {
            UUID id = UUID.fromString(idOrSlug);
            return spotRepository.findByIdAndDeletedFalse(id)
                    .orElseThrow(() -> new SpotException(HttpStatus.NOT_FOUND, "not_found", "Spot not found"));
        } catch (IllegalArgumentException e) {
            // Not a valid UUID, try slug
            return spotRepository.findBySlugAndDeletedFalse(idOrSlug)
                    .orElseThrow(() -> new SpotException(HttpStatus.NOT_FOUND, "not_found", "Spot not found"));
        }
    }

    public SpotRankingResponse getRanking(String type, int top, String requestId) {
        String field = RANKING_FIELD_MAP.get(type);
        if (field == null) {
            throw new SpotException(HttpStatus.BAD_REQUEST, "validation_error",
                    "Invalid ranking type: " + type + ". Valid types: rating, heat, bookmark");
        }

        if (top > MAX_RANKING_TOP) {
            throw new SpotException(HttpStatus.BAD_REQUEST, "validation_error",
                    "Top must not exceed " + MAX_RANKING_TOP);
        }

        PageRequest pageable = PageRequest.of(0, top, Sort.by(Sort.Direction.DESC, field));
        Page<SpotEntity> result = spotRepository.findByStatusAndDeletedFalse(SpotStatus.PUBLISHED, pageable);

        List<SpotResponse> items = result.getContent().stream()
                .map(spot -> toSpotResponse(spot, requestId))
                .toList();

        return new SpotRankingResponse(requestId, type, items);
    }

    private SpotResponse toSpotResponse(SpotEntity spot, String requestId) {
        return new SpotResponse(
                requestId,
                spot.getId().toString(),
                spot.getName(),
                spot.getNameZh(),
                spot.getSlug(),
                spot.getDescription(),
                spot.getDescriptionZh(),
                spot.getCoverImage(),
                spot.getGallery(),
                spot.getTags(),
                spot.getCityId() != null ? spot.getCityId().toString() : null,
                spot.getCityName(),
                spot.getStatus() != null ? spot.getStatus().name().toLowerCase() : null,
                spot.getRating() != null ? spot.getRating().toPlainString() : "0.0",
                spot.getViewCount(),
                spot.getBookmarkCount(),
                spot.getCreatedAt() != null ? spot.getCreatedAt().toString() : null,
                spot.getUpdatedAt() != null ? spot.getUpdatedAt().toString() : null,
                spot.getTicketPrice(),
                spot.getOpeningHours(),
                spot.getAddress()
        );
    }
}
