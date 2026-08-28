package com.resolveiq.ticket.application.dto;

import com.resolveiq.ticket.domain.model.AiSuggestion;
import com.resolveiq.ticket.domain.model.SuggestionStatus;

import java.time.Instant;
import java.util.UUID;

public record AiSuggestionResponse(
    UUID id,
    UUID ticketId,
    UUID tenantId,
    String suggestedResponse,
    Double confidenceScore,
    String modelName,
    String promptVersion,
    String citations,
    SuggestionStatus status,
    Instant createdAt,
    Instant reviewedAt,
    UUID reviewedByAgentId
) {
    public static AiSuggestionResponse fromEntity(AiSuggestion suggestion) {
        return new AiSuggestionResponse(
            suggestion.getId(),
            suggestion.getTicketId(),
            suggestion.getTenantId(),
            suggestion.getSuggestedResponse(),
            suggestion.getConfidenceScore(),
            suggestion.getModelName(),
            suggestion.getPromptVersion(),
            suggestion.getCitations(),
            suggestion.getStatus(),
            suggestion.getCreatedAt(),
            suggestion.getReviewedAt(),
            suggestion.getReviewedByAgentId()
        );
    }
}
