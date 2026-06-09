package com.mooc.app.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitServiceTest {

    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        rateLimitService = new RateLimitService();
    }

    @Test
    void isRateLimited_expiredEntriesCleanup_doesNotThrow() throws InterruptedException {
        // First call adds an entry with a 1-second window
        boolean first = rateLimitService.isRateLimited("test-key", 5, 1);
        assertThat(first).isFalse();

        // Wait for the entry to expire
        Thread.sleep(1100);

        // Second call triggers cleanup of expired entry — should not throw UnsupportedOperationException
        boolean second = rateLimitService.isRateLimited("test-key", 5, 1);
        assertThat(second).isFalse();
    }

    @Test
    void isRateLimited_withinLimit_returnsFalse() {
        assertThat(rateLimitService.isRateLimited("key", 3, 60)).isFalse();
        assertThat(rateLimitService.isRateLimited("key", 3, 60)).isFalse();
        assertThat(rateLimitService.isRateLimited("key", 3, 60)).isFalse();
    }

    @Test
    void isRateLimited_exceedsLimit_returnsTrue() {
        rateLimitService.isRateLimited("key", 2, 60);
        rateLimitService.isRateLimited("key", 2, 60);

        assertThat(rateLimitService.isRateLimited("key", 2, 60)).isTrue();
    }
}
