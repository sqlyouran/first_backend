package com.mooc.app.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class BookmarkListResponse extends BaseResponse {

    private final List<BookmarkItemResponse> items;
    private final long total;
    private final int page;
    private final int size;

    public BookmarkListResponse(String requestId, List<BookmarkItemResponse> items, long total, int page, int size) {
        super(requestId);
        this.items = items;
        this.total = total;
        this.page = page;
        this.size = size;
    }

    public List<BookmarkItemResponse> getItems() { return items; }
    public long getTotal() { return total; }
    public int getPage() { return page; }
    public int getSize() { return size; }

    public static class BookmarkItemResponse {

        @JsonProperty("bookmark_id")
        private final String bookmarkId;

        @JsonProperty("entity_id")
        private final String entityId;

        @JsonProperty("entity_type")
        private final String entityType;

        @JsonProperty("entity_title")
        private final String entityTitle;

        @JsonProperty("created_at")
        private final String createdAt;

        public BookmarkItemResponse(String bookmarkId, String entityId, String entityType, String entityTitle, String createdAt) {
            this.bookmarkId = bookmarkId;
            this.entityId = entityId;
            this.entityType = entityType;
            this.entityTitle = entityTitle;
            this.createdAt = createdAt;
        }

        public String getBookmarkId() { return bookmarkId; }
        public String getEntityId() { return entityId; }
        public String getEntityType() { return entityType; }
        public String getEntityTitle() { return entityTitle; }
        public String getCreatedAt() { return createdAt; }
    }
}
