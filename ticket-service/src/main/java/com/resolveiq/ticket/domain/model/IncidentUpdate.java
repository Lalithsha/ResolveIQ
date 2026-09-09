package com.resolveiq.ticket.domain.model;

import jakarta.persistence.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Entity
@Table(name = "incident_updates", schema = "ticket_schema")
public class IncidentUpdate {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "incident_id", nullable = false)
    private UUID incidentId;

    @Column(name = "update_number", nullable = false)
    private int updateNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private IncidentUpdateStatus status;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "audience_type", nullable = false, length = 32)
    private AudienceType audienceType;

    @Column(name = "audience_count", nullable = false)
    private int audienceCount;

    @Column(name = "author_id", nullable = false)
    private UUID authorId;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "content_digest", length = 64)
    private String contentDigest;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public IncidentUpdate() {}

    public IncidentUpdate(
        UUID id,
        UUID tenantId,
        UUID incidentId,
        int updateNumber,
        IncidentUpdateStatus status,
        String title,
        String message,
        AudienceType audienceType,
        int audienceCount,
        UUID authorId
    ) {
        this.id = id != null ? id : UUID.randomUUID();
        this.tenantId = tenantId;
        this.incidentId = incidentId;
        this.updateNumber = updateNumber;
        this.status = status != null ? status : IncidentUpdateStatus.DRAFT;
        this.title = title;
        this.message = message;
        this.audienceType = audienceType != null ? audienceType : AudienceType.ALL_AFFECTED;
        this.audienceCount = audienceCount;
        this.authorId = authorId;
        this.contentDigest = computeDigest(title, message, this.audienceType);
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public static String computeDigest(String title, String message, AudienceType audienceType) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            String raw = (title != null ? title.trim() : "") + "|"
                + (message != null ? message.trim() : "") + "|"
                + (audienceType != null ? audienceType.name() : "");
            byte[] hash = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public void submitForApproval() {
        if (this.status != IncidentUpdateStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT updates can be submitted for approval");
        }
        this.status = IncidentUpdateStatus.AWAITING_APPROVAL;
        this.updatedAt = Instant.now();
    }

    public void approve(UUID approverId, int currentAudienceCount) {
        if (this.status != IncidentUpdateStatus.DRAFT && this.status != IncidentUpdateStatus.AWAITING_APPROVAL) {
            throw new IllegalStateException("Cannot approve update in status " + this.status);
        }
        this.status = IncidentUpdateStatus.APPROVED;
        this.approvedBy = approverId;
        this.approvedAt = Instant.now();
        this.audienceCount = currentAudienceCount;
        this.contentDigest = computeDigest(title, message, audienceType);
        this.updatedAt = Instant.now();
    }

    public void publish(UUID publisherId) {
        if (this.status != IncidentUpdateStatus.APPROVED) {
            throw new IllegalStateException("Only APPROVED updates can be published");
        }
        String currentDigest = computeDigest(title, message, audienceType);
        if (!currentDigest.equals(this.contentDigest)) {
            this.status = IncidentUpdateStatus.AWAITING_APPROVAL;
            this.approvedBy = null;
            this.approvedAt = null;
            this.updatedAt = Instant.now();
            throw new IllegalStateException("Update content changed after approval. Re-approval required.");
        }
        this.status = IncidentUpdateStatus.PUBLISHED;
        this.publishedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void reject(UUID actorId) {
        this.status = IncidentUpdateStatus.REJECTED;
        this.approvedBy = actorId;
        this.updatedAt = Instant.now();
    }

    public void cancel() {
        if (this.status == IncidentUpdateStatus.PUBLISHED) {
            throw new IllegalStateException("Cannot cancel a published update");
        }
        this.status = IncidentUpdateStatus.CANCELLED;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getIncidentId() { return incidentId; }
    public int getUpdateNumber() { return updateNumber; }
    public IncidentUpdateStatus getStatus() { return status; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public AudienceType getAudienceType() { return audienceType; }
    public void setAudienceType(AudienceType audienceType) { this.audienceType = audienceType; }
    public int getAudienceCount() { return audienceCount; }
    public UUID getAuthorId() { return authorId; }
    public UUID getApprovedBy() { return approvedBy; }
    public Instant getApprovedAt() { return approvedAt; }
    public Instant getPublishedAt() { return publishedAt; }
    public String getContentDigest() { return contentDigest; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
