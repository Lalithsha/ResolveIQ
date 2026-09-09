package com.resolveiq.rag.domain.model.flywheel;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "evaluation_runs", schema = "rag_schema")
public class EvaluationRun {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "candidate_id", nullable = false)
    private UUID candidateId;

    @Column(name = "dataset_version", nullable = false)
    private String datasetVersion;

    @Column(name = "baseline_recall_at_5", nullable = false)
    private double baselineRecallAt5;

    @Column(name = "proposed_recall_at_5", nullable = false)
    private double proposedRecallAt5;

    @Column(name = "baseline_mrr", nullable = false)
    private double baselineMrr;

    @Column(name = "proposed_mrr", nullable = false)
    private double proposedMrr;

    @Column(name = "safety_cases_passed", nullable = false)
    private boolean safetyCasesPassed;

    @Column(name = "latency_p95_ratio", nullable = false)
    private double latencyP95Ratio;

    @Column(name = "gate_passed", nullable = false)
    private boolean gatePassed;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected EvaluationRun() {}

    public EvaluationRun(UUID tenantId, UUID candidateId, String datasetVersion,
                         double baselineRecallAt5, double proposedRecallAt5,
                         double baselineMrr, double proposedMrr,
                         boolean safetyCasesPassed, double latencyP95Ratio) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.candidateId = candidateId;
        this.datasetVersion = datasetVersion;
        this.baselineRecallAt5 = baselineRecallAt5;
        this.proposedRecallAt5 = proposedRecallAt5;
        this.baselineMrr = baselineMrr;
        this.proposedMrr = proposedMrr;
        this.safetyCasesPassed = safetyCasesPassed;
        this.latencyP95Ratio = latencyP95Ratio;
        // Gate requirements from Section 22.7:
        // Recall@5 >= 0.85, MRR >= 0.75, neither lower than baseline, safety cases pass, latency <= 1.2x
        this.gatePassed = proposedRecallAt5 >= 0.85 &&
                          proposedMrr >= 0.75 &&
                          proposedRecallAt5 >= baselineRecallAt5 &&
                          proposedMrr >= baselineMrr &&
                          safetyCasesPassed &&
                          latencyP95Ratio <= 1.20;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getCandidateId() { return candidateId; }
    public String getDatasetVersion() { return datasetVersion; }
    public double getBaselineRecallAt5() { return baselineRecallAt5; }
    public double getProposedRecallAt5() { return proposedRecallAt5; }
    public double getBaselineMrr() { return baselineMrr; }
    public double getProposedMrr() { return proposedMrr; }
    public boolean isSafetyCasesPassed() { return safetyCasesPassed; }
    public double getLatencyP95Ratio() { return latencyP95Ratio; }
    public boolean isGatePassed() { return gatePassed; }
    public Instant getCreatedAt() { return createdAt; }
}
