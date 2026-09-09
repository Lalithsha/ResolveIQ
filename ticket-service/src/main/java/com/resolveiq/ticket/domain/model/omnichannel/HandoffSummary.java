package com.resolveiq.ticket.domain.model.omnichannel;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "handoff_summaries", schema = "ticket_schema")
public class HandoffSummary {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;

    @Column(name = "ticket_id", nullable = false)
    private UUID ticketId;

    @Column(name = "issue_summary", nullable = false, columnDefinition = "TEXT")
    private String issueSummary;

    @Column(name = "verified_facts", nullable = false, columnDefinition = "TEXT")
    private String verifiedFacts;

    @Column(name = "attempted_steps", nullable = false, columnDefinition = "TEXT")
    private String attemptedSteps;

    @Column(name = "promised_actions", nullable = false, columnDefinition = "TEXT")
    private String promisedActions;

    @Column(name = "sentiment", nullable = false, length = 50)
    private String sentiment;

    @Column(name = "open_questions", nullable = false, columnDefinition = "TEXT")
    private String openQuestions;

    @Column(name = "created_by_user_id")
    private UUID createdByUserId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public HandoffSummary() {}

    public HandoffSummary(
        UUID tenantId,
        UUID conversationId,
        UUID ticketId,
        String issueSummary,
        String verifiedFacts,
        String attemptedSteps,
        String promisedActions,
        String sentiment,
        String openQuestions,
        UUID createdByUserId
    ) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.conversationId = conversationId;
        this.ticketId = ticketId;
        this.issueSummary = issueSummary;
        this.verifiedFacts = verifiedFacts;
        this.attemptedSteps = attemptedSteps;
        this.promisedActions = promisedActions;
        this.sentiment = sentiment;
        this.openQuestions = openQuestions;
        this.createdByUserId = createdByUserId;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getConversationId() { return conversationId; }
    public UUID getTicketId() { return ticketId; }
    public String getIssueSummary() { return issueSummary; }
    public String getVerifiedFacts() { return verifiedFacts; }
    public String getAttemptedSteps() { return attemptedSteps; }
    public String getPromisedActions() { return promisedActions; }
    public String getSentiment() { return sentiment; }
    public String getOpenQuestions() { return openQuestions; }
    public UUID getCreatedByUserId() { return createdByUserId; }
    public Instant getCreatedAt() { return createdAt; }
}
