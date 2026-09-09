package com.resolveiq.ticket.domain.model.resolution;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "resolution_outcomes", schema = "ticket_schema")
public class ResolutionOutcome {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "resolution_id", nullable = false)
    private UUID resolutionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false)
    private OutcomeSource source;

    @Enumerated(EnumType.STRING)
    @Column(name = "rating", nullable = false)
    private OutcomeRating rating;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "weight", nullable = false)
    private int weight;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ResolutionOutcome() {}

    public ResolutionOutcome(UUID tenantId, UUID resolutionId, OutcomeSource source,
                             OutcomeRating rating, String reason, int weight) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.resolutionId = resolutionId;
        this.source = source;
        this.rating = rating;
        this.reason = reason;
        this.weight = weight;
        this.occurredAt = Instant.now();
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getResolutionId() { return resolutionId; }
    public OutcomeSource getSource() { return source; }
    public OutcomeRating getRating() { return rating; }
    public String getReason() { return reason; }
    public Instant getOccurredAt() { return occurredAt; }
    public int getWeight() { return weight; }
    public Instant getCreatedAt() { return createdAt; }
}
