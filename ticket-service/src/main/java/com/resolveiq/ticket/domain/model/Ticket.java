package com.resolveiq.ticket.domain.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tickets", schema = "ticket_schema")
public class Ticket {

    @Id
    private UUID id;

    @Column(name = "ticket_number", nullable = false, unique = true)
    private String ticketNumber;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "team_id")
    private UUID teamId;

    @Column(name = "assigned_agent_id")
    private UUID assignedAgentId;

    @Column(nullable = false, length = 500)
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 10)
    private String language;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TicketStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TicketPriority priority;

    @Column(length = 100)
    private String category;

    @Column(nullable = false, length = 50)
    private String channel;

    @Column(name = "sla_policy_id")
    private UUID slaPolicyId;

    @Column(name = "first_response_due_at")
    private Instant firstResponseDueAt;

    @Column(name = "resolution_due_at")
    private Instant resolutionDueAt;

    @Column(name = "ai_triage_status", nullable = false, length = 50)
    private String aiTriageStatus;

    @Column(name = "latest_suggestion_id")
    private UUID latestSuggestionId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Version
    private Long version;

    public Ticket() {}

    public Ticket(
        UUID id,
        String ticketNumber,
        UUID tenantId,
        UUID customerId,
        String subject,
        String description,
        String category,
        TicketPriority priority,
        String channel,
        String language
    ) {
        this.id = id != null ? id : UUID.randomUUID();
        this.ticketNumber = ticketNumber;
        this.tenantId = tenantId;
        this.customerId = customerId;
        this.subject = subject;
        this.description = description;
        this.category = category;
        this.priority = priority != null ? priority : TicketPriority.MEDIUM;
        this.channel = channel != null ? channel : "WEB";
        this.language = language != null ? language : "en";
        this.status = TicketStatus.NEW;
        this.aiTriageStatus = "PENDING";
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getTicketNumber() { return ticketNumber; }
    public UUID getTenantId() { return tenantId; }
    public UUID getCustomerId() { return customerId; }
    public UUID getTeamId() { return teamId; }
    public UUID getAssignedAgentId() { return assignedAgentId; }
    public String getSubject() { return subject; }
    public String getDescription() { return description; }
    public String getLanguage() { return language; }
    public TicketStatus getStatus() { return status; }
    public TicketPriority getPriority() { return priority; }
    public String getCategory() { return category; }
    public String getChannel() { return channel; }
    public UUID getSlaPolicyId() { return slaPolicyId; }
    public Instant getFirstResponseDueAt() { return firstResponseDueAt; }
    public Instant getResolutionDueAt() { return resolutionDueAt; }
    public String getAiTriageStatus() { return aiTriageStatus; }
    public UUID getLatestSuggestionId() { return latestSuggestionId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getResolvedAt() { return resolvedAt; }
    public Instant getClosedAt() { return closedAt; }
    public Long getVersion() { return version; }

    public void transitionTo(TicketStatus targetStatus) {
        if (!this.status.canTransitionTo(targetStatus)) {
            throw new IllegalStateException("Illegal ticket state transition from " + this.status + " to " + targetStatus);
        }
        this.status = targetStatus;
        if (targetStatus == TicketStatus.RESOLVED) {
            this.resolvedAt = Instant.now();
        } else if (targetStatus == TicketStatus.CLOSED) {
            this.closedAt = Instant.now();
        }
        this.updatedAt = Instant.now();
    }

    public void assign(UUID teamId, UUID agentId) {
        this.teamId = teamId;
        this.assignedAgentId = agentId;
        this.updatedAt = Instant.now();
    }

    public void updateTriageResult(String aiTriageStatus, String category, UUID suggestionId, UUID teamId) {
        this.aiTriageStatus = aiTriageStatus;
        if (category != null) this.category = category;
        if (suggestionId != null) this.latestSuggestionId = suggestionId;
        if (teamId != null) this.teamId = teamId;
        this.updatedAt = Instant.now();
    }

    public void setSlaDeadlines(UUID slaPolicyId, Instant firstResponseDueAt, Instant resolutionDueAt) {
        this.slaPolicyId = slaPolicyId;
        this.firstResponseDueAt = firstResponseDueAt;
        this.resolutionDueAt = resolutionDueAt;
        this.updatedAt = Instant.now();
    }
}
