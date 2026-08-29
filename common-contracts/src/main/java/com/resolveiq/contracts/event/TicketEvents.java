package com.resolveiq.contracts.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Event payloads for Ticket and Workflow domain events.
 */
public final class TicketEvents {

    private TicketEvents() {}

    public static final String TICKET_CREATED = "resolveiq.ticket.created";
    public static final String TICKET_MESSAGE_ADDED = "resolveiq.ticket.message_added";
    public static final String TICKET_TRIAGE_COMPLETED = "resolveiq.ticket.triage_completed";
    public static final String TICKET_TRIAGE_FAILED = "resolveiq.ticket.triage_failed";
    public static final String TICKET_ASSIGNED = "resolveiq.ticket.assigned";
    public static final String TICKET_RESOLVED = "resolveiq.ticket.resolved";
    public static final String RESOLVED_CASE_APPROVED = "resolveiq.ticket.resolved_case_approved";
    public static final String AGENT_FEEDBACK_RECORDED = "resolveiq.ticket.agent_feedback_recorded";

    public record TicketCreatedPayload(
        UUID ticketId,
        String ticketNumber,
        UUID tenantId,
        UUID customerId,
        String subject,
        String description,
        String channel,
        String priority,
        String category,
        Instant createdAt
    ) {}

    public record TicketMessageAddedPayload(
        UUID ticketId,
        UUID messageId,
        UUID senderId,
        String senderRole,
        String content,
        Instant sentAt
    ) {}

    public record TicketTriageCompletedPayload(
        UUID ticketId,
        String intent,
        String category,
        String sentiment,
        String urgency,
        Double confidence,
        UUID assignedTeamId,
        UUID assignedAgentId,
        UUID slaPolicyId,
        Instant firstResponseDueAt,
        Instant resolutionDueAt,
        UUID suggestionId,
        String suggestedResponse,
        String modelName,
        String promptVersion,
        String citationsJson,
        Instant triageCompletedAt
    ) {}

    public record TicketTriageFailedPayload(
        UUID ticketId,
        String failureReason,
        String errorCode,
        boolean retryable,
        Instant failedAt
    ) {}

    public record TicketAssignedPayload(
        UUID ticketId,
        UUID teamId,
        UUID agentId,
        String assignmentReason,
        Instant assignedAt
    ) {}

    public record TicketResolvedPayload(
        UUID ticketId,
        String resolutionSummary,
        UUID resolvedByAgentId,
        Instant resolvedAt
    ) {}

    public record ResolvedCaseApprovedPayload(
        UUID ticketId,
        String sanitizedSubject,
        String sanitizedDescription,
        String sanitizedResolution,
        List<String> tags,
        UUID approvedByUserId,
        Instant approvedAt
    ) {}

    public record AgentFeedbackRecordedPayload(
        UUID suggestionId,
        UUID ticketId,
        UUID agentId,
        String feedbackAction, // ACCEPTED, EDITED, REJECTED, REGENERATED
        String rejectionReason,
        Integer rating,
        Instant recordedAt
    ) {}
}
