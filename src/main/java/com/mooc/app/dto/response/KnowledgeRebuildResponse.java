package com.mooc.app.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public class KnowledgeRebuildResponse extends BaseResponse {

    @JsonProperty("status")
    private final String status;

    public KnowledgeRebuildResponse(String requestId, String status) {
        super(requestId);
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}
