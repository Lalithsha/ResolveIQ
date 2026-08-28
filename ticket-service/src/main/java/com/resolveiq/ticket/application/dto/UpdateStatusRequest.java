package com.resolveiq.ticket.application.dto;

import com.resolveiq.ticket.domain.model.TicketStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(
    @NotNull(message = "Target status is required")
    TicketStatus status,
    String reason
) {}
