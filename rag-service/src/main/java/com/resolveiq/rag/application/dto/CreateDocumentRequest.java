package com.resolveiq.rag.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateDocumentRequest(
    @NotBlank(message = "Title is required")
    @Size(max = 500, message = "Title cannot exceed 500 characters")
    String title,

    @NotBlank(message = "Category is required")
    String category,

    String product,
    String language,

    @NotBlank(message = "Initial content is required")
    String content,

    String summary
) {}
