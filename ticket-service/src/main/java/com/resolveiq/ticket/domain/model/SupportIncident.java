package com.resolveiq.ticket.domain.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "support_incidents", schema = "ticket_schema")
public class SupportIncident {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "incident_number", nullable = false, length = 32)
    private String incidentNumber;

    @Column(name = "title", nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private IncidentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 16)
    private IncidentSeverity severity;

    @Column(name = "detected_at", nullable = false)
    private Instant detectedAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "owner_user_id")
    private UUID ownerUserId;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "root_cause", columnDefinition = "TEXT")
    private String rootCause;

    @Column(name = "resolution_summary", columnDefinition = "TEXT")
    private String resolutionSummary;

    @Column(name = "detection_algorithm_version", length = 64)
    private String detectionAlgorithmVersion;

    @Column(name = "cluster_confidence")
    private Double clusterConfidence;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public SupportIncident() {}

    public SupportIncident(
        UUID id,
        UUID tenantId,
        String incidentNumber,
        String title,
        IncidentStatus status,
        IncidentSeverity severity,
        Instant detectedAt,
        UUID createdBy,
        String summary,
        String detectionAlgorithmVersion,
        Double clusterConfidence
    ) {
        this.id = id != null ? id : UUID.randomUUID();
        this.tenantId = tenantId;
        this.incidentNumber = incidentNumber;
        this.title = title;
        this.status = status != null ? status : IncidentStatus.PROPOSED;
        this.severity = severity != null ? severity : IncidentSeverity.MEDIUM;
        this.detectedAt = detectedAt != null ? detectedAt : Instant.now();
        this.createdBy = createdBy;
        this.summary = summary;
        this.detectionAlgorithmVersion = detectionAlgorithmVersion;
        this.clusterConfidence = clusterConfidence;
        this.version = 0L;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void confirm(UUID ownerUserId) {
        if (!status.canTransitionTo(IncidentStatus.INVESTIGATING)) {
            throw new IllegalStateException("Cannot confirm incident from status " + status);
        }
        this.status = IncidentStatus.INVESTIGATING;
        this.confirmedAt = Instant.now();
        this.ownerUserId = ownerUserId;
        this.updatedAt = Instant.now();
    }

    public void dismiss(String reason) {
        if (!status.canTransitionTo(IncidentStatus.DISMISSED)) {
            throw new IllegalStateException("Cannot dismiss incident from status " + status);
        }
        this.status = IncidentStatus.DISMISSED;
        this.rootCause = reason;
        this.updatedAt = Instant.now();
    }

    public void transition(IncidentStatus newStatus, String resolutionSummary, UUID actorId) {
        if (!this.status.canTransitionTo(newStatus)) {
            throw new IllegalStateException(String.format("Invalid transition from %s to %s", this.status, newStatus));
        }
        this.status = newStatus;
        if (newStatus == IncidentStatus.RESOLVED) {
            this.resolvedAt = Instant.now();
            this.resolutionSummary = resolutionSummary;
        }
        if (actorId != null && this.ownerUserId == null) {
            this.ownerUserId = actorId;
        }
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getIncidentNumber() { return incidentNumber; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public IncidentStatus getStatus() { return status; }
    public IncidentSeverity getSeverity() { return severity; }
    public void setSeverity(IncidentSeverity severity) { this.severity = severity; }
    public Instant getDetectedAt() { return detectedAt; }
    public Instant getConfirmedAt() { return confirmedAt; }
    public Instant getResolvedAt() { return resolvedAt; }
    public UUID getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(UUID ownerUserId) { this.ownerUserId = ownerUserId; }
    public UUID getCreatedBy() { return createdBy; }
    public Long getVersion() { return version; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getRootCause() { return rootCause; }
    public void setRootCause(String rootCause) { this.rootCause = rootCause; }
    public String getResolutionSummary() { return resolutionSummary; }
    public String getDetectionAlgorithmVersion() { return detectionAlgorithmVersion; }
    public Double getClusterConfidence() { return clusterConfidence; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
