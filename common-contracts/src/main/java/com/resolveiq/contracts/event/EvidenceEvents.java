package com.resolveiq.contracts.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class EvidenceEvents {

    private EvidenceEvents() {}

    public static final String EVIDENCE_PROCESSING_REQUESTED = "resolveiq.evidence.processing_requested";
    public static final String EVIDENCE_PROCESSING_COMPLETED = "resolveiq.evidence.processing_completed";

    public record EvidenceProcessingRequestedPayload(
        UUID jobId,
        UUID attachmentId,
        UUID ticketId,
        UUID tenantId,
        String mediaType,
        String quarantineStorageKey,
        long fileSizeBytes,
        boolean consentGranted,
        Instant requestedAt
    ) {}

    public record EvidenceProcessingCompletedPayload(
        UUID jobId,
        UUID attachmentId,
        UUID ticketId,
        UUID tenantId,
        String status, // READY, PARTIAL, BLOCKED_REDACTION, FAILED
        String redactedStorageKey,
        List<String> extractedObservations,
        List<String> errorCodes,
        Integer failureTimestampSeconds,
        Instant completedAt
    ) {}
}
