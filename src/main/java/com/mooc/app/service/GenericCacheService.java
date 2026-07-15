package com.mooc.app.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;

@Service
public class GenericCacheService {

    private static final Logger log = LoggerFactory.getLogger(GenericCacheService.class);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public GenericCacheService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Read cached value from Redis. Returns null on cache miss or any error.
     */
    public <T> T get(String key, TypeReference<T> typeRef) {
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json != null) {
                return objectMapper.readValue(json, typeRef);
            }
        } catch (Exception e) {
            log.warn("Cache read failed for key={}", key, e);
        }
        return null;
    }

    /**
     * Write value to Redis with TTL. Silently ignores errors.
     */
    public void put(String key, Object value, Duration ttl) {
        try {
            String json = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, json, ttl);
        } catch (Exception e) {
            log.warn("Cache write failed for key={}", key, e);
        }
    }

    /**
     * Delete all keys matching the given pattern.
     * Uses keys() which is acceptable for small key sets.
     */
    public void evict(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.debug("Evicted {} cache keys matching {}", keys.size(), pattern);
            }
        } catch (Exception e) {
            log.warn("Cache evict failed for pattern={}", pattern, e);
        }
    }
}
