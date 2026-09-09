package com.resolveiq.analysis.domain.model.evidence;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "evidence_jobs", schema = "analysis_schema")
public class EvidenceJob {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "ticket_id", nullable = false)
    private UUID ticketId;

    @Column(name = "attachment_id", nullable = false)
    private UUID attachmentId;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "media_type", nullable = false)
    private String mediaType;

    @Column(name = "file_size_bytes", nullable = false)
    private long fileSizeBytes;

    @Enumerated(EnumType.STRING)
    @Column(name = "pipeline_status", nullable = false)
    private PipelineStatus pipelineStatus;

    @Column(name = "consent_granted", nullable = false)
    private boolean consentGranted;

    @Column(name = "retention_class", nullable = false)
    private String retentionClass;

    @Column(name = "attempt", nullable = false)
    private int attempt;

    @Column(name = "lease_expires_at")
    private Instant leaseExpiresAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tool_versions", columnDefinition = "JSONB")
    private String toolVersions;

    @Column(name = "original_object_key")
    private String originalObjectKey;

    @Column(name = "raw_content", columnDefinition = "TEXT")
    private String rawContent;

    @Column(name = "error_details", columnDefinition = "TEXT")
    private String errorDetails;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected EvidenceJob() {}

    public EvidenceJob(UUID tenantId, UUID ticketId, UUID attachmentId, String fileName,
                       String mediaType, long fileSizeBytes, boolean consentGranted) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.ticketId = ticketId;
        this.attachmentId = attachmentId;
        this.fileName = fileName;
        this.mediaType = mediaType;
        this.fileSizeBytes = fileSizeBytes;
        this.pipelineStatus = PipelineStatus.QUEUED;
        this.consentGranted = consentGranted;
        this.retentionClass = "STANDARD_30D";
        this.attempt = 0;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getTicketId() { return ticketId; }
    public UUID getAttachmentId() { return attachmentId; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getMediaType() { return mediaType; }
    public long getFileSizeBytes() { return fileSizeBytes; }
    public PipelineStatus getPipelineStatus() { return pipelineStatus; }
    public void setPipelineStatus(PipelineStatus pipelineStatus) {
        this.pipelineStatus = pipelineStatus;
        this.updatedAt = Instant.now();
    }
    public boolean isConsentGranted() { return consentGranted; }
    public void setConsentGranted(boolean consentGranted) {
        this.consentGranted = consentGranted;
        this.updatedAt = Instant.now();
    }
    public String getRetentionClass() { return retentionClass; }
    public void setRetentionClass(String retentionClass) { this.retentionClass = retentionClass; }
    public int getAttempt() { return attempt; }
    public void setAttempt(int attempt) { this.attempt = attempt; }
    public Instant getLeaseExpiresAt() { return leaseExpiresAt; }
    public void setLeaseExpiresAt(Instant leaseExpiresAt) { this.leaseExpiresAt = leaseExpiresAt; }
    public String getToolVersions() { return toolVersions; }
    public void setToolVersions(String toolVersions) { this.toolVersions = toolVersions; }
    public String getOriginalObjectKey() { return originalObjectKey; }
    public void setOriginalObjectKey(String originalObjectKey) { this.originalObjectKey = originalObjectKey; }
    public String getRawContent() { return rawContent; }
    public void setRawContent(String rawContent) { this.rawContent = rawContent; }
    public String getErrorDetails() { return errorDetails; }
    public void setErrorDetails(String errorDetails) {
        this.errorDetails = errorDetails;
        this.updatedAt = Instant.now();
    }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
