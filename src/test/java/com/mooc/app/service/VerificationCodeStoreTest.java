package com.mooc.app.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VerificationCodeStoreTest {

    @Nested
    class RedisAvailable {
        private VerificationCodeStore store;
        private StringRedisTemplate mockRedis;
        private ValueOperations<String, String> mockOps;

        @BeforeEach
        void setUp() {
            mockRedis = mock(StringRedisTemplate.class);
            mockOps = mock(ValueOperations.class);
            when(mockRedis.opsForValue()).thenReturn(mockOps);
            store = new VerificationCodeStore(mockRedis);
        }

        @Test
        void save_storesToRedisWithTtl() {
            store.save("test@example.com", "123456", 600);
            verify(mockOps).set("verify:test@example.com", "123456", Duration.ofSeconds(600));
        }

        @Test
        void getCode_readsFromRedis() {
            when(mockOps.get("verify:test@example.com")).thenReturn("654321");
            assertEquals("654321", store.getCode("test@example.com").orElse(null));
        }

        @Test
        void getCode_redisReturnsNull_returnsEmpty() {
            when(mockOps.get("verify:test@example.com")).thenReturn(null);
            assertTrue(store.getCode("test@example.com").isEmpty());
        }

        @Test
        void remove_deletesFromRedis() {
            store.remove("test@example.com");
            verify(mockRedis).delete("verify:test@example.com");
        }

        @Test
        void isExpired_redisHasKey_returnsFalse() {
            when(mockRedis.hasKey("verify:test@example.com")).thenReturn(true);
            assertFalse(store.isExpired("test@example.com"));
        }

        @Test
        void isExpired_redisLacksKey_returnsTrue() {
            when(mockRedis.hasKey("verify:test@example.com")).thenReturn(false);
            assertTrue(store.isExpired("test@example.com"));
        }
    }

    @Nested
    class RedisUnavailable {

    private VerificationCodeStore store;

    @BeforeEach
    void setUp() {
        StringRedisTemplate mockRedis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> mockOps = mock(ValueOperations.class);
        when(mockRedis.opsForValue()).thenReturn(mockOps);
        // Make Redis always throw to trigger fallback to memory
        when(mockOps.get(anyString())).thenThrow(new RuntimeException("Redis unavailable"));
        doThrow(new RuntimeException("Redis unavailable")).when(mockOps).set(anyString(), anyString(), any());
        when(mockRedis.hasKey(anyString())).thenThrow(new RuntimeException("Redis unavailable"));
        doThrow(new RuntimeException("Redis unavailable")).when(mockRedis).delete(anyString());
        store = new VerificationCodeStore(mockRedis);
    }

        @Test
        void saveAndGetCode_returnsCode() {
        store.save("test@example.com", "123456", 600);
        assertEquals("123456", store.getCode("test@example.com").orElse(null));
    }

        @Test
        void emailCaseInsensitive() {
        store.save("Test@Example.COM", "111111", 600);
        assertEquals("111111", store.getCode("test@example.com").orElse(null));
    }

        @Test
        void unsavedEmail_returnsEmpty() {
        assertTrue(store.getCode("nonexistent@test.com").isEmpty());
    }

        @Test
        void expiredCode_returnsEmpty() {
        store.save("test@test.com", "999999", 0);
        assertTrue(store.getCode("test@test.com").isEmpty());
    }

        @Test
        void expiredCode_isExpiredTrue() {
        store.save("test@test.com", "888888", 0);
        assertTrue(store.isExpired("test@test.com"));
    }

        @Test
        void validCode_isExpiredFalse() {
        store.save("test@test.com", "777777", 600);
        assertFalse(store.isExpired("test@test.com"));
    }

        @Test
        void nonexistentEmail_isExpiredTrue() {
        assertTrue(store.isExpired("never-saved@test.com"));
    }

        @Test
        void remove_deletesCode() {
        store.save("test@test.com", "666666", 600);
        store.remove("test@test.com");
        assertTrue(store.getCode("test@test.com").isEmpty());
    }

        @Test
        void removeNonexistent_noException() {
        assertDoesNotThrow(() -> store.remove("nonexistent@test.com"));
    }

        @Test
        void saveOverwrites() {
            store.save("test@test.com", "111111", 600);
            store.save("test@test.com", "222222", 600);
            assertEquals("222222", store.getCode("test@test.com").orElse(null));
        }
    }
}
