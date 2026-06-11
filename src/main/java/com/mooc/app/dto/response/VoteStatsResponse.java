package com.mooc.app.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public class VoteStatsResponse extends BaseResponse {

    @JsonProperty("up_count")
    private final long upCount;

    @JsonProperty("down_count")
    private final long downCount;

    @JsonProperty("user_vote")
    private final String userVote;

    public VoteStatsResponse(String requestId, long upCount, long downCount, String userVote) {
        super(requestId);
        this.upCount = upCount;
        this.downCount = downCount;
        this.userVote = userVote;
    }

    public long getUpCount() { return upCount; }
    public long getDownCount() { return downCount; }
    public String getUserVote() { return userVote; }
}
