package com.mooc.app.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateCommentRequest(
        @NotBlank(message = "Content must not be blank")
        @Size(max = 10000, message = "Content must not exceed 10000 characters")
        String content,

        @JsonProperty("parent_comment_id")
        UUID parentCommentId
) {}
