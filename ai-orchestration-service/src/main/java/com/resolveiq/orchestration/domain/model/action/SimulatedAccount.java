package com.resolveiq.orchestration.domain.model.action;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "simulated_accounts", schema = "orchestration_schema")
public class SimulatedAccount {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "user_email", nullable = false)
    private String userEmail;

    @Column(name = "is_locked", nullable = false)
    private boolean isLocked = true;

    @Column(name = "lock_reason")
    private String lockReason;

    @Column(name = "last_verified_identity_at")
    private Instant lastVerifiedIdentityAt;

    @Column(name = "sessions_revoked_at")
    private Instant sessionsRevokedAt;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public SimulatedAccount() {}

    public SimulatedAccount(UUID tenantId, UUID userId, String userEmail, boolean isLocked,
                            String lockReason, Instant lastVerifiedIdentityAt) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.userId = userId;
        this.userEmail = userEmail;
        this.isLocked = isLocked;
        this.lockReason = lockReason;
        this.lastVerifiedIdentityAt = lastVerifiedIdentityAt;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getUserId() { return userId; }
    public String getUserEmail() { return userEmail; }
    public boolean isLocked() { return isLocked; }
    public void setLocked(boolean locked) { isLocked = locked; this.updatedAt = Instant.now(); }
    public String getLockReason() { return lockReason; }
    public void setLockReason(String lockReason) { this.lockReason = lockReason; this.updatedAt = Instant.now(); }
    public Instant getLastVerifiedIdentityAt() { return lastVerifiedIdentityAt; }
    public void setLastVerifiedIdentityAt(Instant lastVerifiedIdentityAt) { this.lastVerifiedIdentityAt = lastVerifiedIdentityAt; }
    public Instant getSessionsRevokedAt() { return sessionsRevokedAt; }
    public void setSessionsRevokedAt(Instant sessionsRevokedAt) { this.sessionsRevokedAt = sessionsRevokedAt; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
