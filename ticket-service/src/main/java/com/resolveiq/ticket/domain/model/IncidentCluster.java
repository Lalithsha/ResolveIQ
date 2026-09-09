package com.resolveiq.ticket.domain.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "incident_clusters", schema = "ticket_schema")
public class IncidentCluster {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "incident_id")
    private UUID incidentId;

    @Column(name = "window_start", nullable = false)
    private Instant windowStart;

    @Column(name = "window_end", nullable = false)
    private Instant windowEnd;

    @Column(name = "centroid_reference", length = 128)
    private String centroidReference;

    @Column(name = "centroid_hash", length = 64)
    private String centroidHash;

    @Column(name = "ticket_count", nullable = false)
    private int ticketCount;

    @Column(name = "baseline_count", nullable = false)
    private int baselineCount;

    @Column(name = "anomaly_score", nullable = false)
    private double anomalyScore;

    @Column(name = "dominant_category", nullable = false, length = 64)
    private String dominantCategory;

    @Column(name = "product", nullable = false, length = 128)
    private String product;

    @Column(name = "region", length = 64)
    private String region;

    @Column(name = "error_fingerprints", columnDefinition = "JSONB")
    private String errorFingerprints;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ClusterStatus status;

    @Column(name = "explanation", columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "algorithm_version", nullable = false, length = 64)
    private String algorithmVersion;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public IncidentCluster() {}

    public IncidentCluster(
        UUID id,
        UUID tenantId,
        UUID incidentId,
        Instant windowStart,
        Instant windowEnd,
        String centroidReference,
        String centroidHash,
        int ticketCount,
        int baselineCount,
        double anomalyScore,
        String dominantCategory,
        String product,
        String region,
        String errorFingerprints,
        ClusterStatus status,
        String explanation,
        String algorithmVersion
    ) {
        this.id = id != null ? id : UUID.randomUUID();
        this.tenantId = tenantId;
        this.incidentId = incidentId;
        this.windowStart = windowStart;
        this.windowEnd = windowEnd;
        this.centroidReference = centroidReference;
        this.centroidHash = centroidHash;
        this.ticketCount = ticketCount;
        this.baselineCount = baselineCount;
        this.anomalyScore = anomalyScore;
        this.dominantCategory = dominantCategory;
        this.product = product;
        this.region = region;
        this.errorFingerprints = errorFingerprints;
        this.status = status != null ? status : ClusterStatus.PROPOSED;
        this.explanation = explanation;
        this.algorithmVersion = algorithmVersion;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getIncidentId() { return incidentId; }
    public void setIncidentId(UUID incidentId) { this.incidentId = incidentId; }
    public Instant getWindowStart() { return windowStart; }
    public Instant getWindowEnd() { return windowEnd; }
    public String getCentroidReference() { return centroidReference; }
    public void setCentroidReference(String centroidReference) { this.centroidReference = centroidReference; }
    public String getCentroidHash() { return centroidHash; }
    public void setCentroidHash(String centroidHash) { this.centroidHash = centroidHash; }
    public int getTicketCount() { return ticketCount; }
    public int getBaselineCount() { return baselineCount; }
    public double getAnomalyScore() { return anomalyScore; }
    public String getDominantCategory() { return dominantCategory; }
    public String getProduct() { return product; }
    public String getRegion() { return region; }
    public String getErrorFingerprints() { return errorFingerprints; }
    public ClusterStatus getStatus() { return status; }
    public void setStatus(ClusterStatus status) { this.status = status; }
    public String getExplanation() { return explanation; }
    public String getAlgorithmVersion() { return algorithmVersion; }
    public Instant getCreatedAt() { return createdAt; }
}
