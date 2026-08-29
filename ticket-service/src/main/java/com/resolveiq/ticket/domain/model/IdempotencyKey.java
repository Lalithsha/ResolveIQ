package com.resolveiq.ticket.domain.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "idempotent_commands", schema = "ticket_schema")
public class IdempotencyKey {
    public static final String IN_PROGRESS = "IN_PROGRESS";
    public static final String COMPLETED = "COMPLETED";

    @Id
    private UUID id;
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    @Column(name = "actor_id", nullable = false)
    private UUID actorId;
    @Column(nullable = false, length = 100)
    private String operation;
    @Column(name = "idempotency_key", nullable = false)
    private String key;
    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;
    @Column(nullable = false, length = 50)
    private String status;
    @Column(name = "response_code")
    private Integer responseCode;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response_body", columnDefinition = "jsonb")
    private String responseBody;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
    @Version
    private long version;

    protected IdempotencyKey() {}

    public IdempotencyKey(UUID id, UUID tenantId, UUID actorId, String operation, String key,
                          String requestHash, Instant expiresAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.actorId = actorId;
        this.operation = operation;
        this.key = key;
        this.requestHash = requestHash;
        this.status = IN_PROGRESS;
        this.createdAt = Instant.now();
        this.expiresAt = expiresAt;
    }

    public void complete(int responseCode, String responseBody) {
        this.status = COMPLETED;
        this.responseCode = responseCode;
        this.responseBody = responseBody;
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getActorId() { return actorId; }
    public String getOperation() { return operation; }
    public String getKey() { return key; }
    public String getRequestHash() { return requestHash; }
    public String getStatus() { return status; }
    public Integer getResponseCode() { return responseCode; }
    public String getResponseBody() { return responseBody; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public boolean isCompleted() { return COMPLETED.equals(status); }
}
