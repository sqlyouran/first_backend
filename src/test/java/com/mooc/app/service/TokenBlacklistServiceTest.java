package com.mooc.app.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TokenBlacklistServiceTest {

    private TokenBlacklistService service;

    @BeforeEach
    void setUp() {
        service = new TokenBlacklistService();
    }

    @Test
    void emptyBlacklist_returnsFalse() {
        assertFalse(service.isBlacklisted("any-jti"));
    }

    @Test
    void addThenCheck_returnsTrue() {
        service.add("jti-123");
        assertTrue(service.isBlacklisted("jti-123"));
    }

    @Test
    void unaddedJti_returnsFalse() {
        service.add("jti-A");
        assertFalse(service.isBlacklisted("jti-B"));
    }

    @Test
    void addIsIdempotent() {
        assertDoesNotThrow(() -> {
            service.add("jti-X");
            service.add("jti-X");
        });
        assertTrue(service.isBlacklisted("jti-X"));
    }

    @Test
    void multipleJtisIndependent() {
        service.add("jti-1");
        service.add("jti-2");
        assertTrue(service.isBlacklisted("jti-1"));
        assertTrue(service.isBlacklisted("jti-2"));
    }
}
