package com.resolveiq.rag.domain.model.flywheel;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "knowledge_releases", schema = "rag_schema")
public class KnowledgeRelease {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "candidate_id", nullable = false)
    private UUID candidateId;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private KnowledgeReleaseStatus status;

    @Column(name = "evaluation_run_id")
    private UUID evaluationRunId;

    @Column(name = "approver_id", nullable = false)
    private UUID approverId;

    @Column(name = "release_notes", columnDefinition = "TEXT")
    private String releaseNotes;

    @Column(name = "released_at", nullable = false)
    private Instant releasedAt;

    protected KnowledgeRelease() {}

    public KnowledgeRelease(UUID tenantId, UUID candidateId, UUID documentId,
                            int versionNumber, UUID evaluationRunId,
                            UUID approverId, String releaseNotes) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.candidateId = candidateId;
        this.documentId = documentId;
        this.versionNumber = versionNumber;
        this.status = KnowledgeReleaseStatus.ACTIVE;
        this.evaluationRunId = evaluationRunId;
        this.approverId = approverId;
        this.releaseNotes = releaseNotes;
        this.releasedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getCandidateId() { return candidateId; }
    public UUID getDocumentId() { return documentId; }
    public int getVersionNumber() { return versionNumber; }
    public KnowledgeReleaseStatus getStatus() { return status; }
    public void setStatus(KnowledgeReleaseStatus status) { this.status = status; }
    public UUID getEvaluationRunId() { return evaluationRunId; }
    public UUID getApproverId() { return approverId; }
    public String getReleaseNotes() { return releaseNotes; }
    public Instant getReleasedAt() { return releasedAt; }
}
