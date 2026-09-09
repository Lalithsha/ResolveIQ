package com.resolveiq.rag.application.dto.flywheel;

import java.time.Instant;
import java.util.UUID;

public class FlywheelDtos {

    public record CreateCandidateRequest(
        String title,
        String contentDraft,
        String category,
        int verifiedOutcomeScore,
        int distinctSourceCustomers
    ) {}

    public record CandidateResponse(
        UUID id,
        String title,
        String contentDraft,
        String sanitizedContent,
        String category,
        String eligibilityStatus,
        int verifiedOutcomeScore,
        int distinctSourceCustomers,
        String sanitizationStatus,
        String contentHash,
        Instant createdAt
    ) {}

    public record EvaluationRunResponse(
        UUID id,
        UUID candidateId,
        String datasetVersion,
        double baselineRecallAt5,
        double proposedRecallAt5,
        double baselineMrr,
        double proposedMrr,
        boolean safetyCasesPassed,
        double latencyP95Ratio,
        boolean gatePassed,
        Instant createdAt
    ) {}

    public record KnowledgeReleaseResponse(
        UUID id,
        UUID candidateId,
        UUID documentId,
        int versionNumber,
        String status,
        UUID evaluationRunId,
        UUID approverId,
        String releaseNotes,
        Instant releasedAt
    ) {}

    public record RollbackRequest(
        UUID targetReleaseId,
        String reason
    ) {}

    public record RollbackResponse(
        UUID id,
        UUID releaseId,
        UUID targetReleaseId,
        String reason,
        UUID performedBy,
        Instant occurredAt
    ) {}
}
