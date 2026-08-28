package com.resolveiq.rag.application.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;

public record ApproveResolvedCaseRequest(
    UUID originalTicketId,
    @NotBlank(message = "Sanitized subject is required")
    String sanitizedSubject,
    @NotBlank(message = "Sanitized description is required")
    String sanitizedDescription,
    @NotBlank(message = "Sanitized resolution is required")
    String sanitizedResolution,
    String category,
    List<String> tags
) {}
