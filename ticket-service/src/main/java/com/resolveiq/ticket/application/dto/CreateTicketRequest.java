package com.resolveiq.ticket.application.dto;

import com.resolveiq.ticket.domain.model.TicketPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTicketRequest(
    @NotBlank(message = "Subject is required")
    @Size(max = 500, message = "Subject cannot exceed 500 characters")
    String subject,

    @NotBlank(message = "Description is required")
    String description,

    String category,
    TicketPriority priority,
    String channel,
    String language
) {}
