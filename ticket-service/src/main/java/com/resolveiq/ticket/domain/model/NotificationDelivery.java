package com.resolveiq.ticket.domain.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notification_deliveries", schema = "ticket_schema")
public class NotificationDelivery {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "update_id", nullable = false)
    private UUID updateId;

    @Column(name = "recipient_customer_id", nullable = false)
    private UUID recipientCustomerId;

    @Column(name = "channel", nullable = false, length = 32)
    private String channel;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private DeliveryStatus status;

    @Column(name = "delivery_key", nullable = false, length = 128)
    private String deliveryKey;

    @Column(name = "provider_message_id", length = 128)
    private String providerMessageId;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public NotificationDelivery() {}

    public NotificationDelivery(
        UUID id,
        UUID tenantId,
        UUID updateId,
        UUID recipientCustomerId,
        String channel,
        DeliveryStatus status,
        String deliveryKey
    ) {
        this.id = id != null ? id : UUID.randomUUID();
        this.tenantId = tenantId;
        this.updateId = updateId;
        this.recipientCustomerId = recipientCustomerId;
        this.channel = channel;
        this.status = status != null ? status : DeliveryStatus.PENDING;
        this.deliveryKey = deliveryKey;
        this.attemptCount = 0;
        this.createdAt = Instant.now();
    }

    public void markSent(String providerMessageId) {
        this.status = DeliveryStatus.SENT;
        this.providerMessageId = providerMessageId;
        this.deliveredAt = Instant.now();
        this.attemptCount++;
    }

    public void markFailed(String reason) {
        this.status = DeliveryStatus.FAILED;
        this.failureReason = reason;
        this.attemptCount++;
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getUpdateId() { return updateId; }
    public UUID getRecipientCustomerId() { return recipientCustomerId; }
    public String getChannel() { return channel; }
    public DeliveryStatus getStatus() { return status; }
    public String getDeliveryKey() { return deliveryKey; }
    public String getProviderMessageId() { return providerMessageId; }
    public String getFailureReason() { return failureReason; }
    public int getAttemptCount() { return attemptCount; }
    public Instant getDeliveredAt() { return deliveredAt; }
    public Instant getCreatedAt() { return createdAt; }
}
