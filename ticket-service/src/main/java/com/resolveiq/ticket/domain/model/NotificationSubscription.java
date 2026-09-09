package com.resolveiq.ticket.domain.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notification_subscriptions", schema = "ticket_schema")
public class NotificationSubscription {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "incident_id", nullable = false)
    private UUID incidentId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "channel", nullable = false, length = 32)
    private String channel;

    @Column(name = "subscribed_at", nullable = false)
    private Instant subscribedAt;

    public NotificationSubscription() {}

    public NotificationSubscription(UUID id, UUID tenantId, UUID incidentId, UUID customerId, String channel) {
        this.id = id != null ? id : UUID.randomUUID();
        this.tenantId = tenantId;
        this.incidentId = incidentId;
        this.customerId = customerId;
        this.channel = channel != null ? channel : "PORTAL";
        this.subscribedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getIncidentId() { return incidentId; }
    public UUID getCustomerId() { return customerId; }
    public String getChannel() { return channel; }
    public Instant getSubscribedAt() { return subscribedAt; }
}
