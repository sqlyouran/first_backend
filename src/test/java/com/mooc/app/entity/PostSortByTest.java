package com.mooc.app.entity;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class PostSortByTest {

    @Test
    void encodeCursor_latest_returnsPureTimestamp() {
        Instant now = Instant.parse("2026-06-12T08:00:00Z");
        String cursor = PostSortBy.LATEST.encodeCursor(5, now);
        assertEquals("2026-06-12T08:00:00Z", cursor);
    }

    @Test
    void encodeCursor_mostUpvoted_returnsCountColonTimestamp() {
        Instant now = Instant.parse("2026-06-12T08:00:00Z");
        String cursor = PostSortBy.MOST_UPVOTED.encodeCursor(5, now);
        assertEquals("5:2026-06-12T08:00:00Z", cursor);
    }

    @Test
    void encodeCursor_mostCommented_returnsCountColonTimestamp() {
        Instant now = Instant.parse("2026-06-12T08:00:00Z");
        String cursor = PostSortBy.MOST_COMMENTED.encodeCursor(10, now);
        assertEquals("10:2026-06-12T08:00:00Z", cursor);
    }

    @Test
    void decodeCursor_latest_returnsZeroCountWithTimestamp() {
        PostSortBy.CursorValue cv = PostSortBy.LATEST.decodeCursor("2026-06-12T08:00:00Z");
        assertEquals(0, cv.count());
        assertEquals(Instant.parse("2026-06-12T08:00:00Z"), cv.timestamp());
    }

    @Test
    void decodeCursor_mostUpvoted_returnsCountAndTimestamp() {
        PostSortBy.CursorValue cv = PostSortBy.MOST_UPVOTED.decodeCursor("5:2026-06-12T08:00:00Z");
        assertEquals(5, cv.count());
        assertEquals(Instant.parse("2026-06-12T08:00:00Z"), cv.timestamp());
    }

    @Test
    void decodeCursor_mostCommented_returnsCountAndTimestamp() {
        PostSortBy.CursorValue cv = PostSortBy.MOST_COMMENTED.decodeCursor("10:2026-06-12T08:00:00Z");
        assertEquals(10, cv.count());
        assertEquals(Instant.parse("2026-06-12T08:00:00Z"), cv.timestamp());
    }

    @Test
    void decodeCursor_null_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> PostSortBy.LATEST.decodeCursor(null));
    }

    @Test
    void decodeCursor_blank_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> PostSortBy.MOST_UPVOTED.decodeCursor(""));
    }

    @Test
    void decodeCursor_invalidFormat_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> PostSortBy.MOST_UPVOTED.decodeCursor("noColon"));
    }

    @Test
    void roundtrip_encodeThenDecode() {
        Instant ts = Instant.parse("2026-06-12T08:00:00Z");
        String encoded = PostSortBy.MOST_UPVOTED.encodeCursor(7, ts);
        PostSortBy.CursorValue decoded = PostSortBy.MOST_UPVOTED.decodeCursor(encoded);
        assertEquals(7, decoded.count());
        assertEquals(ts, decoded.timestamp());
    }

    @Test
    void fromString_null_defaultsToLatest() {
        assertEquals(PostSortBy.LATEST, PostSortBy.fromString(null));
    }

    @Test
    void fromString_invalid_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> PostSortBy.fromString("bogus"));
    }
}
