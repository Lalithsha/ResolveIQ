package com.resolveiq.ticket.application.dto;

import jakarta.validation.constraints.NotBlank;

public record AddMessageRequest(
    @NotBlank(message = "Message content is required")
    String content,
    boolean isInternal
) {}
