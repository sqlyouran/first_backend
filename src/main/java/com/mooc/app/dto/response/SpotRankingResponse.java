package com.mooc.app.dto.response;

import java.util.List;

public class SpotRankingResponse extends BaseResponse {

    private final String type;
    private final List<SpotResponse> items;

    public SpotRankingResponse(String requestId, String type, List<SpotResponse> items) {
        super(requestId);
        this.type = type;
        this.items = items;
    }

    public String getType() { return type; }
    public List<SpotResponse> getItems() { return items; }
}
