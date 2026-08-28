package com.resolveiq.ticket.domain.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "suggestion_feedback", schema = "ticket_schema")
public class SuggestionFeedback {

    @Id
    private UUID id;

    @Column(name = "suggestion_id", nullable = false)
    private UUID suggestionId;

    @Column(name = "ticket_id", nullable = false)
    private UUID ticketId;

    @Column(name = "agent_id", nullable = false)
    private UUID agentId;

    @Column(nullable = false, length = 50)
    private String action; // ACCEPTED, EDITED, REJECTED, REGENERATED

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "edited_content", columnDefinition = "TEXT")
    private String editedContent;

    private Integer rating;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public SuggestionFeedback() {}

    public SuggestionFeedback(UUID suggestionId, UUID ticketId, UUID agentId, String action, String rejectionReason, String editedContent, Integer rating) {
        this.id = UUID.randomUUID();
        this.suggestionId = suggestionId;
        this.ticketId = ticketId;
        this.agentId = agentId;
        this.action = action;
        this.rejectionReason = rejectionReason;
        this.editedContent = editedContent;
        this.rating = rating;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getSuggestionId() { return suggestionId; }
    public UUID getTicketId() { return ticketId; }
    public UUID getAgentId() { return agentId; }
    public String getAction() { return action; }
    public String getRejectionReason() { return rejectionReason; }
    public String getEditedContent() { return editedContent; }
    public Integer getRating() { return rating; }
    public Instant getCreatedAt() { return createdAt; }
}
