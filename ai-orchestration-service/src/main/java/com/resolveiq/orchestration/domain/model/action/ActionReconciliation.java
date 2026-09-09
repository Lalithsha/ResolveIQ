package com.resolveiq.orchestration.domain.model.action;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "action_reconciliations", schema = "orchestration_schema")
public class ActionReconciliation {

    @Id
    private UUID id;

    @Column(name = "proposal_id", nullable = false)
    private UUID proposalId;

    @Column(name = "execution_id", nullable = false)
    private UUID executionId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 50)
    private String status; // RECONCILED, MISMATCH_MANUAL_REVIEW

    @Column(name = "expected_state", nullable = false, columnDefinition = "jsonb")
    private String expectedState;

    @Column(name = "observed_state", nullable = false, columnDefinition = "jsonb")
    private String observedState;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "reconciled_at", nullable = false)
    private Instant reconciledAt = Instant.now();

    public ActionReconciliation() {}

    public ActionReconciliation(UUID proposalId, UUID executionId, UUID tenantId, String status,
                                String expectedState, String observedState, String notes) {
        this.id = UUID.randomUUID();
        this.proposalId = proposalId;
        this.executionId = executionId;
        this.tenantId = tenantId;
        this.status = status;
        this.expectedState = expectedState;
        this.observedState = observedState;
        this.notes = notes;
        this.reconciledAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getProposalId() { return proposalId; }
    public UUID getExecutionId() { return executionId; }
    public UUID getTenantId() { return tenantId; }
    public String getStatus() { return status; }
    public String getExpectedState() { return expectedState; }
    public String getObservedState() { return observedState; }
    public String getNotes() { return notes; }
    public Instant getReconciledAt() { return reconciledAt; }
}
