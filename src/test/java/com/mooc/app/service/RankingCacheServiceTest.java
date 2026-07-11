package com.mooc.app.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mooc.app.dto.response.SpotRankingResponse;
import com.mooc.app.dto.response.SpotResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RankingCacheServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private SpotService spotService;

    private RankingCacheService rankingCacheService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        rankingCacheService = new RankingCacheService(redisTemplate, objectMapper, spotService);
    }

    @Test
    void getRanking_cacheHit_returnsCachedDataWithoutCallingSpotService() throws Exception {
        List<SpotResponse> cachedItems = generateSpotResponses(50, "cached-req");
        String cachedJson = objectMapper.writeValueAsString(cachedItems);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("spot:ranking:heat")).thenReturn(cachedJson);

        SpotRankingResponse response = rankingCacheService.getRanking("heat", 10, "current-req");

        assertEquals("current-req", response.getRequestId());
        assertEquals("heat", response.getType());
        assertEquals(10, response.getItems().size());
        assertEquals("spot-0", response.getItems().get(0).getId());
        verifyNoInteractions(spotService);
    }

    @Test
    void getRanking_cacheHit_usesCurrentRequestIdNotCached() throws Exception {
        List<SpotResponse> cachedItems = generateSpotResponses(5, "old-req-id");
        String cachedJson = objectMapper.writeValueAsString(cachedItems);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("spot:ranking:rating")).thenReturn(cachedJson);

        SpotRankingResponse response = rankingCacheService.getRanking("rating", 5, "new-req-id");

        assertEquals("new-req-id", response.getRequestId());
    }

    @Test
    void getRanking_cacheHit_topExceedsListSize_returnsAllCachedItems() throws Exception {
        List<SpotResponse> cachedItems = generateSpotResponses(3, "req");
        String cachedJson = objectMapper.writeValueAsString(cachedItems);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("spot:ranking:bookmark")).thenReturn(cachedJson);

        SpotRankingResponse response = rankingCacheService.getRanking("bookmark", 10, "req");

        assertEquals(3, response.getItems().size());
    }

    @Test
    void getRanking_cacheMiss_callsSpotServiceAndStoresInRedis() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("spot:ranking:heat")).thenReturn(null);

        List<SpotResponse> dbItems = generateSpotResponses(50, "req-789");
        SpotRankingResponse dbResponse = new SpotRankingResponse("req-789", "heat", dbItems);
        when(spotService.getRanking("heat", 50, "req-789")).thenReturn(dbResponse);

        SpotRankingResponse response = rankingCacheService.getRanking("heat", 10, "req-789");

        verify(spotService).getRanking("heat", 50, "req-789");
        assertEquals("req-789", response.getRequestId());
        assertEquals("heat", response.getType());
        assertEquals(10, response.getItems().size());

        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(eq("spot:ranking:heat"), jsonCaptor.capture(), eq(Duration.ofMinutes(5)));

        List<SpotResponse> stored = objectMapper.readValue(jsonCaptor.getValue(), new TypeReference<>() {});
        assertEquals(50, stored.size());
    }

    @Test
    void getRanking_cacheMiss_topEquals50_returnsAllItems() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("spot:ranking:rating")).thenReturn(null);

        List<SpotResponse> dbItems = generateSpotResponses(50, "req-001");
        SpotRankingResponse dbResponse = new SpotRankingResponse("req-001", "rating", dbItems);
        when(spotService.getRanking("rating", 50, "req-001")).thenReturn(dbResponse);

        SpotRankingResponse response = rankingCacheService.getRanking("rating", 50, "req-001");

        assertEquals(50, response.getItems().size());
    }

    @Test
    void evictRanking_deletesRedisKey() {
        when(redisTemplate.delete("spot:ranking:heat")).thenReturn(true);

        rankingCacheService.evictRanking("heat");

        verify(redisTemplate).delete("spot:ranking:heat");
    }

    @Test
    void evictRanking_nonExistingKey_doesNotThrow() {
        when(redisTemplate.delete("spot:ranking:nonexistent")).thenReturn(false);

        assertDoesNotThrow(() -> rankingCacheService.evictRanking("nonexistent"));
    }

    @Test
    void getRanking_usesCorrectKeyFormatForEachType() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        List<SpotResponse> dbItems = generateSpotResponses(5, "req");

        for (String type : List.of("rating", "heat", "bookmark")) {
            when(spotService.getRanking(eq(type), eq(50), anyString()))
                    .thenReturn(new SpotRankingResponse("req", type, dbItems));
            rankingCacheService.getRanking(type, 5, "req");
            verify(valueOperations).set(eq("spot:ranking:" + type), anyString(), eq(Duration.ofMinutes(5)));
        }
    }

    private List<SpotResponse> generateSpotResponses(int count, String requestId) {
        List<SpotResponse> items = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            items.add(new SpotResponse(
                    requestId, "spot-" + i, "Spot " + i, "景点 " + i,
                    "spot-" + i, "Desc", "描述",
                    "https://example.com/img.jpg", List.of(), List.of(),
                    "city-1", "Beijing", "published",
                    "4.5", 100 + i, 50 + i,
                    "2026-01-01T00:00:00Z", "2026-01-01T00:00:00Z",
                    null, null, null
            ));
        }
        return items;
    }
}
