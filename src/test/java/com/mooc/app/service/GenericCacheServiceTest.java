package com.mooc.app.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GenericCacheServiceTest {

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOps;

    private GenericCacheService cacheService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        cacheService = new GenericCacheService(redisTemplate, objectMapper);
    }

    @Test
    void get_cacheHit_returnsDeserializedValue() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("key1")).thenReturn("{\"name\":\"test\"}");

        TestDto result = cacheService.get("key1", new TypeReference<>() {});

        assertNotNull(result);
        assertEquals("test", result.name);
    }

    @Test
    void get_cacheMiss_returnsNull() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("key1")).thenReturn(null);

        TestDto result = cacheService.get("key1", new TypeReference<>() {});

        assertNull(result);
    }

    @Test
    void get_redisException_returnsNull() {
        when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("Redis down"));

        TestDto result = cacheService.get("key1", new TypeReference<>() {});

        assertNull(result);
    }

    @Test
    void put_writesSerializedValue() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        cacheService.put("key1", new TestDto("hello"), Duration.ofMinutes(5));

        verify(valueOps).set(eq("key1"), contains("hello"), eq(Duration.ofMinutes(5)));
    }

    @Test
    void put_redisException_doesNotThrow() {
        when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("Redis down"));

        assertDoesNotThrow(() -> cacheService.put("key1", "value", Duration.ofMinutes(5)));
    }

    @Test
    void evict_deletesMatchingKeys() {
        when(redisTemplate.keys("cache:posts:*")).thenReturn(Set.of("cache:posts:list:1", "cache:posts:detail:abc"));

        cacheService.evict("cache:posts:*");

        verify(redisTemplate).delete(Set.of("cache:posts:list:1", "cache:posts:detail:abc"));
    }

    @Test
    void evict_noMatchingKeys_doesNotCallDelete() {
        when(redisTemplate.keys("cache:posts:*")).thenReturn(Set.of());

        cacheService.evict("cache:posts:*");

        verify(redisTemplate, never()).delete(anyCollection());
    }

    @Test
    void evict_redisException_doesNotThrow() {
        when(redisTemplate.keys(anyString())).thenThrow(new RuntimeException("Redis down"));

        assertDoesNotThrow(() -> cacheService.evict("cache:posts:*"));
    }

    private static class TestDto {
        public String name;
        public TestDto() {}
        public TestDto(String name) { this.name = name; }
    }
}
