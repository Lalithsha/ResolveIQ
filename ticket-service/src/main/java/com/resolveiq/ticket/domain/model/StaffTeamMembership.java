package com.resolveiq.ticket.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "staff_team_memberships", schema = "ticket_schema")
public class StaffTeamMembership {
    @Id private UUID id;
    @Column(name = "tenant_id", nullable = false) private UUID tenantId;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Column(name = "team_id", nullable = false) private UUID teamId;
    @Column(nullable = false, length = 30) private String role;
    @Column(nullable = false) private boolean active;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    protected StaffTeamMembership() {}
    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getUserId() { return userId; }
    public UUID getTeamId() { return teamId; }
    public String getRole() { return role; }
    public boolean isActive() { return active; }
    public Instant getUpdatedAt() { return updatedAt; }
}
