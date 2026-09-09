package com.resolveiq.orchestration.domain.model.action;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "action_approvals", schema = "orchestration_schema")
public class ActionApproval {

    @Id
    private UUID id;

    @Column(name = "proposal_id", nullable = false)
    private UUID proposalId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "actor_id", nullable = false)
    private UUID actorId;

    @Column(name = "actor_role", nullable = false, length = 50)
    private String actorRole;

    @Column(nullable = false, length = 50)
    private String decision; // APPROVED, REJECTED

    @Column(name = "approved_digest", nullable = false, length = 64)
    private String approvedDigest;

    @Column(name = "authentication_time")
    private Instant authenticationTime;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public ActionApproval() {}

    public ActionApproval(UUID proposalId, UUID tenantId, UUID actorId, String actorRole,
                          String decision, String approvedDigest, Instant authenticationTime, String comment) {
        this.id = UUID.randomUUID();
        this.proposalId = proposalId;
        this.tenantId = tenantId;
        this.actorId = actorId;
        this.actorRole = actorRole;
        this.decision = decision;
        this.approvedDigest = approvedDigest;
        this.authenticationTime = authenticationTime;
        this.comment = comment;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getProposalId() { return proposalId; }
    public UUID getTenantId() { return tenantId; }
    public UUID getActorId() { return actorId; }
    public String getActorRole() { return actorRole; }
    public String getDecision() { return decision; }
    public String getApprovedDigest() { return approvedDigest; }
    public Instant getAuthenticationTime() { return authenticationTime; }
    public String getComment() { return comment; }
    public Instant getCreatedAt() { return createdAt; }
}
