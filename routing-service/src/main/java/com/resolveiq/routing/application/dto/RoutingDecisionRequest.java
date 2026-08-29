package com.resolveiq.routing.application.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record RoutingDecisionRequest(
    @NotNull(message = "Ticket ID is required")
    UUID ticketId,
    @NotNull(message = "Tenant ID is required") UUID tenantId,
    String category,
    String intent,
    String urgency,
    String priority,
    String language
) {}
