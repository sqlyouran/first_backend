package com.mooc.app.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {

    private static final Logger log = LoggerFactory.getLogger(RateLimitService.class);

    private final StringRedisTemplate redisTemplate;

    // Fallback in-memory store when Redis is unavailable
    private final Map<String, List<Instant>> fallbackWindows = new ConcurrentHashMap<>();
    private boolean redisAvailable = true;

    public RateLimitService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Check if the action is allowed and record it.
     * @return true if rate limit exceeded (blocked), false if allowed
     */
    public boolean isRateLimited(String key, int limit, long windowSeconds) {
        if (redisAvailable) {
            try {
                return isRateLimitedRedis(key, limit, windowSeconds);
            } catch (Exception e) {
                log.warn("Redis unavailable for rate limiting, falling back to memory: {}", e.getMessage());
                redisAvailable = false;
            }
        }
        return isRateLimitedMemory(key, limit, windowSeconds);
    }

    private boolean isRateLimitedRedis(String key, int limit, long windowSeconds) {
        Instant now = Instant.now();
        double nowEpoch = now.toEpochMilli();
        double windowStart = now.minusSeconds(windowSeconds).toEpochMilli();
        String redisKey = "ratelimit:" + key;

        ZSetOperations<String, String> zOps = redisTemplate.opsForZSet();
        // Remove expired entries
        zOps.removeRangeByScore(redisKey, 0, windowStart);
        // Count current entries
        Long count = zOps.zCard(redisKey);
        if (count != null && count >= limit) {
            return true;
        }
        // Add new entry with unique member
        zOps.add(redisKey, nowEpoch + ":" + Math.random(), nowEpoch);
        // Set expiry on the key to auto-clean
        redisTemplate.expire(redisKey, Duration.ofSeconds(windowSeconds));
        return false;
    }

    private boolean isRateLimitedMemory(String key, int limit, long windowSeconds) {
        Instant now = Instant.now();
        Instant windowStart = now.minusSeconds(windowSeconds);

        List<Instant> timestamps = fallbackWindows.computeIfAbsent(key, k -> new ArrayList<>());
        timestamps.removeIf(t -> t.isBefore(windowStart));

        if (timestamps.size() >= limit) {
            return true;
        }
        timestamps.add(now);
        return false;
    }

    // Predefined rate limit checks
    public boolean isLoginRateLimited(String ip) {
        return isRateLimited("login:ip:" + ip, 20, 3600);
    }

    public boolean isSendCodeIpRateLimited(String ip) {
        return isRateLimited("sendcode:ip:" + ip, 5, 60);
    }

    public boolean isSendCodeEmailRateLimited(String email) {
        return isRateLimited("sendcode:email:" + email.toLowerCase(), 5, 86400);
    }

    public boolean isRegisterRateLimited(String ip) {
        return isRateLimited("register:ip:" + ip, 3, 3600);
    }

    /**
     * AI chat rate limiting: per IP per day, 20 requests max.
     */
    public boolean isAiChatIpRateLimited(String ip) {
        return isRateLimited("aichat:ip:" + ip, 20, 86400);
    }
}
