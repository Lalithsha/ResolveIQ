package com.resolveiq.ticket.domain.model.omnichannel;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "conversations", schema = "ticket_schema")
public class Conversation {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "ticket_id", nullable = false)
    private UUID ticketId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private ConversationStatus status;

    @Column(name = "primary_customer_id", nullable = false)
    private UUID primaryCustomerId;

    @Column(name = "assigned_agent_id")
    private UUID assignedAgentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "handoff_state", nullable = false, length = 50)
    private HandoffState handoffState;

    @Column(name = "handoff_requested_at")
    private Instant handoffRequestedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_channel", nullable = false, length = 50)
    private ChannelType preferredChannel;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Conversation() {}

    public Conversation(UUID tenantId, UUID ticketId, UUID primaryCustomerId) {
        this.id = ticketId; // Canonical 1-to-1 default with ticket
        this.tenantId = tenantId;
        this.ticketId = ticketId;
        this.status = ConversationStatus.ACTIVE;
        this.primaryCustomerId = primaryCustomerId;
        this.handoffState = HandoffState.NONE;
        this.preferredChannel = ChannelType.PORTAL;
        this.version = 0L;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getTicketId() { return ticketId; }
    public ConversationStatus getStatus() { return status; }
    public void setStatus(ConversationStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }
    public UUID getPrimaryCustomerId() { return primaryCustomerId; }
    public void setPrimaryCustomerId(UUID primaryCustomerId) {
        this.primaryCustomerId = primaryCustomerId;
        this.updatedAt = Instant.now();
    }
    public UUID getAssignedAgentId() { return assignedAgentId; }
    public void setAssignedAgentId(UUID assignedAgentId) {
        this.assignedAgentId = assignedAgentId;
        this.updatedAt = Instant.now();
    }
    public HandoffState getHandoffState() { return handoffState; }
    public void setHandoffState(HandoffState handoffState) {
        this.handoffState = handoffState;
        this.updatedAt = Instant.now();
    }
    public Instant getHandoffRequestedAt() { return handoffRequestedAt; }
    public void setHandoffRequestedAt(Instant handoffRequestedAt) {
        this.handoffRequestedAt = handoffRequestedAt;
    }
    public ChannelType getPreferredChannel() { return preferredChannel; }
    public void setPreferredChannel(ChannelType preferredChannel) {
        this.preferredChannel = preferredChannel;
        this.updatedAt = Instant.now();
    }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void requestHandoff() {
        this.handoffState = HandoffState.QUEUED;
        this.handoffRequestedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void assignAgent(UUID agentId) {
        this.assignedAgentId = agentId;
        this.handoffState = HandoffState.ASSIGNED;
        this.updatedAt = Instant.now();
    }
}
