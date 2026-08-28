package com.resolveiq.routing.domain.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "teams", schema = "routing_schema")
public class Team {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "max_active_tickets", nullable = false)
    private int maxActiveTickets;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Team() {}

    public Team(UUID id, UUID tenantId, String name, String description, int maxActiveTickets) {
        this.id = id != null ? id : UUID.randomUUID();
        this.tenantId = tenantId;
        this.name = name;
        this.description = description;
        this.maxActiveTickets = maxActiveTickets > 0 ? maxActiveTickets : 50;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getMaxActiveTickets() { return maxActiveTickets; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
