package com.mooc.app.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public record SearchResultItem(
        @JsonProperty("type") String type,
        @JsonProperty("id") UUID id,
        @JsonProperty("slug") String slug,
        @JsonProperty("name") String name,
        @JsonProperty("name_zh") String nameZh,
        @JsonProperty("summary") String summary,
        @JsonProperty("score") double score
) {}
