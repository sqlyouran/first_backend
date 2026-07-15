package com.mooc.app.service;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RateLimitServiceTest {

    @Nested
    class RedisAvailable {
        @Test
        void isRateLimited_recordsToRedisSortedSet() {
            StringRedisTemplate mockRedis = mock(StringRedisTemplate.class);
            ZSetOperations<String, String> mockZOps = mock(ZSetOperations.class);
            when(mockRedis.opsForZSet()).thenReturn(mockZOps);
            when(mockZOps.zCard(anyString())).thenReturn(0L);
            when(mockZOps.add(anyString(), anyString(), anyDouble())).thenReturn(true);

            RateLimitService service = new RateLimitService(mockRedis);
            boolean result = service.isRateLimited("test-key", 5, 60);

            assertThat(result).isFalse();
            verify(mockZOps).removeRangeByScore(eq("ratelimit:test-key"), eq(0.0), anyDouble());
            verify(mockZOps).zCard("ratelimit:test-key");
            verify(mockZOps).add(eq("ratelimit:test-key"), anyString(), anyDouble());
            verify(mockRedis).expire(eq("ratelimit:test-key"), eq(Duration.ofSeconds(60)));
        }

        @Test
        void isRateLimited_redisSaysOverLimit_returnsTrue() {
            StringRedisTemplate mockRedis = mock(StringRedisTemplate.class);
            ZSetOperations<String, String> mockZOps = mock(ZSetOperations.class);
            when(mockRedis.opsForZSet()).thenReturn(mockZOps);
            when(mockZOps.zCard("ratelimit:test-key")).thenReturn(5L);

            RateLimitService service = new RateLimitService(mockRedis);
            boolean result = service.isRateLimited("test-key", 5, 60);

            assertThat(result).isTrue();
            verify(mockZOps, never()).add(anyString(), anyString(), anyDouble());
        }
    }

    @Nested
    class RedisUnavailable {

        private RateLimitService createService() {
            StringRedisTemplate mockRedis = mock(StringRedisTemplate.class);
            ZSetOperations<String, String> mockZOps = mock(ZSetOperations.class);
            when(mockRedis.opsForZSet()).thenReturn(mockZOps);
            when(mockZOps.zCard(anyString())).thenThrow(new RuntimeException("Redis unavailable"));
            return new RateLimitService(mockRedis);
        }

        @Test
        void isRateLimited_expiredEntriesCleanup_doesNotThrow() throws InterruptedException {
            RateLimitService service = createService();
            boolean first = service.isRateLimited("test-key", 5, 1);
            assertThat(first).isFalse();

            Thread.sleep(1100);

            boolean second = service.isRateLimited("test-key", 5, 1);
            assertThat(second).isFalse();
        }

        @Test
        void isRateLimited_withinLimit_returnsFalse() {
            RateLimitService service = createService();
            assertThat(service.isRateLimited("key", 3, 60)).isFalse();
            assertThat(service.isRateLimited("key", 3, 60)).isFalse();
            assertThat(service.isRateLimited("key", 3, 60)).isFalse();
        }

        @Test
        void isRateLimited_exceedsLimit_returnsTrue() {
            RateLimitService service = createService();
            service.isRateLimited("key", 2, 60);
            service.isRateLimited("key", 2, 60);

            assertThat(service.isRateLimited("key", 2, 60)).isTrue();
        }
    }
}
