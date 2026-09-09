package com.resolveiq.rag.domain.model.flywheel;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "rollback_records", schema = "rag_schema")
public class RollbackRecord {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "release_id", nullable = false)
    private UUID releaseId;

    @Column(name = "target_release_id")
    private UUID targetReleaseId;

    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(name = "performed_by", nullable = false)
    private UUID performedBy;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected RollbackRecord() {}

    public RollbackRecord(UUID tenantId, UUID releaseId, UUID targetReleaseId, String reason, UUID performedBy) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.releaseId = releaseId;
        this.targetReleaseId = targetReleaseId;
        this.reason = reason;
        this.performedBy = performedBy;
        this.occurredAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getReleaseId() { return releaseId; }
    public UUID getTargetReleaseId() { return targetReleaseId; }
    public String getReason() { return reason; }
    public UUID getPerformedBy() { return performedBy; }
    public Instant getOccurredAt() { return occurredAt; }
}
