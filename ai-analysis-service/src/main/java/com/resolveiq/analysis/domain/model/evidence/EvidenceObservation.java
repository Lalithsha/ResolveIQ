package com.resolveiq.analysis.domain.model.evidence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "evidence_observations", schema = "analysis_schema")
public class EvidenceObservation {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "job_id", nullable = false)
    private UUID jobId;

    @Column(name = "ticket_id", nullable = false)
    private UUID ticketId;

    @Enumerated(EnumType.STRING)
    @Column(name = "observation_type", nullable = false)
    private ObservationType observationType;

    @Column(name = "code_or_key", nullable = false)
    private String codeOrKey;

    @Column(name = "summary", nullable = false, columnDefinition = "TEXT")
    private String summary;

    @Column(name = "confidence", nullable = false)
    private double confidence;

    @Column(name = "source_coordinates", columnDefinition = "TEXT")
    private String sourceCoordinates;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected EvidenceObservation() {}

    public EvidenceObservation(UUID tenantId, UUID jobId, UUID ticketId, ObservationType observationType,
                               String codeOrKey, String summary, double confidence, String sourceCoordinates) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.jobId = jobId;
        this.ticketId = ticketId;
        this.observationType = observationType;
        this.codeOrKey = codeOrKey;
        this.summary = summary;
        this.confidence = confidence;
        this.sourceCoordinates = sourceCoordinates;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getJobId() { return jobId; }
    public UUID getTicketId() { return ticketId; }
    public ObservationType getObservationType() { return observationType; }
    public String getCodeOrKey() { return codeOrKey; }
    public String getSummary() { return summary; }
    public double getConfidence() { return confidence; }
    public String getSourceCoordinates() { return sourceCoordinates; }
    public Instant getCreatedAt() { return createdAt; }
}
