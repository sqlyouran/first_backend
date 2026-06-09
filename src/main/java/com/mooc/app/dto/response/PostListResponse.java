package com.mooc.app.dto.response;

import java.util.List;

public class PostListResponse extends BaseResponse {

    private final List<PostResponse> items;
    private final long total;
    private final int page;
    private final int size;

    public PostListResponse(String requestId, List<PostResponse> items, long total, int page, int size) {
        super(requestId);
        this.items = items;
        this.total = total;
        this.page = page;
        this.size = size;
    }

    public List<PostResponse> getItems() { return items; }
    public long getTotal() { return total; }
    public int getPage() { return page; }
    public int getSize() { return size; }
}
