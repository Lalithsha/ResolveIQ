package com.resolveiq.ticket.domain.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "incident_components", schema = "ticket_schema")
public class IncidentComponent {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "incident_id", nullable = false)
    private UUID incidentId;

    @Column(name = "component_name", nullable = false, length = 128)
    private String componentName;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "impact_summary", columnDefinition = "TEXT")
    private String impactSummary;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public IncidentComponent() {}

    public IncidentComponent(UUID id, UUID tenantId, UUID incidentId, String componentName, String status, String impactSummary) {
        this.id = id != null ? id : UUID.randomUUID();
        this.tenantId = tenantId;
        this.incidentId = incidentId;
        this.componentName = componentName;
        this.status = status;
        this.impactSummary = impactSummary;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getIncidentId() { return incidentId; }
    public String getComponentName() { return componentName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getImpactSummary() { return impactSummary; }
    public void setImpactSummary(String impactSummary) { this.impactSummary = impactSummary; }
    public Instant getCreatedAt() { return createdAt; }
}
