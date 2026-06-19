package com.mooc.app.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateProfileRequest(
    @Pattern(regexp = "^[a-z0-9_]{3,20}$", message = "Username must be 3-20 lowercase letters, digits, or underscores")
    String username,

    @Size(max = 30, message = "Nickname must not exceed 30 characters")
    String nickname,

    @Size(max = 2048, message = "Avatar URL must not exceed 2048 characters")
    @JsonProperty("avatar_url")
    String avatarUrl,

    @Size(max = 500, message = "Bio must not exceed 500 characters")
    String bio,

    @JsonProperty("interest_tags")
    List<String> interestTags
) {}
