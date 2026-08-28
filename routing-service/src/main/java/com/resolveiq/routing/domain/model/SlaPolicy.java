package com.resolveiq.routing.domain.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sla_policies", schema = "routing_schema")
public class SlaPolicy {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 50)
    private String priority; // LOW, MEDIUM, HIGH, CRITICAL

    @Column(name = "first_response_target_minutes", nullable = false)
    private int firstResponseTargetMinutes;

    @Column(name = "resolution_target_minutes", nullable = false)
    private int resolutionTargetMinutes;

    @Column(name = "business_hours_only", nullable = false)
    private boolean businessHoursOnly;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public SlaPolicy() {}

    public SlaPolicy(
        UUID id,
        UUID tenantId,
        String name,
        String priority,
        int firstResponseTargetMinutes,
        int resolutionTargetMinutes,
        boolean businessHoursOnly
    ) {
        this.id = id != null ? id : UUID.randomUUID();
        this.tenantId = tenantId;
        this.name = name;
        this.priority = priority;
        this.firstResponseTargetMinutes = firstResponseTargetMinutes;
        this.resolutionTargetMinutes = resolutionTargetMinutes;
        this.businessHoursOnly = businessHoursOnly;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getName() { return name; }
    public String getPriority() { return priority; }
    public int getFirstResponseTargetMinutes() { return firstResponseTargetMinutes; }
    public int getResolutionTargetMinutes() { return resolutionTargetMinutes; }
    public boolean isBusinessHoursOnly() { return businessHoursOnly; }
    public Instant getCreatedAt() { return createdAt; }
}
