package com.resolveiq.rag.application.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;

public record TicketSimilarityRequest(
    UUID ticketId,
    @NotBlank String queryText,
    String category,
    String product,
    String errorFingerprint,
    Double minSimilarity,
    List<CandidateTicket> candidates
) {
    public record CandidateTicket(
        UUID ticketId,
        String text,
        String category,
        String product,
        String errorFingerprint
    ) {}
}
