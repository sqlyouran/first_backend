package com.mooc.app.dto.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class SpotResponse extends BaseResponse {

    private final String id;
    private final String name;

    @JsonProperty("name_zh")
    private final String nameZh;

    private final String slug;
    private final String description;

    @JsonProperty("description_zh")
    private final String descriptionZh;

    @JsonProperty("cover_image")
    private final String coverImage;

    private final List<String> gallery;
    private final List<String> tags;

    @JsonProperty("city_id")
    private final String cityId;

    @JsonProperty("city_name")
    private final String cityName;

    private final String status;
    private final String rating;

    @JsonProperty("view_count")
    private final int viewCount;

    @JsonProperty("bookmark_count")
    private final int bookmarkCount;

    @JsonProperty("created_at")
    private final String createdAt;

    @JsonProperty("updated_at")
    private final String updatedAt;

    @JsonCreator
    public SpotResponse(
            @JsonProperty("request_id") String requestId,
            @JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("name_zh") String nameZh,
            @JsonProperty("slug") String slug,
            @JsonProperty("description") String description,
            @JsonProperty("description_zh") String descriptionZh,
            @JsonProperty("cover_image") String coverImage,
            @JsonProperty("gallery") List<String> gallery,
            @JsonProperty("tags") List<String> tags,
            @JsonProperty("city_id") String cityId,
            @JsonProperty("city_name") String cityName,
            @JsonProperty("status") String status,
            @JsonProperty("rating") String rating,
            @JsonProperty("view_count") int viewCount,
            @JsonProperty("bookmark_count") int bookmarkCount,
            @JsonProperty("created_at") String createdAt,
            @JsonProperty("updated_at") String updatedAt) {
        super(requestId);
        this.id = id;
        this.name = name;
        this.nameZh = nameZh;
        this.slug = slug;
        this.description = description;
        this.descriptionZh = descriptionZh;
        this.coverImage = coverImage;
        this.gallery = gallery;
        this.tags = tags;
        this.cityId = cityId;
        this.cityName = cityName;
        this.status = status;
        this.rating = rating;
        this.viewCount = viewCount;
        this.bookmarkCount = bookmarkCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getNameZh() { return nameZh; }
    public String getSlug() { return slug; }
    public String getDescription() { return description; }
    public String getDescriptionZh() { return descriptionZh; }
    public String getCoverImage() { return coverImage; }
    public List<String> getGallery() { return gallery; }
    public List<String> getTags() { return tags; }
    public String getCityId() { return cityId; }
    public String getCityName() { return cityName; }
    public String getStatus() { return status; }
    public String getRating() { return rating; }
    public int getViewCount() { return viewCount; }
    public int getBookmarkCount() { return bookmarkCount; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }
}
