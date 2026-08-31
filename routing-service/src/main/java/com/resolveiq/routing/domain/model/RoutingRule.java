package com.resolveiq.routing.domain.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "routing_rules", schema = "routing_schema")
public class RoutingRule {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 50)
    private String version;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "JSONB")
    private String conditions;

    @Column(name = "target_team_id", nullable = false)
    private UUID targetTeamId;

    @Column(name = "priority_order", nullable = false)
    private int priorityOrder;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public RoutingRule() {}

    public RoutingRule(UUID id, UUID tenantId, String name, String version, String conditions, UUID targetTeamId, int priorityOrder) {
        this.id = id != null ? id : UUID.randomUUID();
        this.tenantId = tenantId;
        this.name = name;
        this.version = version != null ? version : "v1.0";
        this.conditions = conditions;
        this.targetTeamId = targetTeamId;
        this.priorityOrder = priorityOrder;
        this.active = true;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getName() { return name; }
    public String getVersion() { return version; }
    public String getConditions() { return conditions; }
    public UUID getTargetTeamId() { return targetTeamId; }
    public int getPriorityOrder() { return priorityOrder; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }

    public void setActive(boolean active) {
        this.active = active;
    }
}
