package com.resolveiq.ticket.domain.model.omnichannel;

import com.resolveiq.ticket.domain.model.DeliveryStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "delivery_attempts", schema = "ticket_schema")
public class DeliveryAttempt {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "message_id", nullable = false)
    private UUID messageId;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 50)
    private ChannelType channel;

    @Column(name = "recipient_address", nullable = false, length = 255)
    private String recipientAddress;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private DeliveryStatus status;

    @Column(name = "provider_message_id")
    private String providerMessageId;

    @Column(name = "error_details", columnDefinition = "TEXT")
    private String errorDetails;

    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public DeliveryAttempt() {}

    public DeliveryAttempt(
        UUID tenantId,
        UUID messageId,
        ChannelType channel,
        String recipientAddress,
        DeliveryStatus status,
        String providerMessageId,
        String errorDetails,
        int attemptNumber
    ) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.messageId = messageId;
        this.channel = channel;
        this.recipientAddress = recipientAddress;
        this.status = status;
        this.providerMessageId = providerMessageId;
        this.errorDetails = errorDetails;
        this.attemptNumber = attemptNumber;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getMessageId() { return messageId; }
    public ChannelType getChannel() { return channel; }
    public String getRecipientAddress() { return recipientAddress; }
    public DeliveryStatus getStatus() { return status; }
    public String getProviderMessageId() { return providerMessageId; }
    public String getErrorDetails() { return errorDetails; }
    public int getAttemptNumber() { return attemptNumber; }
    public Instant getCreatedAt() { return createdAt; }
}
