package com.resolveiq.ticket.application.dto;

import com.resolveiq.ticket.domain.model.Ticket;
import com.resolveiq.ticket.domain.model.TicketPriority;
import com.resolveiq.ticket.domain.model.TicketStatus;

import java.time.Instant;
import java.util.UUID;

public record TicketResponse(
    UUID id,
    String ticketNumber,
    UUID tenantId,
    UUID customerId,
    UUID teamId,
    UUID assignedAgentId,
    String subject,
    String description,
    String language,
    TicketStatus status,
    TicketPriority priority,
    String category,
    String intent,
    String sentiment,
    String urgency,
    Double triageConfidence,
    String channel,
    UUID slaPolicyId,
    Instant firstResponseDueAt,
    Instant resolutionDueAt,
    String aiTriageStatus,
    UUID latestSuggestionId,
    Instant createdAt,
    Instant updatedAt,
    Instant resolvedAt,
    Instant closedAt,
    Long version
) {
    public static TicketResponse fromEntity(Ticket ticket) {
        return new TicketResponse(
            ticket.getId(),
            ticket.getTicketNumber(),
            ticket.getTenantId(),
            ticket.getCustomerId(),
            ticket.getTeamId(),
            ticket.getAssignedAgentId(),
            ticket.getSubject(),
            ticket.getDescription(),
            ticket.getLanguage(),
            ticket.getStatus(),
            ticket.getPriority(),
            ticket.getCategory(),
            ticket.getIntent(),
            ticket.getSentiment(),
            ticket.getUrgency(),
            ticket.getTriageConfidence(),
            ticket.getChannel(),
            ticket.getSlaPolicyId(),
            ticket.getFirstResponseDueAt(),
            ticket.getResolutionDueAt(),
            ticket.getAiTriageStatus(),
            ticket.getLatestSuggestionId(),
            ticket.getCreatedAt(),
            ticket.getUpdatedAt(),
            ticket.getResolvedAt(),
            ticket.getClosedAt(),
            ticket.getVersion()
        );
    }
}
