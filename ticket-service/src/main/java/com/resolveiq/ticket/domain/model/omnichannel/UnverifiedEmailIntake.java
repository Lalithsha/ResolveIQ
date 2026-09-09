package com.resolveiq.ticket.domain.model.omnichannel;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "unverified_email_intakes", schema = "ticket_schema")
public class UnverifiedEmailIntake {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "sender_email", nullable = false, length = 255)
    private String senderEmail;

    @Column(name = "sender_hmac", nullable = false, length = 128)
    private String senderHmac;

    @Column(name = "subject", nullable = false, length = 500)
    private String subject;

    @Column(name = "body_text", nullable = false, columnDefinition = "TEXT")
    private String bodyText;

    @Column(name = "external_message_id", nullable = false, length = 255)
    private String externalMessageId;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "status", nullable = false, length = 50)
    private String status; // PENDING, VERIFIED_LINKED, EXPIRED, REJECTED

    @Column(name = "promoted_ticket_id")
    private UUID promotedTicketId;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    public UnverifiedEmailIntake() {}

    public UnverifiedEmailIntake(
        UUID tenantId,
        String senderEmail,
        String senderHmac,
        String subject,
        String bodyText,
        String externalMessageId
    ) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.senderEmail = senderEmail;
        this.senderHmac = senderHmac;
        this.subject = subject;
        this.bodyText = bodyText;
        this.externalMessageId = externalMessageId;
        this.receivedAt = Instant.now();
        this.status = "PENDING";
        this.expiresAt = Instant.now().plusSeconds(7 * 24 * 3600); // 7-day default expiry
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getSenderEmail() { return senderEmail; }
    public String getSenderHmac() { return senderHmac; }
    public String getSubject() { return subject; }
    public String getBodyText() { return bodyText; }
    public String getExternalMessageId() { return externalMessageId; }
    public Instant getReceivedAt() { return receivedAt; }
    public String getStatus() { return status; }
    public UUID getPromotedTicketId() { return promotedTicketId; }
    public Instant getExpiresAt() { return expiresAt; }

    public void promoteToTicket(UUID ticketId) {
        this.status = "VERIFIED_LINKED";
        this.promotedTicketId = ticketId;
    }

    public void reject() {
        this.status = "REJECTED";
    }
}
