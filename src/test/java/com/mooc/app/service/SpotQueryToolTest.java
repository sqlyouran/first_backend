package com.mooc.app.service;

import com.mooc.app.dto.response.SpotRankingResponse;
import com.mooc.app.dto.response.SpotResponse;
import com.mooc.app.entity.SpotEntity;
import com.mooc.app.entity.SpotStatus;
import com.mooc.app.repository.SpotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpotQueryToolTest {

    @Mock private SpotRepository spotRepository;
    @Mock private SpotService spotService;

    private SpotQueryTool spotQueryTool;

    @BeforeEach
    void setUp() {
        spotQueryTool = new SpotQueryTool(spotRepository, spotService);
    }

    // === searchSpotsByCity ===

    @Test
    void searchSpotsByCity_returnsFormattedList() {
        SpotEntity spot = createSpot("West Lake", "西湖", "west-lake", "Hangzhou",
                new BigDecimal("4.8"), List.of("nature", "scenic"));
        Page<SpotEntity> page = new PageImpl<>(List.of(spot));
        when(spotRepository.findByCitySlugAndDeletedFalse(eq("Hangzhou"), any(Pageable.class)))
                .thenReturn(page);

        String result = spotQueryTool.searchSpotsByCity("Hangzhou");

        assertTrue(result.contains("West Lake"));
        assertTrue(result.contains("西湖"));
        assertTrue(result.contains("4.8"));
        assertTrue(result.contains("nature"));
    }

    @Test
    void searchSpotsByCity_noResults_returnsNotFoundMessage() {
        Page<SpotEntity> emptyPage = new PageImpl<>(List.of());
        when(spotRepository.findByCitySlugAndDeletedFalse(eq("NonExistent"), any(Pageable.class)))
                .thenReturn(emptyPage);

        String result = spotQueryTool.searchSpotsByCity("NonExistent");

        assertTrue(result.contains("No spots found"));
    }

    // === getSpotDetails ===

    @Test
    void getSpotDetails_returnsFullInfo() {
        SpotEntity spot = createSpot("Lingyin Temple", "灵隐寺", "lingyin-temple", "Hangzhou",
                new BigDecimal("4.5"), List.of("culture", "heritage"));
        spot.setDescription("One of the largest Buddhist temples in China");
        spot.setDescriptionZh("中国最大的佛教寺庙之一");
        spot.setGallery(List.of("https://picsum.photos/1"));
        when(spotRepository.findBySlugAndDeletedFalse("lingyin-temple"))
                .thenReturn(Optional.of(spot));

        String result = spotQueryTool.getSpotDetails("lingyin-temple");

        assertTrue(result.contains("Lingyin Temple"));
        assertTrue(result.contains("灵隐寺"));
        assertTrue(result.contains("largest Buddhist temples"));
        assertTrue(result.contains("culture"));
        assertTrue(result.contains("4.5"));
        assertTrue(result.contains("Hangzhou"));
    }

    @Test
    void getSpotDetails_notFound_returnsNotFoundMessage() {
        when(spotRepository.findBySlugAndDeletedFalse("non-existent"))
                .thenReturn(Optional.empty());

        String result = spotQueryTool.getSpotDetails("non-existent");

        assertTrue(result.contains("Spot not found"));
    }

    // === getTopRatedSpots ===

    @Test
    void getTopRatedSpots_returnsFormattedList() {
        SpotResponse spot = new SpotResponse("req", "id-1", "Forbidden City", "故宫",
                "forbidden-city", "desc", "desc-zh", null, null, null,
                null, "Beijing", "published", "4.9", 1000, 500, null, null);
        SpotRankingResponse ranking = new SpotRankingResponse("req", "rating", List.of(spot));
        when(spotService.getRanking(eq("rating"), eq(5), any())).thenReturn(ranking);

        String result = spotQueryTool.getTopRatedSpots(5);

        assertTrue(result.contains("Forbidden City"));
        assertTrue(result.contains("故宫"));
        assertTrue(result.contains("4.9"));
    }

    @Test
    void getTopRatedSpots_limitExceeds10_truncatesTo10() {
        SpotRankingResponse ranking = new SpotRankingResponse("req", "rating", List.of());
        when(spotService.getRanking(eq("rating"), eq(10), any())).thenReturn(ranking);

        spotQueryTool.getTopRatedSpots(20);

        verify(spotService).getRanking(eq("rating"), eq(10), any());
    }

    @Test
    void getTopRatedSpots_emptyResults_returnsMessage() {
        SpotRankingResponse ranking = new SpotRankingResponse("req", "rating", List.of());
        when(spotService.getRanking(eq("rating"), eq(5), any())).thenReturn(ranking);

        String result = spotQueryTool.getTopRatedSpots(5);

        assertTrue(result.contains("No top rated spots"));
    }

    private SpotEntity createSpot(String name, String nameZh, String slug, String cityName,
                                  BigDecimal rating, List<String> tags) {
        SpotEntity spot = new SpotEntity();
        spot.setId(UUID.randomUUID());
        spot.setName(name);
        spot.setNameZh(nameZh);
        spot.setSlug(slug);
        spot.setCityName(cityName);
        spot.setCityId(UUID.randomUUID());
        spot.setRating(rating);
        spot.setTags(tags);
        spot.setStatus(SpotStatus.PUBLISHED);
        return spot;
    }
}
