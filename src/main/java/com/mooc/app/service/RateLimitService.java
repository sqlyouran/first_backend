package com.mooc.app.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {

    private final Map<String, List<Instant>> windows = new ConcurrentHashMap<>();

    /**
     * Check if the action is allowed and record it.
     * @return true if rate limit exceeded (blocked), false if allowed
     */
    public boolean isRateLimited(String key, int limit, long windowSeconds) {
        Instant now = Instant.now();
        Instant windowStart = now.minusSeconds(windowSeconds);

        List<Instant> timestamps = windows.computeIfAbsent(key, k -> new ArrayList<>());

        // Clean expired entries
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
}
