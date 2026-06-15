package com.mooc.app.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mooc.app.dto.response.SpotRankingResponse;
import com.mooc.app.dto.response.SpotResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
public class RankingCacheService {

    private static final Logger log = LoggerFactory.getLogger(RankingCacheService.class);
    private static final String KEY_PREFIX = "spot:ranking:";
    private static final Duration TTL = Duration.ofMinutes(5);
    private static final int CACHE_TOP = 50;
    private static final TypeReference<List<SpotResponse>> LIST_TYPE = new TypeReference<>() {};

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final SpotService spotService;

    public RankingCacheService(StringRedisTemplate redisTemplate,
                               ObjectMapper objectMapper,
                               SpotService spotService) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.spotService = spotService;
    }

    public SpotRankingResponse getRanking(String type, int top, String requestId) {
        String key = KEY_PREFIX + type;

        try {
            String cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                List<SpotResponse> items = objectMapper.readValue(cached, LIST_TYPE);
                log.debug("Ranking cache hit for key={}", key);
                return new SpotRankingResponse(requestId, type,
                        items.subList(0, Math.min(top, items.size())));
            }
        } catch (Exception e) {
            log.warn("Failed to read ranking cache for key={}", key, e);
        }

        log.debug("Ranking cache miss for key={}, querying database", key);
        SpotRankingResponse dbResponse = spotService.getRanking(type, CACHE_TOP, requestId);
        List<SpotResponse> allItems = dbResponse.getItems();

        try {
            String json = objectMapper.writeValueAsString(allItems);
            redisTemplate.opsForValue().set(key, json, TTL);
        } catch (Exception e) {
            log.warn("Failed to store ranking cache for key={}", key, e);
        }

        return new SpotRankingResponse(requestId, type,
                allItems.subList(0, Math.min(top, allItems.size())));
    }

    public void evictRanking(String type) {
        String key = KEY_PREFIX + type;
        redisTemplate.delete(key);
        log.debug("Evicted ranking cache for key={}", key);
    }
}
