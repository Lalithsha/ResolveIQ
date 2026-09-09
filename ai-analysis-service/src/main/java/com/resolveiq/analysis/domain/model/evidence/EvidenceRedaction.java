package com.resolveiq.analysis.domain.model.evidence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "evidence_redactions", schema = "analysis_schema")
public class EvidenceRedaction {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "job_id", nullable = false)
    private UUID jobId;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private RedactionCategory category;

    @Column(name = "source_location", nullable = false)
    private String sourceLocation;

    @Column(name = "mask_method", nullable = false)
    private String maskMethod;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected EvidenceRedaction() {}

    public EvidenceRedaction(UUID tenantId, UUID jobId, RedactionCategory category,
                             String sourceLocation, String maskMethod) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.jobId = jobId;
        this.category = category;
        this.sourceLocation = sourceLocation;
        this.maskMethod = maskMethod != null ? maskMethod : "MASK_HASH";
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getJobId() { return jobId; }
    public RedactionCategory getCategory() { return category; }
    public String getSourceLocation() { return sourceLocation; }
    public String getMaskMethod() { return maskMethod; }
    public Instant getCreatedAt() { return createdAt; }
}
