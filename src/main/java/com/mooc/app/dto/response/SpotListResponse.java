package com.mooc.app.dto.response;

import java.util.List;

public class SpotListResponse extends BaseResponse {

    private final List<SpotResponse> items;
    private final long total;
    private final int page;
    private final int size;

    public SpotListResponse(String requestId, List<SpotResponse> items, long total, int page, int size) {
        super(requestId);
        this.items = items;
        this.total = total;
        this.page = page;
        this.size = size;
    }

    public List<SpotResponse> getItems() { return items; }
    public long getTotal() { return total; }
    public int getPage() { return page; }
    public int getSize() { return size; }
}
