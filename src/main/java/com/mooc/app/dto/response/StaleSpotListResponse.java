package com.mooc.app.dto.response;

import java.util.List;

public class StaleSpotListResponse extends BaseResponse {

    private final List<StaleSpotResponse> items;
    private final long total;

    public StaleSpotListResponse(String requestId, List<StaleSpotResponse> items, long total) {
        super(requestId);
        this.items = items;
        this.total = total;
    }

    public List<StaleSpotResponse> getItems() { return items; }
    public long getTotal() { return total; }
}
