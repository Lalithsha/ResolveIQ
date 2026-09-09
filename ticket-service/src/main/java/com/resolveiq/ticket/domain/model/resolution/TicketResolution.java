package com.resolveiq.ticket.domain.model.resolution;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Entity
@Table(name = "ticket_resolutions", schema = "ticket_schema")
public class TicketResolution {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "ticket_id", nullable = false)
    private UUID ticketId;

    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber;

    @Column(name = "resolver_id", nullable = false)
    private UUID resolverId;

    @Column(name = "resolved_at", nullable = false)
    private Instant resolvedAt;

    @Column(name = "confirmation_window_expires_at", nullable = false)
    private Instant confirmationWindowExpiresAt;

    @Column(name = "scheduled_closure_at")
    private Instant scheduledClosureAt;

    @Column(name = "solution_fingerprint")
    private String solutionFingerprint;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ResolutionAttemptStatus status;

    @Column(name = "score", nullable = false)
    private int score;

    @Column(name = "score_formula_version", nullable = false)
    private String scoreFormulaVersion;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected TicketResolution() {}

    public TicketResolution(UUID tenantId, UUID ticketId, int attemptNumber, UUID resolverId, String solutionFingerprint) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.ticketId = ticketId;
        this.attemptNumber = attemptNumber;
        this.resolverId = resolverId;
        this.resolvedAt = Instant.now();
        // 7-day confirmation window per specification
        this.confirmationWindowExpiresAt = this.resolvedAt.plus(7, ChronoUnit.DAYS);
        this.solutionFingerprint = solutionFingerprint;
        this.status = ResolutionAttemptStatus.AWAITING_CONFIRMATION;
        this.score = 0;
        this.scoreFormulaVersion = "v1";
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getTicketId() { return ticketId; }
    public int getAttemptNumber() { return attemptNumber; }
    public UUID getResolverId() { return resolverId; }
    public Instant getResolvedAt() { return resolvedAt; }
    public Instant getConfirmationWindowExpiresAt() { return confirmationWindowExpiresAt; }
    public Instant getScheduledClosureAt() { return scheduledClosureAt; }
    public void setScheduledClosureAt(Instant scheduledClosureAt) {
        this.scheduledClosureAt = scheduledClosureAt;
        this.updatedAt = Instant.now();
    }
    public String getSolutionFingerprint() { return solutionFingerprint; }
    public ResolutionAttemptStatus getStatus() { return status; }
    public void setStatus(ResolutionAttemptStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }
    public int getScore() { return score; }
    public void setScore(int score) {
        this.score = score;
        this.updatedAt = Instant.now();
    }
    public String getScoreFormulaVersion() { return scoreFormulaVersion; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
