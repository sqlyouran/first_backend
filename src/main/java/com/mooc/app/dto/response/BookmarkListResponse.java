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

        @JsonProperty("post_id")
        private final String postId;

        @JsonProperty("post_title")
        private final String postTitle;

        @JsonProperty("created_at")
        private final String createdAt;

        public BookmarkItemResponse(String bookmarkId, String postId, String postTitle, String createdAt) {
            this.bookmarkId = bookmarkId;
            this.postId = postId;
            this.postTitle = postTitle;
            this.createdAt = createdAt;
        }

        public String getBookmarkId() { return bookmarkId; }
        public String getPostId() { return postId; }
        public String getPostTitle() { return postTitle; }
        public String getCreatedAt() { return createdAt; }
    }
}
