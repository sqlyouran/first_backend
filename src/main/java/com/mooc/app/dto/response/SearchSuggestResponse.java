package com.mooc.app.dto.response;

import java.util.List;

public class SearchSuggestResponse extends BaseResponse {

    private final List<SearchSuggestItem> items;

    public SearchSuggestResponse(String requestId, List<SearchSuggestItem> items) {
        super(requestId);
        this.items = items;
    }

    public List<SearchSuggestItem> getItems() { return items; }
}
