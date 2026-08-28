package com.resolveiq.routing.domain.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "agents", schema = "routing_schema")
public class Agent {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "team_id")
    private UUID teamId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false, length = 50)
    private String status; // ONLINE, BUSY, OFFLINE

    @Column(name = "active_ticket_count", nullable = false)
    private int activeTicketCount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Agent() {}

    public Agent(UUID id, UUID tenantId, UUID teamId, String name, String email) {
        this.id = id != null ? id : UUID.randomUUID();
        this.tenantId = tenantId;
        this.teamId = teamId;
        this.name = name;
        this.email = email;
        this.status = "ONLINE";
        this.activeTicketCount = 0;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getTeamId() { return teamId; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getStatus() { return status; }
    public int getActiveTicketCount() { return activeTicketCount; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void incrementWorkload() {
        this.activeTicketCount++;
        this.updatedAt = Instant.now();
    }

    public void decrementWorkload() {
        if (this.activeTicketCount > 0) this.activeTicketCount--;
        this.updatedAt = Instant.now();
    }
}
