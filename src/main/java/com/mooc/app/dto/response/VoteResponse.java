package com.mooc.app.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public class VoteResponse extends BaseResponse {

    @JsonProperty("vote_type")
    private final String voteType;

    public VoteResponse(String requestId, String voteType) {
        super(requestId);
        this.voteType = voteType;
    }

    public String getVoteType() { return voteType; }
}
