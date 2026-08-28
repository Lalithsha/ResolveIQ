package com.resolveiq.ticket.domain.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ticket_status_history", schema = "ticket_schema")
public class TicketStatusHistory {

    @Id
    private UUID id;

    @Column(name = "ticket_id", nullable = false)
    private UUID ticketId;

    @Column(name = "from_status", nullable = false, length = 50)
    private String fromStatus;

    @Column(name = "to_status", nullable = false, length = 50)
    private String toStatus;

    @Column(name = "changed_by_user_id")
    private UUID changedByUserId;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public TicketStatusHistory() {}

    public TicketStatusHistory(UUID ticketId, String fromStatus, String toStatus, UUID changedByUserId, String reason) {
        this.id = UUID.randomUUID();
        this.ticketId = ticketId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.changedByUserId = changedByUserId;
        this.reason = reason;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getTicketId() { return ticketId; }
    public String getFromStatus() { return fromStatus; }
    public String getToStatus() { return toStatus; }
    public UUID getChangedByUserId() { return changedByUserId; }
    public String getReason() { return reason; }
    public Instant getCreatedAt() { return createdAt; }
}
