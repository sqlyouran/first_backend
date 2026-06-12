package com.mooc.app.entity;

import java.time.Instant;

public enum PostSortBy {
    LATEST,
    MOST_UPVOTED,
    MOST_COMMENTED;

    public record CursorValue(long count, Instant timestamp) {}

    public static PostSortBy fromString(String value) {
        if (value == null || value.isBlank()) {
            return LATEST;
        }
        return switch (value.toLowerCase()) {
            case "latest" -> LATEST;
            case "most_upvoted" -> MOST_UPVOTED;
            case "most_commented" -> MOST_COMMENTED;
            default -> throw new IllegalArgumentException("Invalid sort value: " + value);
        };
    }

    public String encodeCursor(long count, Instant createdAt) {
        return switch (this) {
            case LATEST -> createdAt.toString();
            case MOST_UPVOTED, MOST_COMMENTED -> count + ":" + createdAt.toString();
        };
    }

    public CursorValue decodeCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            throw new IllegalArgumentException("Cursor must not be blank");
        }
        return switch (this) {
            case LATEST -> {
                yield new CursorValue(0, Instant.parse(cursor));
            }
            case MOST_UPVOTED, MOST_COMMENTED -> {
                int idx = cursor.indexOf(':');
                if (idx <= 0 || idx >= cursor.length() - 1) {
                    throw new IllegalArgumentException("Invalid composite cursor: " + cursor);
                }
                long count = Long.parseLong(cursor.substring(0, idx));
                Instant ts = Instant.parse(cursor.substring(idx + 1));
                yield new CursorValue(count, ts);
            }
        };
    }
}
