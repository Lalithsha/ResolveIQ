package com.resolveiq.rag.application.dto;

import jakarta.validation.constraints.Size;

public record ReviewDecisionRequest(
    @Size(max = 2000, message = "Review note cannot exceed 2000 characters") String note
) {}
