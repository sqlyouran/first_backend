package com.mooc.app.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VerificationCodeStoreTest {

    private VerificationCodeStore store;

    @BeforeEach
    void setUp() {
        store = new VerificationCodeStore();
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
