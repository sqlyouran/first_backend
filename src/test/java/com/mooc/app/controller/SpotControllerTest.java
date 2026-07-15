package com.mooc.app.controller;

import com.mooc.app.config.SecurityConfig;
import com.mooc.app.dto.response.PostResponse;
import com.mooc.app.dto.response.SpotListResponse;
import com.mooc.app.dto.response.SpotPostsResponse;
import com.mooc.app.dto.response.SpotRankingResponse;
import com.mooc.app.dto.response.SpotResponse;
import com.mooc.app.exception.SpotException;
import com.mooc.app.service.JwtService;
import com.mooc.app.service.RankingCacheService;
import com.mooc.app.service.SpotPostService;
import com.mooc.app.service.SpotService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SpotController.class)
@Import(SecurityConfig.class)
class SpotControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private SpotService spotService;

    @MockBean
    private SpotPostService spotPostService;

    @MockBean
    private RankingCacheService rankingCacheService;

    private SpotResponse sampleSpot() {
        return new SpotResponse("req", "id", "Forbidden City", "故宫", "forbidden-city",
                "Description", "描述", "https://example.com/img.jpg",
                List.of("https://example.com/g1.jpg"), List.of("heritage"),
                "city-id", "Beijing", "published",
                "4.8", 1000, 200,
                "2026-01-01T00:00:00Z", "2026-01-01T00:00:00Z",
                null, null, null);
    }

    @Test
    void listSpots_defaultParams_delegatesToService() throws Exception {
        SpotListResponse listResponse = new SpotListResponse("req", List.of(sampleSpot()), 1, 1, 20);
        when(spotService.listSpots(eq(1), eq(20), isNull(), isNull(), eq("latest"), anyString())).thenReturn(listResponse);

        mockMvc.perform(get("/api/spots"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(20));

        verify(spotService).listSpots(eq(1), eq(20), isNull(), isNull(), eq("latest"), anyString());
    }

    @Test
    void listSpots_withCityId_passesThrough() throws Exception {
        UUID cityId = UUID.randomUUID();
        SpotListResponse listResponse = new SpotListResponse("req", List.of(sampleSpot()), 1, 1, 20);
        when(spotService.listSpots(eq(1), eq(20), eq(cityId), isNull(), eq("latest"), anyString())).thenReturn(listResponse);

        mockMvc.perform(get("/api/spots?city_id=" + cityId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1));

        verify(spotService).listSpots(eq(1), eq(20), eq(cityId), isNull(), eq("latest"), anyString());
    }

    @Test
    void listSpots_withSort_passesThrough() throws Exception {
        SpotListResponse listResponse = new SpotListResponse("req", List.of(sampleSpot()), 1, 1, 20);
        when(spotService.listSpots(eq(1), eq(20), isNull(), isNull(), eq("rating"), anyString())).thenReturn(listResponse);

        mockMvc.perform(get("/api/spots?sort=rating"))
                .andExpect(status().isOk());

        verify(spotService).listSpots(eq(1), eq(20), isNull(), isNull(), eq("rating"), anyString());
    }

    @Test
    void getSpot_exists_returnsSpot() throws Exception {
        UUID id = UUID.randomUUID();
        when(spotService.getSpot(eq(id.toString()), anyString())).thenReturn(sampleSpot());

        mockMvc.perform(get("/api/spots/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Forbidden City"))
                .andExpect(jsonPath("$.slug").value("forbidden-city"));
    }

    @Test
    void getSpot_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(spotService.getSpot(eq(id.toString()), anyString()))
                .thenThrow(new SpotException(HttpStatus.NOT_FOUND, "not_found", "Spot not found"));

        mockMvc.perform(get("/api/spots/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error_code").value("not_found"));
    }

    @Test
    void getRanking_byRating_delegatesToCacheService() throws Exception {
        SpotRankingResponse rankingResponse = new SpotRankingResponse("req", "rating", List.of(sampleSpot()));
        when(rankingCacheService.getRanking(eq("rating"), eq(10), anyString())).thenReturn(rankingResponse);

        mockMvc.perform(get("/api/spots/ranking?type=rating"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("rating"))
                .andExpect(jsonPath("$.items").isArray());

        verify(rankingCacheService).getRanking(eq("rating"), eq(10), anyString());
        verifyNoInteractions(spotService);
    }

    @Test
    void getRanking_byHeat_passesThrough() throws Exception {
        SpotRankingResponse rankingResponse = new SpotRankingResponse("req", "heat", List.of(sampleSpot()));
        when(rankingCacheService.getRanking(eq("heat"), eq(10), anyString())).thenReturn(rankingResponse);

        mockMvc.perform(get("/api/spots/ranking?type=heat"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("heat"));

        verify(rankingCacheService).getRanking(eq("heat"), eq(10), anyString());
    }

    @Test
    void getRanking_invalidType_returns400() throws Exception {
        when(rankingCacheService.getRanking(eq("invalid"), eq(10), anyString()))
                .thenThrow(new SpotException(HttpStatus.BAD_REQUEST, "validation_error", "Invalid ranking type"));

        mockMvc.perform(get("/api/spots/ranking?type=invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("validation_error"));
    }

    @Test
    void getRanking_topExceedsMax_returns400() throws Exception {
        when(rankingCacheService.getRanking(eq("rating"), eq(100), anyString()))
                .thenThrow(new SpotException(HttpStatus.BAD_REQUEST, "validation_error", "Top must not exceed 50"));

        mockMvc.perform(get("/api/spots/ranking?type=rating&top=100"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("validation_error"));
    }

    @Test
    void getSpotPosts_exists_returnsPostList() throws Exception {
        UUID spotId = UUID.randomUUID();
        PostResponse post = new PostResponse("req", "post-1", "Title", "title-post-1", "Content", null,
                List.of("tag"), "published", "author-1", "author1", "2026-01-01T00:00:00Z", "2026-01-01T00:00:00Z", 0, 0, 0);
        SpotPostsResponse response = new SpotPostsResponse("req", List.of(post), 1, 1, 20);
        when(spotPostService.getPostsBySpotId(eq(spotId), eq(1), eq(20), anyString())).thenReturn(response);

        mockMvc.perform(get("/api/spots/" + spotId + "/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(20));
    }

    @Test
    void getSpotPosts_empty_returnsEmptyList() throws Exception {
        UUID spotId = UUID.randomUUID();
        SpotPostsResponse response = new SpotPostsResponse("req", List.of(), 0, 1, 20);
        when(spotPostService.getPostsBySpotId(eq(spotId), eq(1), eq(20), anyString())).thenReturn(response);

        mockMvc.perform(get("/api/spots/" + spotId + "/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void getSpotPosts_spotNotFound_returns404() throws Exception {
        UUID spotId = UUID.randomUUID();
        when(spotPostService.getPostsBySpotId(eq(spotId), eq(1), eq(20), anyString()))
                .thenThrow(new SpotException(HttpStatus.NOT_FOUND, "not_found", "Spot not found"));

        mockMvc.perform(get("/api/spots/" + spotId + "/posts"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error_code").value("not_found"));
    }

    @Test
    void getSpotPosts_sizeExceedsMax_returns400() throws Exception {
        UUID spotId = UUID.randomUUID();
        when(spotPostService.getPostsBySpotId(eq(spotId), eq(1), eq(200), anyString()))
                .thenThrow(new SpotException(HttpStatus.BAD_REQUEST, "validation_error", "Page size must not exceed 100"));

        mockMvc.perform(get("/api/spots/" + spotId + "/posts?size=200"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("validation_error"));
    }
}
