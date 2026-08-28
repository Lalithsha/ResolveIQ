package com.resolveiq.ticket.domain.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "idempotency_keys", schema = "ticket_schema")
public class IdempotencyKey {

    @Id
    private String key;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "request_hash", nullable = false)
    private String requestHash;

    @Column(name = "response_body", columnDefinition = "JSONB")
    private String responseBody;

    @Column(name = "status_code", nullable = false)
    private int statusCode;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    public IdempotencyKey() {}

    public IdempotencyKey(String key, UUID tenantId, String requestHash, String responseBody, int statusCode, Instant expiresAt) {
        this.key = key;
        this.tenantId = tenantId;
        this.requestHash = requestHash;
        this.responseBody = responseBody;
        this.statusCode = statusCode;
        this.createdAt = Instant.now();
        this.expiresAt = expiresAt;
    }

    public String getKey() { return key; }
    public UUID getTenantId() { return tenantId; }
    public String getRequestHash() { return requestHash; }
    public String getResponseBody() { return responseBody; }
    public int getStatusCode() { return statusCode; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getExpiresAt() { return expiresAt; }

    public boolean isExpired() {
        return expiresAt.isBefore(Instant.now());
    }
}
