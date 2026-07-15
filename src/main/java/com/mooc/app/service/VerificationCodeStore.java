package com.mooc.app.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class VerificationCodeStore {

    private static final Logger log = LoggerFactory.getLogger(VerificationCodeStore.class);
    private static final String KEY_PREFIX = "verify:";

    private final StringRedisTemplate redisTemplate;

    // Fallback in-memory store when Redis is unavailable
    private record CodeEntry(String code, Instant expiresAt) {}
    private final Map<String, CodeEntry> fallbackStore = new ConcurrentHashMap<>();
    private boolean redisAvailable = true;

    public VerificationCodeStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void save(String email, String code, long ttlSeconds) {
        String key = KEY_PREFIX + email.toLowerCase();
        if (redisAvailable) {
            try {
                redisTemplate.opsForValue().set(key, code, Duration.ofSeconds(ttlSeconds));
                return;
            } catch (Exception e) {
                log.warn("Redis unavailable for verification code save, falling back to memory: {}", e.getMessage());
                redisAvailable = false;
            }
        }
        fallbackStore.put(email.toLowerCase(), new CodeEntry(code, Instant.now().plusSeconds(ttlSeconds)));
    }

    public Optional<String> getCode(String email) {
        String key = KEY_PREFIX + email.toLowerCase();
        if (redisAvailable) {
            try {
                String code = redisTemplate.opsForValue().get(key);
                return Optional.ofNullable(code);
            } catch (Exception e) {
                log.warn("Redis unavailable for verification code read, falling back to memory: {}", e.getMessage());
                redisAvailable = false;
            }
        }
        CodeEntry entry = fallbackStore.get(email.toLowerCase());
        if (entry == null) return Optional.empty();
        if (Instant.now().isAfter(entry.expiresAt())) {
            fallbackStore.remove(email.toLowerCase());
            return Optional.empty();
        }
        return Optional.of(entry.code());
    }

    public boolean isExpired(String email) {
        String key = KEY_PREFIX + email.toLowerCase();
        if (redisAvailable) {
            try {
                return Boolean.TRUE.equals(redisTemplate.hasKey(key)) ? false : true;
            } catch (Exception e) {
                log.warn("Redis unavailable for verification code expiry check, falling back to memory: {}", e.getMessage());
                redisAvailable = false;
            }
        }
        CodeEntry entry = fallbackStore.get(email.toLowerCase());
        if (entry == null) return true;
        return Instant.now().isAfter(entry.expiresAt());
    }

    public void remove(String email) {
        String key = KEY_PREFIX + email.toLowerCase();
        if (redisAvailable) {
            try {
                redisTemplate.delete(key);
                return;
            } catch (Exception e) {
                log.warn("Redis unavailable for verification code delete, falling back to memory: {}", e.getMessage());
                redisAvailable = false;
            }
        }
        fallbackStore.remove(email.toLowerCase());
    }
}
