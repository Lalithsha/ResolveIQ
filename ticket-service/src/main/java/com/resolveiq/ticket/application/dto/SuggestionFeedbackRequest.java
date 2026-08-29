package com.resolveiq.ticket.application.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record SuggestionFeedbackRequest(
    @NotNull UUID suggestionId,
    @NotBlank(message = "Action is required")
    @Pattern(regexp = "ACCEPTED|EDITED|REJECTED", flags = Pattern.Flag.CASE_INSENSITIVE)
    String action, // ACCEPTED, EDITED, REJECTED, REGENERATED
    String rejectionReason,
    String editedContent,
    @Min(1) @Max(5) Integer rating
) {}
