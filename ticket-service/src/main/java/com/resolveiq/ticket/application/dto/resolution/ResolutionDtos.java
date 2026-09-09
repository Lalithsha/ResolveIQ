package com.resolveiq.ticket.application.dto.resolution;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class ResolutionDtos {

    public record ResolutionOutcomeRequest(
        String rating, // YES, PARTLY, NO
        String reason
    ) {}

    public record ResolutionReopenRequest(
        String reason
    ) {}

    public record OutcomeItemResponse(
        UUID id,
        String source,
        String rating,
        String reason,
        Instant occurredAt,
        int weight
    ) {}

    public record ResolutionAttemptResponse(
        UUID id,
        UUID ticketId,
        int attemptNumber,
        UUID resolverId,
        Instant resolvedAt,
        Instant confirmationWindowExpiresAt,
        Instant scheduledClosureAt,
        String status,
        int score,
        String scoreFormulaVersion,
        List<OutcomeItemResponse> outcomes
    ) {}

    public record ResolutionMetricsResponse(
        long eligibleAttempts,
        long respondedAttempts,
        double feedbackCoverage,
        double verifiedSuccessRate,
        double firstContactResolutionRate,
        int averageScore
    ) {}
}
