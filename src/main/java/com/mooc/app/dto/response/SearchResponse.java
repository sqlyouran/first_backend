package com.mooc.app.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class SearchResponse extends BaseResponse {

    private final List<SearchResultItem> items;

    @JsonProperty("spots_count")
    private final int spotsCount;

    @JsonProperty("posts_count")
    private final int postsCount;

    @JsonProperty("cities_count")
    private final int citiesCount;

    public SearchResponse(String requestId, List<SearchResultItem> items,
                          int spotsCount, int postsCount, int citiesCount) {
        super(requestId);
        this.items = items;
        this.spotsCount = spotsCount;
        this.postsCount = postsCount;
        this.citiesCount = citiesCount;
    }

    public List<SearchResultItem> getItems() { return items; }
    public int getSpotsCount() { return spotsCount; }
    public int getPostsCount() { return postsCount; }
    public int getCitiesCount() { return citiesCount; }
}
