package com.mooc.app.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PostListResponse extends BaseResponse {

    private final List<PostResponse> items;
    private final long total;
    private final Integer page;
    private final int size;

    @JsonProperty("next_cursor")
    private final String nextCursor;

    @JsonProperty("has_more")
    private final boolean hasMore;

    public PostListResponse(String requestId, List<PostResponse> items, long total, Integer page, int size,
                            String nextCursor, boolean hasMore) {
        super(requestId);
        this.items = items;
        this.total = total;
        this.page = page;
        this.size = size;
        this.nextCursor = nextCursor;
        this.hasMore = hasMore;
    }

    public List<PostResponse> getItems() { return items; }
    public long getTotal() { return total; }
    public Integer getPage() { return page; }
    public int getSize() { return size; }
    public String getNextCursor() { return nextCursor; }
    public boolean isHasMore() { return hasMore; }
}
