package com.resolveiq.rag.application.dto;

import jakarta.validation.constraints.NotBlank;

public record PublishVersionRequest(
    @NotBlank(message = "Content is required")
    String content,
    String summary
) {}
