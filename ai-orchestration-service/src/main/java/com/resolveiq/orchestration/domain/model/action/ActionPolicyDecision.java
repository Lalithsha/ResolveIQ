package com.resolveiq.orchestration.domain.model.action;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "action_policy_decisions", schema = "orchestration_schema")
public class ActionPolicyDecision {

    @Id
    private UUID id;

    @Column(name = "proposal_id", nullable = false)
    private UUID proposalId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PolicyDecision decision;

    @Column(name = "matched_rules", columnDefinition = "jsonb")
    private String matchedRules;

    @Column(name = "required_permissions", columnDefinition = "jsonb")
    private String requiredPermissions;

    @Column(name = "required_approval_count", nullable = false)
    private int requiredApprovalCount = 1;

    @Column(name = "financial_limit_cents")
    private Long financialLimitCents;

    @Column(name = "reason_codes", columnDefinition = "jsonb")
    private String reasonCodes;

    @Column(name = "evaluated_at", nullable = false)
    private Instant evaluatedAt = Instant.now();

    public ActionPolicyDecision() {}

    public ActionPolicyDecision(UUID proposalId, UUID tenantId, PolicyDecision decision,
                                String matchedRules, String requiredPermissions,
                                int requiredApprovalCount, Long financialLimitCents,
                                String reasonCodes) {
        this.id = UUID.randomUUID();
        this.proposalId = proposalId;
        this.tenantId = tenantId;
        this.decision = decision;
        this.matchedRules = matchedRules;
        this.requiredPermissions = requiredPermissions;
        this.requiredApprovalCount = requiredApprovalCount;
        this.financialLimitCents = financialLimitCents;
        this.reasonCodes = reasonCodes;
        this.evaluatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getProposalId() { return proposalId; }
    public UUID getTenantId() { return tenantId; }
    public PolicyDecision getDecision() { return decision; }
    public String getMatchedRules() { return matchedRules; }
    public String getRequiredPermissions() { return requiredPermissions; }
    public int getRequiredApprovalCount() { return requiredApprovalCount; }
    public Long getFinancialLimitCents() { return financialLimitCents; }
    public String getReasonCodes() { return reasonCodes; }
    public Instant getEvaluatedAt() { return evaluatedAt; }
}
