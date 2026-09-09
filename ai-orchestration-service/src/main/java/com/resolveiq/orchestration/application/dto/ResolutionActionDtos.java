package com.resolveiq.orchestration.application.dto;

import com.resolveiq.orchestration.domain.model.action.ActionStatus;
import com.resolveiq.orchestration.domain.model.action.ActionType;
import com.resolveiq.orchestration.domain.model.action.RiskLevel;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ResolutionActionDtos {

    private ResolutionActionDtos() {}

    public record ProposeActionRequest(
            ActionType actionType,
            Map<String, Object> input,
            String aiRationale,
            String evidenceIds
    ) {}

    public record ApproveActionRequest(
            String approvedDigest,
            String comment
    ) {}

    public record RejectActionRequest(
            String reason
    ) {}

    public record ExecuteActionRequest(
            String approvedDigest,
            Long expectedVersion,
            String idempotencyKey
    ) {}

    public record PolicyDecisionDto(
            String decision,
            List<String> matchedRules,
            List<String> requiredPermissions,
            int requiredApprovalCount,
            Long financialLimitCents,
            List<String> reasonCodes,
            Instant evaluatedAt
    ) {}

    public record ActionApprovalDto(
            UUID id,
            UUID actorId,
            String actorRole,
            String decision,
            String approvedDigest,
            Instant authenticationTime,
            String comment,
            Instant createdAt
    ) {}

    public record ActionReconciliationDto(
            UUID id,
            String status,
            Map<String, Object> expectedState,
            Map<String, Object> observedState,
            String notes,
            Instant reconciledAt
    ) {}

    public record ActionProposalResponse(
            UUID id,
            UUID tenantId,
            UUID ticketId,
            ActionType actionType,
            ActionStatus status,
            RiskLevel riskLevel,
            Map<String, Object> input,
            String inputHash,
            String canonicalDigest,
            String canonicalBytes,
            String aiRationale,
            String evidenceIds,
            PolicyDecisionDto policyDecision,
            List<ActionApprovalDto> approvals,
            Long version,
            Instant expiresAt,
            Instant createdAt,
            Instant updatedAt
    ) {}

    public record ActionExecutionResponse(
            UUID executionId,
            UUID proposalId,
            String status,
            String provider,
            String providerReference,
            Map<String, Object> sanitizedResponse,
            String errorMessage,
            ActionReconciliationDto reconciliation,
            Instant startedAt,
            Instant completedAt
    ) {}

    public record CompensationResponse(
            boolean isSupported,
            String status,
            String reason
    ) {}
}
