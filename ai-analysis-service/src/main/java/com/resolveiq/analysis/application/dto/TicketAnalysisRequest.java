package com.resolveiq.analysis.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record TicketAnalysisRequest(
    @NotNull UUID ticketId,
    @NotNull UUID tenantId,
    @NotBlank(message = "Subject is required")
    String subject,
    @NotBlank(message = "Description is required")
    String description,
    String channel
) {}
