package com.resolveiq.ticket.application.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record SuggestionFeedbackRequest(
    UUID suggestionId,
    @NotBlank(message = "Action is required")
    String action, // ACCEPTED, EDITED, REJECTED, REGENERATED
    String rejectionReason,
    String editedContent,
    Integer rating
) {}
