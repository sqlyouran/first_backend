package com.mooc.app.service;

import com.mooc.app.dto.response.SpotListResponse;
import com.mooc.app.dto.response.SpotRankingResponse;
import com.mooc.app.dto.response.SpotResponse;
import com.mooc.app.entity.SpotEntity;
import com.mooc.app.entity.SpotStatus;
import com.mooc.app.exception.SpotException;
import com.mooc.app.repository.SpotRepository;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpotServiceTest {

    @Mock
    private SpotRepository spotRepository;

    @InjectMocks
    private SpotService spotService;

    @Test
    void listSpots_defaultParams_convertsOneBasedPageToZeroBased() {
        Page<SpotEntity> page = new PageImpl<>(List.of(createSpot("Forbidden City", "forbidden-city")));
        when(spotRepository.findByStatusAndDeletedFalse(eq(SpotStatus.PUBLISHED), any(PageRequest.class))).thenReturn(page);

        SpotListResponse response = spotService.listSpots(1, 20, null, null, "latest", "req-id");

        verify(spotRepository).findByStatusAndDeletedFalse(eq(SpotStatus.PUBLISHED),
                eq(PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"))));
        assertEquals(1, response.getPage());
        assertEquals(20, response.getSize());
    }

    @Test
    void listSpots_withCityId_usesCityFilter() {
        UUID cityId = UUID.randomUUID();
        Page<SpotEntity> page = new PageImpl<>(List.of(createSpot("Forbidden City", "forbidden-city")));
        when(spotRepository.findByCityIdAndDeletedFalse(eq(cityId), any(PageRequest.class))).thenReturn(page);

        SpotListResponse response = spotService.listSpots(1, 20, cityId, null, "latest", "req-id");

        verify(spotRepository).findByCityIdAndDeletedFalse(eq(cityId),
                eq(PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"))));
        assertEquals(1, response.getTotal());
    }

    @Test
    void listSpots_sortByRating_usesDescOrder() {
        Page<SpotEntity> page = new PageImpl<>(List.of(createSpot("High", "high")));
        when(spotRepository.findByStatusAndDeletedFalse(eq(SpotStatus.PUBLISHED), any(PageRequest.class))).thenReturn(page);

        spotService.listSpots(1, 20, null, null, "rating", "req-id");

        verify(spotRepository).findByStatusAndDeletedFalse(eq(SpotStatus.PUBLISHED),
                eq(PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "rating"))));
    }

    @Test
    void listSpots_sizeExceedsMax_throwsSpotException() {
        SpotException ex = assertThrows(SpotException.class,
                () -> spotService.listSpots(1, 200, null, null, "latest", "req-id"));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("validation_error", ex.getErrorCode());
    }

    @Test
    void listSpots_invalidSort_throwsSpotException() {
        SpotException ex = assertThrows(SpotException.class,
                () -> spotService.listSpots(1, 20, null, null, "invalid", "req-id"));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("validation_error", ex.getErrorCode());
    }

    @Test
    void getSpot_exists_returnsSpotResponse() {
        UUID id = UUID.randomUUID();
        SpotEntity spot = createSpot("Forbidden City", "forbidden-city");
        spot.setId(id);
        when(spotRepository.findByIdAndDeletedFalse(id)).thenReturn(Optional.of(spot));

        SpotResponse response = spotService.getSpot(id.toString(), "req-id");

        assertEquals("Forbidden City", response.getName());
        assertEquals("forbidden-city", response.getSlug());
        assertEquals("req-id", response.getRequestId());
    }

    @Test
    void getSpot_notFound_throwsSpotException() {
        UUID id = UUID.randomUUID();
        when(spotRepository.findByIdAndDeletedFalse(id)).thenReturn(Optional.empty());

        SpotException ex = assertThrows(SpotException.class,
                () -> spotService.getSpot(id.toString(), "req-id"));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertEquals("not_found", ex.getErrorCode());
    }

    @Test
    void getSpot_draftStatus_throwsSpotException() {
        UUID id = UUID.randomUUID();
        SpotEntity draft = createSpot("Draft", "draft");
        draft.setStatus(SpotStatus.DRAFT);
        draft.setId(id);
        when(spotRepository.findByIdAndDeletedFalse(id)).thenReturn(Optional.of(draft));

        SpotException ex = assertThrows(SpotException.class,
                () -> spotService.getSpot(id.toString(), "req-id"));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void getRanking_byRating_returnsSortedByRating() {
        List<SpotEntity> spots = List.of(
                createSpotWithRating("High", "high", "4.9"),
                createSpotWithRating("Low", "low", "3.0")
        );
        when(spotRepository.findByStatusAndDeletedFalse(eq(SpotStatus.PUBLISHED), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(spots));

        SpotRankingResponse response = spotService.getRanking("rating", 10, "req-id");

        assertEquals("rating", response.getType());
        assertEquals(2, response.getItems().size());
        verify(spotRepository).findByStatusAndDeletedFalse(eq(SpotStatus.PUBLISHED),
                eq(PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "rating"))));
    }

    @Test
    void getRanking_byHeat_sortsByViewCount() {
        when(spotRepository.findByStatusAndDeletedFalse(eq(SpotStatus.PUBLISHED), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(createSpot("Hot", "hot"))));

        spotService.getRanking("heat", 10, "req-id");

        verify(spotRepository).findByStatusAndDeletedFalse(eq(SpotStatus.PUBLISHED),
                eq(PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "viewCount"))));
    }

    @Test
    void getRanking_byBookmark_sortsByBookmarkCount() {
        when(spotRepository.findByStatusAndDeletedFalse(eq(SpotStatus.PUBLISHED), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(createSpot("Bookmarked", "bookmarked"))));

        spotService.getRanking("bookmark", 10, "req-id");

        verify(spotRepository).findByStatusAndDeletedFalse(eq(SpotStatus.PUBLISHED),
                eq(PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "bookmarkCount"))));
    }

    @Test
    void getRanking_invalidType_throwsSpotException() {
        SpotException ex = assertThrows(SpotException.class,
                () -> spotService.getRanking("invalid", 10, "req-id"));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("validation_error", ex.getErrorCode());
    }

    @Test
    void getRanking_topExceedsMax_throwsSpotException() {
        SpotException ex = assertThrows(SpotException.class,
                () -> spotService.getRanking("rating", 100, "req-id"));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("validation_error", ex.getErrorCode());
    }

    private SpotEntity createSpot(String name, String slug) {
        SpotEntity spot = new SpotEntity();
        spot.setId(UUID.randomUUID());
        spot.setName(name);
        spot.setSlug(slug);
        spot.setCityId(UUID.randomUUID());
        spot.setCityName("City");
        spot.setStatus(SpotStatus.PUBLISHED);
        spot.setRating(new BigDecimal("4.0"));
        spot.setViewCount(100);
        spot.setBookmarkCount(50);
        return spot;
    }

    private SpotEntity createSpotWithRating(String name, String slug, String rating) {
        SpotEntity spot = createSpot(name, slug);
        spot.setRating(new BigDecimal(rating));
        return spot;
    }
}
