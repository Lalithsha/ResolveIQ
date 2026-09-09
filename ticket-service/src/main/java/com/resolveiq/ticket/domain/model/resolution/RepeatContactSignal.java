package com.resolveiq.ticket.domain.model.resolution;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "repeat_contact_signals", schema = "ticket_schema")
public class RepeatContactSignal {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "prior_ticket_id", nullable = false)
    private UUID priorTicketId;

    @Column(name = "current_ticket_id", nullable = false)
    private UUID currentTicketId;

    @Column(name = "similarity", nullable = false)
    private double similarity;

    @Column(name = "time_delta_seconds", nullable = false)
    private long timeDeltaSeconds;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RepeatContactStatus status;

    @Column(name = "reviewer_id")
    private UUID reviewerId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected RepeatContactSignal() {}

    public RepeatContactSignal(UUID tenantId, UUID customerId, UUID priorTicketId,
                               UUID currentTicketId, double similarity, long timeDeltaSeconds,
                               RepeatContactStatus status) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.customerId = customerId;
        this.priorTicketId = priorTicketId;
        this.currentTicketId = currentTicketId;
        this.similarity = similarity;
        this.timeDeltaSeconds = timeDeltaSeconds;
        this.status = status != null ? status : RepeatContactStatus.SUGGESTED;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getCustomerId() { return customerId; }
    public UUID getPriorTicketId() { return priorTicketId; }
    public UUID getCurrentTicketId() { return currentTicketId; }
    public double getSimilarity() { return similarity; }
    public long getTimeDeltaSeconds() { return timeDeltaSeconds; }
    public RepeatContactStatus getStatus() { return status; }
    public void setStatus(RepeatContactStatus status) { this.status = status; }
    public UUID getReviewerId() { return reviewerId; }
    public void setReviewerId(UUID reviewerId) { this.reviewerId = reviewerId; }
    public Instant getCreatedAt() { return createdAt; }
}
