package com.resolveiq.ticket.domain.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_suggestions", schema = "ticket_schema")
public class AiSuggestion {

    @Id
    private UUID id;

    @Column(name = "ticket_id", nullable = false)
    private UUID ticketId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "suggested_response", nullable = false, columnDefinition = "TEXT")
    private String suggestedResponse;

    @Column(name = "confidence_score")
    private Double confidenceScore;

    @Column(name = "model_name", nullable = false)
    private String modelName;

    @Column(name = "prompt_version", nullable = false)
    private String promptVersion;

    @Column(columnDefinition = "JSONB")
    private String citations;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private SuggestionStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "reviewed_by_agent_id")
    private UUID reviewedByAgentId;

    public AiSuggestion() {}

    public AiSuggestion(
        UUID id,
        UUID ticketId,
        UUID tenantId,
        String suggestedResponse,
        Double confidenceScore,
        String modelName,
        String promptVersion,
        String citations
    ) {
        this.id = id != null ? id : UUID.randomUUID();
        this.ticketId = ticketId;
        this.tenantId = tenantId;
        this.suggestedResponse = suggestedResponse;
        this.confidenceScore = confidenceScore;
        this.modelName = modelName;
        this.promptVersion = promptVersion;
        this.citations = citations;
        this.status = SuggestionStatus.PENDING_REVIEW;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getTicketId() { return ticketId; }
    public UUID getTenantId() { return tenantId; }
    public String getSuggestedResponse() { return suggestedResponse; }
    public Double getConfidenceScore() { return confidenceScore; }
    public String getModelName() { return modelName; }
    public String getPromptVersion() { return promptVersion; }
    public String getCitations() { return citations; }
    public SuggestionStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getReviewedAt() { return reviewedAt; }
    public UUID getReviewedByAgentId() { return reviewedByAgentId; }

    public void review(SuggestionStatus newStatus, UUID agentId) {
        this.status = newStatus;
        this.reviewedByAgentId = agentId;
        this.reviewedAt = Instant.now();
    }
}
