package com.resolveiq.contracts.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class IncidentEvents {

    private IncidentEvents() {}

    public static final String INCIDENT_PROPOSED = "resolveiq.incident.proposed";
    public static final String INCIDENT_UPDATED = "resolveiq.incident.updated";

    public record IncidentProposedPayload(
        UUID clusterId,
        UUID tenantId,
        String dominantCategory,
        String product,
        int ticketCount,
        double anomalyScore,
        List<UUID> ticketIds,
        List<String> errorFingerprints,
        String explanation,
        String algorithmVersion,
        Instant proposedAt
    ) {}

    public record IncidentUpdatedPayload(
        UUID incidentId,
        String incidentNumber,
        UUID tenantId,
        String title,
        String status,
        String severity,
        UUID updateId,
        String updateStatus,
        String updateSummary,
        int affectedCustomerCount,
        UUID authorId,
        Instant occurredAt
    ) {}
}
