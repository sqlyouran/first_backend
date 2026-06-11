package com.mooc.app.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record VoteRequest(
        @NotBlank(message = "Vote type must not be blank")
        @JsonProperty("vote_type")
        String voteType
) {}
