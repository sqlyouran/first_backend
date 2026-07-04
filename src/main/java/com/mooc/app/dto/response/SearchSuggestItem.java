package com.mooc.app.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SearchSuggestItem(
        @JsonProperty("type") String type,
        @JsonProperty("slug") String slug,
        @JsonProperty("name") String name,
        @JsonProperty("name_zh") String nameZh
) {}
