package com.mooc.app.dto.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CityResponse extends BaseResponse {

    private final String id;
    private final String name;

    @JsonProperty("name_zh")
    private final String nameZh;

    private final String slug;

    @JsonProperty("cover_image")
    private final String coverImage;

    private final String description;

    @JsonProperty("best_season")
    private final String bestSeason;

    @JsonProperty("created_at")
    private final String createdAt;

    @JsonProperty("updated_at")
    private final String updatedAt;

    @JsonCreator
    public CityResponse(
            @JsonProperty("request_id") String requestId,
            @JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("name_zh") String nameZh,
            @JsonProperty("slug") String slug,
            @JsonProperty("cover_image") String coverImage,
            @JsonProperty("description") String description,
            @JsonProperty("best_season") String bestSeason,
            @JsonProperty("created_at") String createdAt,
            @JsonProperty("updated_at") String updatedAt) {
        super(requestId);
        this.id = id;
        this.name = name;
        this.nameZh = nameZh;
        this.slug = slug;
        this.coverImage = coverImage;
        this.description = description;
        this.bestSeason = bestSeason;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getNameZh() { return nameZh; }
    public String getSlug() { return slug; }
    public String getCoverImage() { return coverImage; }
    public String getDescription() { return description; }
    public String getBestSeason() { return bestSeason; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }
}
