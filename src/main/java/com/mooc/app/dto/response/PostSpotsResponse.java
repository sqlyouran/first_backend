package com.mooc.app.dto.response;

import java.util.List;

public class PostSpotsResponse extends BaseResponse {

    private final List<SpotResponse> items;

    public PostSpotsResponse(String requestId, List<SpotResponse> items) {
        super(requestId);
        this.items = items;
    }

    public List<SpotResponse> getItems() { return items; }
}
