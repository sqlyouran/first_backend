package com.mooc.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AiPostAssistRequest(
        @NotBlank(message = "Action must not be blank")
        @Pattern(regexp = "generate_title|recommend_tags|polish", message = "Invalid action")
        String action,

        @NotBlank(message = "Content must not be blank")
        @Size(max = 50000, message = "Content must not exceed 50000 characters")
        String content,

        @Size(max = 200, message = "Title must not exceed 200 characters")
        String title
) {}
