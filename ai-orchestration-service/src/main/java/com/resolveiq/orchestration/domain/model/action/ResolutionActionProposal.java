package com.resolveiq.orchestration.domain.model.action;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "resolution_action_proposals", schema = "orchestration_schema")
public class ResolutionActionProposal {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "ticket_id", nullable = false)
    private UUID ticketId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 100)
    private ActionType actionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ActionStatus status = ActionStatus.PROPOSED;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 20)
    private RiskLevel riskLevel = RiskLevel.LOW;

    @Column(name = "input_payload", nullable = false, columnDefinition = "jsonb")
    private String inputPayload;

    @Column(name = "input_hash", nullable = false, length = 64)
    private String inputHash;

    @Column(name = "canonical_digest", nullable = false, length = 64)
    private String canonicalDigest;

    @Column(name = "canonical_bytes", nullable = false, columnDefinition = "TEXT")
    private String canonicalBytes;

    @Column(name = "ai_rationale", columnDefinition = "TEXT")
    private String aiRationale;

    @Column(name = "evidence_ids", columnDefinition = "TEXT")
    private String evidenceIds;

    @Column(name = "policy_version", nullable = false, length = 50)
    private String policyVersion = "1.0";

    @Column(name = "current_state_version", nullable = false, length = 50)
    private String currentStateVersion = "1";

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public ResolutionActionProposal() {}

    public ResolutionActionProposal(UUID tenantId, UUID ticketId, ActionType actionType,
                                    String inputPayload, String inputHash,
                                    String canonicalDigest, String canonicalBytes,
                                    String aiRationale, String evidenceIds,
                                    Instant expiresAt) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.ticketId = ticketId;
        this.actionType = actionType;
        this.status = ActionStatus.PROPOSED;
        this.inputPayload = inputPayload;
        this.inputHash = inputHash;
        this.canonicalDigest = canonicalDigest;
        this.canonicalBytes = canonicalBytes;
        this.aiRationale = aiRationale;
        this.evidenceIds = evidenceIds;
        this.expiresAt = expiresAt;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getTicketId() { return ticketId; }
    public ActionType getActionType() { return actionType; }
    public ActionStatus getStatus() { return status; }
    public void setStatus(ActionStatus status) { this.status = status; this.updatedAt = Instant.now(); }
    public RiskLevel getRiskLevel() { return riskLevel; }
    public void setRiskLevel(RiskLevel riskLevel) { this.riskLevel = riskLevel; }
    public String getInputPayload() { return inputPayload; }
    public String getInputHash() { return inputHash; }
    public String getCanonicalDigest() { return canonicalDigest; }
    public void setCanonicalDigest(String canonicalDigest) { this.canonicalDigest = canonicalDigest; }
    public String getCanonicalBytes() { return canonicalBytes; }
    public void setCanonicalBytes(String canonicalBytes) { this.canonicalBytes = canonicalBytes; }
    public String getAiRationale() { return aiRationale; }
    public String getEvidenceIds() { return evidenceIds; }
    public String getPolicyVersion() { return policyVersion; }
    public void setPolicyVersion(String policyVersion) { this.policyVersion = policyVersion; }
    public String getCurrentStateVersion() { return currentStateVersion; }
    public void setCurrentStateVersion(String currentStateVersion) { this.currentStateVersion = currentStateVersion; }
    public Long getVersion() { return version; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }
}
