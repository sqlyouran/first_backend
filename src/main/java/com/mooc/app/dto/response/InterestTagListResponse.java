package com.mooc.app.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class InterestTagListResponse extends BaseResponse {

    private final List<TagItem> tags;

    public InterestTagListResponse(String requestId, List<TagItem> tags) {
        super(requestId);
        this.tags = tags;
    }

    public List<TagItem> getTags() {
        return tags;
    }

    public static class TagItem {
        private final String value;
        private final String label;
        private final String category;

        public TagItem(String value, String label, String category) {
            this.value = value;
            this.label = label;
            this.category = category;
        }

        public String getValue() { return value; }
        public String getLabel() { return label; }
        public String getCategory() { return category; }
    }
}
