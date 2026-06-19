package com.mooc.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateConversationRequest(
    @NotBlank(message = "Recipient username is required")
    @Size(max = 20, message = "Recipient username must be at most 20 characters")
    String recipient_username,

    @NotBlank(message = "Content is required")
    @Size(min = 1, max = 2000, message = "Content must be between 1 and 2000 characters")
    String content
) {}
