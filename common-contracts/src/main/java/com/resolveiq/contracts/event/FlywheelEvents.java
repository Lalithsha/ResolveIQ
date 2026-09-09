package com.resolveiq.contracts.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class FlywheelEvents {

    private FlywheelEvents() {}

    public static final String RESOLUTION_OUTCOME_RECORDED = "resolveiq.resolution.outcome_recorded";
    public static final String KNOWLEDGE_CANDIDATE_CREATED = "resolveiq.knowledge.candidate_created";

    public record ResolutionOutcomeRecordedPayload(
        UUID outcomeId,
        UUID resolutionId,
        UUID ticketId,
        UUID tenantId,
        String source, // CUSTOMER, REOPEN, REPEAT_CONTACT, AGENT_CORRECTION
        String rating, // YES, PARTLY, NO, NO_RESPONSE
        String feedbackReason,
        int scoreDelta,
        int totalScore,
        Instant occurredAt
    ) {}

    public record KnowledgeCandidateCreatedPayload(
        UUID candidateId,
        UUID tenantId,
        String topic,
        String proposedTitle,
        int verifiedSourceCount,
        double averageScore,
        List<UUID> sourceTicketIds,
        Instant createdAt
    ) {}
}
