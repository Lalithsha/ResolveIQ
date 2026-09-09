package com.resolveiq.analysis.domain.model.evidence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "evidence_artifacts", schema = "analysis_schema")
public class EvidenceArtifact {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "job_id", nullable = false)
    private UUID jobId;

    @Column(name = "ticket_id", nullable = false)
    private UUID ticketId;

    @Enumerated(EnumType.STRING)
    @Column(name = "artifact_type", nullable = false)
    private ArtifactType artifactType;

    @Column(name = "redacted_object_key", nullable = false)
    private String redactedObjectKey;

    @Column(name = "page_or_frame")
    private Integer pageOrFrame;

    @Column(name = "timestamp_seconds")
    private Double timestampSeconds;

    @Column(name = "checksum_sha256", nullable = false)
    private String checksumSha256;

    @Column(name = "sensitivity_class", nullable = false)
    private String sensitivityClass;

    @Column(name = "redacted_content", columnDefinition = "TEXT")
    private String redactedContent;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected EvidenceArtifact() {}

    public EvidenceArtifact(UUID tenantId, UUID jobId, UUID ticketId, ArtifactType artifactType,
                            String redactedObjectKey, Integer pageOrFrame, Double timestampSeconds,
                            String checksumSha256, String sensitivityClass, String redactedContent) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.jobId = jobId;
        this.ticketId = ticketId;
        this.artifactType = artifactType;
        this.redactedObjectKey = redactedObjectKey;
        this.pageOrFrame = pageOrFrame;
        this.timestampSeconds = timestampSeconds;
        this.checksumSha256 = checksumSha256;
        this.sensitivityClass = sensitivityClass != null ? sensitivityClass : "INTERNAL";
        this.redactedContent = redactedContent;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getJobId() { return jobId; }
    public UUID getTicketId() { return ticketId; }
    public ArtifactType getArtifactType() { return artifactType; }
    public String getRedactedObjectKey() { return redactedObjectKey; }
    public Integer getPageOrFrame() { return pageOrFrame; }
    public Double getTimestampSeconds() { return timestampSeconds; }
    public String getChecksumSha256() { return checksumSha256; }
    public String getSensitivityClass() { return sensitivityClass; }
    public String getRedactedContent() { return redactedContent; }
    public Instant getCreatedAt() { return createdAt; }
}
