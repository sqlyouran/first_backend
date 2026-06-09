package com.mooc.app.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreatePostRequest(
        @NotBlank(message = "Title must not be blank")
        @Size(max = 200, message = "Title must not exceed 200 characters")
        String title,

        @NotBlank(message = "Content must not be blank")
        @Size(max = 50000, message = "Content must not exceed 50000 characters")
        String content,

        @Size(max = 2048, message = "Cover image URL must not exceed 2048 characters")
        @Pattern(regexp = "^(https?://.*)?$", message = "Cover image must be a valid URL")
        String coverImage,

        @Valid
        List<@Size(max = 30, message = "Each tag must not exceed 30 characters") String> tags,

        String status
) {
    public CreatePostRequest {
        if (tags != null && tags.size() > 10) {
            throw new IllegalArgumentException("Maximum 10 tags allowed");
        }
    }
}
