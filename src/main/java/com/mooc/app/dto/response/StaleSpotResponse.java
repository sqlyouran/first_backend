package com.mooc.app.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

public class StaleSpotResponse {

    private final UUID id;
    private final String name;
    @JsonProperty("name_zh")
    private final String nameZh;
    private final String slug;
    @JsonProperty("city_name")
    private final String cityName;
    @JsonProperty("data_refreshed_at")
    private final Instant dataRefreshedAt;
    @JsonProperty("days_since_refresh")
    private final long daysSinceRefresh;
    private final String priority;

    public StaleSpotResponse(UUID id, String name, String nameZh, String slug,
                             String cityName, Instant dataRefreshedAt,
                             long daysSinceRefresh, String priority) {
        this.id = id;
        this.name = name;
        this.nameZh = nameZh;
        this.slug = slug;
        this.cityName = cityName;
        this.dataRefreshedAt = dataRefreshedAt;
        this.daysSinceRefresh = daysSinceRefresh;
        this.priority = priority;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getNameZh() { return nameZh; }
    public String getSlug() { return slug; }
    public String getCityName() { return cityName; }
    public Instant getDataRefreshedAt() { return dataRefreshedAt; }
    public long getDaysSinceRefresh() { return daysSinceRefresh; }
    public String getPriority() { return priority; }
}
