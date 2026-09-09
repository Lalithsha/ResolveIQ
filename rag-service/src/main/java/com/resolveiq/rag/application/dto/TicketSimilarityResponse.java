package com.resolveiq.rag.application.dto;

import java.util.List;
import java.util.UUID;

public record TicketSimilarityResponse(
    UUID sourceTicketId,
    List<SimilarityMatch> matches
) {
    public record SimilarityMatch(
        UUID ticketId,
        double similarityScore,
        boolean fingerprintMatched,
        boolean categoryMatched,
        String explanation
    ) {}
}
