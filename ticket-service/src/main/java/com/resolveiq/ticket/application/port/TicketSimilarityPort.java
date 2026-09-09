package com.resolveiq.ticket.application.port;

import java.util.List;
import java.util.UUID;

public interface TicketSimilarityPort {
    record CandidateTicket(UUID ticketId, String text, String category, String product, String errorFingerprint) {}
    record SimilarityResult(UUID ticketId, double similarityScore, boolean fingerprintMatched, boolean categoryMatched, String explanation) {}

    List<SimilarityResult> findSimilarTickets(
        UUID tenantId,
        UUID ticketId,
        String queryText,
        String category,
        String product,
        String errorFingerprint,
        Double minSimilarity,
        List<CandidateTicket> candidates
    );
}
