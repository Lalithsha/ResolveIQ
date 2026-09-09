package com.resolveiq.analysis.application.dto;

import com.resolveiq.analysis.domain.model.evidence.PipelineStatus;

import java.time.Instant;
import java.util.UUID;

public class EvidenceDtos {

    public record EvidenceUploadRequest(
        String fileName,
        String mediaType,
        byte[] content,
        boolean consentGranted
    ) {}

    public record EvidenceConsentRequest(
        boolean consent
    ) {}

    public record EvidenceJobResponse(
        UUID id,
        UUID tenantId,
        UUID ticketId,
        UUID attachmentId,
        String fileName,
        String mediaType,
        long fileSizeBytes,
        PipelineStatus pipelineStatus,
        boolean consentGranted,
        String retentionClass,
        int attempt,
        String errorDetails,
        Instant createdAt,
        Instant updatedAt
    ) {}

    public record EvidenceArtifactResponse(
        UUID id,
        UUID jobId,
        UUID ticketId,
        String artifactType,
        String redactedObjectKey,
        Integer pageOrFrame,
        Double timestampSeconds,
        String checksumSha256,
        String sensitivityClass,
        String redactedContent,
        Instant createdAt
    ) {}

    public record EvidenceObservationResponse(
        UUID id,
        UUID jobId,
        UUID ticketId,
        String observationType,
        String codeOrKey,
        String summary,
        double confidence,
        String sourceCoordinates,
        Instant createdAt
    ) {}
}
