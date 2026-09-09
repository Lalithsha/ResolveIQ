package com.resolveiq.orchestration.domain.model.action;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "simulated_payment_operations", schema = "orchestration_schema")
public class SimulatedPaymentOperation {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "provider_idempotency_key", nullable = false, length = 128)
    private String providerIdempotencyKey;

    @Column(name = "payment_reference", nullable = false, length = 100)
    private String paymentReference;

    @Column(name = "operation_type", nullable = false, length = 50)
    private String operationType;

    @Column(name = "amount_cents", nullable = false)
    private long amountCents;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, length = 50)
    private String status; // SUCCEEDED, TIMEOUT_ACCEPTED, FAILED

    @Column(name = "provider_reference", nullable = false, length = 128)
    private String providerReference;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public SimulatedPaymentOperation() {}

    public SimulatedPaymentOperation(UUID tenantId, String providerIdempotencyKey,
                                     String paymentReference, String operationType,
                                     long amountCents, String currency, String status,
                                     String providerReference) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.providerIdempotencyKey = providerIdempotencyKey;
        this.paymentReference = paymentReference;
        this.operationType = operationType;
        this.amountCents = amountCents;
        this.currency = currency;
        this.status = status;
        this.providerReference = providerReference;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getProviderIdempotencyKey() { return providerIdempotencyKey; }
    public String getPaymentReference() { return paymentReference; }
    public String getOperationType() { return operationType; }
    public long getAmountCents() { return amountCents; }
    public String getCurrency() { return currency; }
    public String getStatus() { return status; }
    public String getProviderReference() { return providerReference; }
    public Instant getCreatedAt() { return createdAt; }
}
