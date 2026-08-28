package com.resolveiq.ticket.domain.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ticket_messages", schema = "ticket_schema")
public class TicketMessage {

    @Id
    private UUID id;

    @Column(name = "ticket_id", nullable = false)
    private UUID ticketId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "sender_id", nullable = false)
    private UUID senderId;

    @Column(name = "sender_role", nullable = false, length = 50)
    private String senderRole;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "is_internal", nullable = false)
    private boolean isInternal;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public TicketMessage() {}

    public TicketMessage(UUID ticketId, UUID tenantId, UUID senderId, String senderRole, String content, boolean isInternal) {
        this.id = UUID.randomUUID();
        this.ticketId = ticketId;
        this.tenantId = tenantId;
        this.senderId = senderId;
        this.senderRole = senderRole;
        this.content = content;
        this.isInternal = isInternal;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getTicketId() { return ticketId; }
    public UUID getTenantId() { return tenantId; }
    public UUID getSenderId() { return senderId; }
    public String getSenderRole() { return senderRole; }
    public String getContent() { return content; }
    public boolean isInternal() { return isInternal; }
    public Instant getCreatedAt() { return createdAt; }
}
