package com.resolveiq.contracts.event;

import java.time.Instant;
import java.util.UUID;

public final class ActionEvents {

    private ActionEvents() {}

    public static final String ACTION_EXECUTION_REQUESTED = "resolveiq.action.execution_requested";
    public static final String ACTION_EXECUTION_COMPLETED = "resolveiq.action.execution_completed";

    public record ActionExecutionRequestedPayload(
        UUID proposalId,
        UUID ticketId,
        UUID tenantId,
        String actionType,
        String approvedDigest,
        String providerIdempotencyKey,
        UUID requestedByUserId,
        Instant requestedAt
    ) {}

    public record ActionExecutionCompletedPayload(
        UUID executionId,
        UUID proposalId,
        UUID ticketId,
        UUID tenantId,
        String actionType,
        String status, // SUCCEEDED, FAILED_RETRYABLE, FAILED_FINAL, EXECUTION_UNKNOWN
        String providerReference,
        String reconciliationStatus, // RECONCILED, MANUAL_REVIEW
        String failureReason,
        Instant completedAt
    ) {}
}
