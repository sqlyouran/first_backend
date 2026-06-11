package com.mooc.app.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BookmarkResponse extends BaseResponse {

    @JsonProperty("bookmarked")
    private final boolean bookmarked;

    public BookmarkResponse(String requestId, boolean bookmarked) {
        super(requestId);
        this.bookmarked = bookmarked;
    }

    public boolean isBookmarked() { return bookmarked; }
}
