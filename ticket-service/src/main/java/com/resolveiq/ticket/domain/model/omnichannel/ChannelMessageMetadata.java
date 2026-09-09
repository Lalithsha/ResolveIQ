package com.resolveiq.ticket.domain.model.omnichannel;

import com.resolveiq.ticket.domain.model.DeliveryStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "channel_message_metadata", schema = "ticket_schema")
public class ChannelMessageMetadata {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "message_id", nullable = false, unique = true)
    private UUID messageId;

    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 50)
    private ChannelType channel;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false, length = 50)
    private ChannelDirection direction;

    @Column(name = "external_message_id")
    private String externalMessageId;

    @Column(name = "sender_address")
    private String senderAddress;

    @Column(name = "recipient_address")
    private String recipientAddress;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status", nullable = false, length = 50)
    private DeliveryStatus deliveryStatus;

    @Column(name = "idempotency_key")
    private String idempotencyKey;

    @Column(name = "provider_timestamp")
    private Instant providerTimestamp;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public ChannelMessageMetadata() {}

    public ChannelMessageMetadata(
        UUID tenantId,
        UUID messageId,
        UUID conversationId,
        ChannelType channel,
        ChannelDirection direction,
        String externalMessageId,
        String senderAddress,
        String recipientAddress,
        DeliveryStatus deliveryStatus,
        String idempotencyKey
    ) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.messageId = messageId;
        this.conversationId = conversationId;
        this.channel = channel;
        this.direction = direction;
        this.externalMessageId = externalMessageId;
        this.senderAddress = senderAddress;
        this.recipientAddress = recipientAddress;
        this.deliveryStatus = deliveryStatus != null ? deliveryStatus : DeliveryStatus.DELIVERED;
        this.idempotencyKey = idempotencyKey;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getMessageId() { return messageId; }
    public UUID getConversationId() { return conversationId; }
    public ChannelType getChannel() { return channel; }
    public ChannelDirection getDirection() { return direction; }
    public String getExternalMessageId() { return externalMessageId; }
    public String getSenderAddress() { return senderAddress; }
    public String getRecipientAddress() { return recipientAddress; }
    public DeliveryStatus getDeliveryStatus() { return deliveryStatus; }
    public void setDeliveryStatus(DeliveryStatus deliveryStatus) { this.deliveryStatus = deliveryStatus; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public Instant getProviderTimestamp() { return providerTimestamp; }
    public void setProviderTimestamp(Instant providerTimestamp) { this.providerTimestamp = providerTimestamp; }
    public Instant getCreatedAt() { return createdAt; }
}
