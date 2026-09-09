package com.resolveiq.orchestration.domain.model.action;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "simulated_account_operations", schema = "orchestration_schema")
public class SimulatedAccountOperation {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "provider_idempotency_key", nullable = false, length = 128)
    private String providerIdempotencyKey;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "operation_type", nullable = false, length = 50)
    private String operationType;

    @Column(nullable = false, length = 50)
    private String status; // SUCCEEDED, FAILED

    @Column(name = "provider_reference", nullable = false, length = 128)
    private String providerReference;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public SimulatedAccountOperation() {}

    public SimulatedAccountOperation(UUID tenantId, String providerIdempotencyKey,
                                     UUID userId, String operationType,
                                     String status, String providerReference) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.providerIdempotencyKey = providerIdempotencyKey;
        this.userId = userId;
        this.operationType = operationType;
        this.status = status;
        this.providerReference = providerReference;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getProviderIdempotencyKey() { return providerIdempotencyKey; }
    public UUID getUserId() { return userId; }
    public String getOperationType() { return operationType; }
    public String getStatus() { return status; }
    public String getProviderReference() { return providerReference; }
    public Instant getCreatedAt() { return createdAt; }
}
