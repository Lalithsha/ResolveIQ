package com.resolveiq.auth.domain.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "security_audit_events", schema = "auth_schema")
public class SecurityAuditEvent {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(nullable = false)
    private String status;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    public SecurityAuditEvent() {}

    public SecurityAuditEvent(UUID tenantId, UUID userId, String eventType, String status, String ipAddress, String userAgent) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.userId = userId;
        this.eventType = eventType;
        this.status = status;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.occurredAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getUserId() { return userId; }
    public String getEventType() { return eventType; }
    public String getStatus() { return status; }
    public String getIpAddress() { return ipAddress; }
    public String getUserAgent() { return userAgent; }
    public Instant getOccurredAt() { return occurredAt; }
}
