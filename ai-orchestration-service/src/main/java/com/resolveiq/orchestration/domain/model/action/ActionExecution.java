package com.resolveiq.orchestration.domain.model.action;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "action_executions", schema = "orchestration_schema")
public class ActionExecution {

    @Id
    private UUID id;

    @Column(name = "proposal_id", nullable = false)
    private UUID proposalId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber = 1;

    @Column(nullable = false, length = 100)
    private String provider;

    @Column(name = "provider_idempotency_key", nullable = false, length = 128)
    private String providerIdempotencyKey;

    @Column(name = "client_idempotency_key", nullable = false, length = 128)
    private String clientIdempotencyKey;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Column(name = "provider_reference", length = 128)
    private String providerReference;

    @Column(nullable = false, length = 50)
    private String status; // EXECUTING, SUCCEEDED, EXECUTION_UNKNOWN, FAILED_RETRYABLE, FAILED_FINAL

    @Column(name = "sanitized_response", columnDefinition = "jsonb")
    private String sanitizedResponse;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt = Instant.now();

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "lease_expires_at")
    private Instant leaseExpiresAt;

    public ActionExecution() {}

    public ActionExecution(UUID proposalId, UUID tenantId, int attemptNumber, String provider,
                           String providerIdempotencyKey, String requestHash) {
        this.id = UUID.randomUUID();
        this.proposalId = proposalId;
        this.tenantId = tenantId;
        this.attemptNumber = attemptNumber;
        this.provider = provider;
        this.providerIdempotencyKey = providerIdempotencyKey;
        this.clientIdempotencyKey = providerIdempotencyKey;
        this.requestHash = requestHash;
        this.status = "QUEUED";
        this.startedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getProposalId() { return proposalId; }
    public UUID getTenantId() { return tenantId; }
    public int getAttemptNumber() { return attemptNumber; }
    public void setAttemptNumber(int attemptNumber) { this.attemptNumber = attemptNumber; }
    public String getProvider() { return provider; }
    public String getProviderIdempotencyKey() { return providerIdempotencyKey; }
    public void setProviderIdempotencyKey(String providerIdempotencyKey) { this.providerIdempotencyKey = providerIdempotencyKey; }
    public String getClientIdempotencyKey() { return clientIdempotencyKey; }
    public UUID getActorId() { return actorId; }
    public void setActorId(UUID actorId) { this.actorId = actorId; }
    public String getRequestHash() { return requestHash; }
    public String getProviderReference() { return providerReference; }
    public void setProviderReference(String providerReference) { this.providerReference = providerReference; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getSanitizedResponse() { return sanitizedResponse; }
    public void setSanitizedResponse(String sanitizedResponse) { this.sanitizedResponse = sanitizedResponse; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public Instant getLeaseExpiresAt() { return leaseExpiresAt; }
    public void setLeaseExpiresAt(Instant leaseExpiresAt) { this.leaseExpiresAt = leaseExpiresAt; }
}
