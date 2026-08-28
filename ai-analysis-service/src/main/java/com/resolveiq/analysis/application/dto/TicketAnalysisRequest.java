package com.resolveiq.analysis.application.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record TicketAnalysisRequest(
    UUID ticketId,
    UUID tenantId,
    @NotBlank(message = "Subject is required")
    String subject,
    @NotBlank(message = "Description is required")
    String description,
    String channel
) {}
