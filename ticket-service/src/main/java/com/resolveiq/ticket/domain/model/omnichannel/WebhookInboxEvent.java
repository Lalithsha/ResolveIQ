package com.resolveiq.ticket.domain.model.omnichannel;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "webhook_inbox_events", schema = "ticket_schema")
public class WebhookInboxEvent {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "provider", nullable = false, length = 100)
    private String provider;

    @Column(name = "external_event_id", nullable = false, length = 255)
    private String externalEventId;

    @Column(name = "payload_hash", nullable = false, length = 128)
    private String payloadHash;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    public WebhookInboxEvent() {}

    public WebhookInboxEvent(UUID tenantId, String provider, String externalEventId, String payloadHash) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.provider = provider;
        this.externalEventId = externalEventId;
        this.payloadHash = payloadHash;
        this.receivedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getProvider() { return provider; }
    public String getExternalEventId() { return externalEventId; }
    public String getPayloadHash() { return payloadHash; }
    public Instant getReceivedAt() { return receivedAt; }
}
