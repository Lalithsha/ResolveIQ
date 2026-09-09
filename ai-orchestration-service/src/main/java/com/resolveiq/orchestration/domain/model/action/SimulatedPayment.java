package com.resolveiq.orchestration.domain.model.action;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "simulated_payments", schema = "orchestration_schema")
public class SimulatedPayment {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "customer_account_id", nullable = false, length = 100)
    private String customerAccountId;

    @Column(name = "payment_reference", nullable = false, length = 100)
    private String paymentReference;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "original_amount_cents", nullable = false)
    private long originalAmountCents;

    @Column(name = "refunded_amount_cents", nullable = false)
    private long refundedAmountCents = 0L;

    @Column(name = "reserved_refund_cents", nullable = false)
    private long reservedRefundCents = 0L;

    @Column(name = "is_settled", nullable = false)
    private boolean isSettled = true;

    @Column(name = "duplicate_of_reference", length = 100)
    private String duplicateOfReference;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public SimulatedPayment() {}

    public SimulatedPayment(UUID tenantId, String customerAccountId, String paymentReference,
                            String currency, long originalAmountCents, boolean isSettled,
                            String duplicateOfReference) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.customerAccountId = customerAccountId;
        this.paymentReference = paymentReference;
        this.currency = currency;
        this.originalAmountCents = originalAmountCents;
        this.refundedAmountCents = 0L;
        this.reservedRefundCents = 0L;
        this.isSettled = isSettled;
        this.duplicateOfReference = duplicateOfReference;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getCustomerAccountId() { return customerAccountId; }
    public String getPaymentReference() { return paymentReference; }
    public String getCurrency() { return currency; }
    public long getOriginalAmountCents() { return originalAmountCents; }
    public long getRefundedAmountCents() { return refundedAmountCents; }
    public void setRefundedAmountCents(long refundedAmountCents) { this.refundedAmountCents = refundedAmountCents; this.updatedAt = Instant.now(); }
    public long getReservedRefundCents() { return reservedRefundCents; }
    public void setReservedRefundCents(long reservedRefundCents) { this.reservedRefundCents = reservedRefundCents; this.updatedAt = Instant.now(); }
    public boolean isSettled() { return isSettled; }
    public void setSettled(boolean settled) { isSettled = settled; this.updatedAt = Instant.now(); }
    public String getDuplicateOfReference() { return duplicateOfReference; }
    public void setDuplicateOfReference(String duplicateOfReference) { this.duplicateOfReference = duplicateOfReference; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public long getAvailableRefundableAmountCents() {
        return Math.max(0, originalAmountCents - refundedAmountCents - reservedRefundCents);
    }
}
