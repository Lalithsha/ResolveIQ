package com.resolveiq.ticket.domain.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "customer_impacts", schema = "ticket_schema")
public class CustomerImpact {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "incident_id", nullable = false)
    private UUID incidentId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "ticket_id", nullable = false)
    private UUID ticketId;

    @Column(name = "impact_level", nullable = false, length = 32)
    private String impactLevel;

    @Column(name = "notified", nullable = false)
    private boolean notified;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public CustomerImpact() {}

    public CustomerImpact(UUID id, UUID tenantId, UUID incidentId, UUID customerId, UUID ticketId, String impactLevel) {
        this.id = id != null ? id : UUID.randomUUID();
        this.tenantId = tenantId;
        this.incidentId = incidentId;
        this.customerId = customerId;
        this.ticketId = ticketId;
        this.impactLevel = impactLevel != null ? impactLevel : "DIRECT";
        this.notified = false;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getIncidentId() { return incidentId; }
    public UUID getCustomerId() { return customerId; }
    public UUID getTicketId() { return ticketId; }
    public String getImpactLevel() { return impactLevel; }
    public boolean isNotified() { return notified; }
    public void setNotified(boolean notified) { this.notified = notified; }
    public Instant getCreatedAt() { return createdAt; }
}
