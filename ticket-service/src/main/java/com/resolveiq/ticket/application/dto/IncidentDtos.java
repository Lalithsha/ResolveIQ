package com.resolveiq.ticket.application.dto;

import com.resolveiq.ticket.domain.model.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class IncidentDtos {

    private IncidentDtos() {}

    public record CreateIncidentRequest(
        @NotBlank String title,
        IncidentSeverity severity,
        String summary,
        String initialComponent,
        List<UUID> initialTicketIds
    ) {}

    public record IncidentResponse(
        UUID id,
        String incidentNumber,
        String title,
        IncidentStatus status,
        IncidentSeverity severity,
        Instant detectedAt,
        Instant confirmedAt,
        Instant resolvedAt,
        UUID ownerUserId,
        String summary,
        String rootCause,
        String resolutionSummary,
        int linkedTicketCount,
        int affectedCustomerCount,
        List<ComponentDto> components,
        List<OperationalSignal> signals
    ) {
        public record ComponentDto(UUID id, String componentName, String status, String impactSummary) {}
    }

    public record IncidentProposalResponse(
        UUID clusterId,
        UUID incidentId,
        String dominantCategory,
        String product,
        int ticketCount,
        int baselineCount,
        double anomalyScore,
        List<SampleTicketDto> sampleTickets,
        List<String> errorFingerprints,
        ClusterStatus status,
        String explanation
    ) {
        public record SampleTicketDto(UUID ticketId, String ticketNumber, String subject, String customerName, double similarityScore) {}
    }

    public record CreateUpdateRequest(
        @NotBlank String title,
        @NotBlank String message,
        AudienceType audienceType
    ) {}

    public record IncidentUpdateResponse(
        UUID id,
        UUID incidentId,
        int updateNumber,
        IncidentUpdateStatus status,
        String title,
        String message,
        AudienceType audienceType,
        int audienceCount,
        UUID authorId,
        UUID approvedBy,
        Instant approvedAt,
        Instant publishedAt,
        String contentDigest
    ) {}

    public record TransitionIncidentRequest(
        @NotNull IncidentStatus status,
        String resolutionSummary
    ) {}

    public record CustomerIncidentResponse(
        UUID id,
        String incidentNumber,
        String title,
        IncidentStatus status,
        IncidentSeverity severity,
        String summary,
        String latestUpdate,
        Instant lastUpdatedAt
    ) {}

    public record SubscribeCustomerRequest(
        String channel
    ) {}

    public record DeliveryDetailDto(
        UUID id,
        UUID recipientCustomerId,
        String channel,
        DeliveryStatus status,
        int attemptCount,
        Instant deliveredAt,
        String failureReason
    ) {}

    public record DetectionRunResult(
        int evaluatedTickets,
        int proposalsCreated,
        int ticketsLinked,
        String algorithmVersion
    ) {}
}
