package com.resolveiq.rag.domain.model.flywheel;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "knowledge_candidates", schema = "rag_schema")
public class KnowledgeCandidate {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "content_draft", nullable = false, columnDefinition = "TEXT")
    private String contentDraft;

    @Column(name = "sanitized_content", columnDefinition = "TEXT")
    private String sanitizedContent;

    @Column(name = "category", nullable = false)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(name = "eligibility_status", nullable = false)
    private CandidateEligibilityStatus eligibilityStatus;

    @Column(name = "verified_outcome_score", nullable = false)
    private int verifiedOutcomeScore;

    @Column(name = "distinct_source_customers", nullable = false)
    private int distinctSourceCustomers;

    @Enumerated(EnumType.STRING)
    @Column(name = "sanitization_status", nullable = false)
    private CandidateSanitizationStatus sanitizationStatus;

    @Column(name = "content_hash", length = 64)
    private String contentHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected KnowledgeCandidate() {}

    public KnowledgeCandidate(UUID tenantId, String title, String contentDraft, String category,
                              int verifiedOutcomeScore, int distinctSourceCustomers) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.title = title;
        this.contentDraft = contentDraft;
        this.category = category;
        this.verifiedOutcomeScore = verifiedOutcomeScore;
        this.distinctSourceCustomers = distinctSourceCustomers;
        // Eligibility rule from Section 22.7: requires at least 5 distinct customers and score >= 80
        this.eligibilityStatus = (distinctSourceCustomers >= 5 && verifiedOutcomeScore >= 80)
            ? CandidateEligibilityStatus.ELIGIBLE
            : CandidateEligibilityStatus.DRAFT;
        this.sanitizationStatus = CandidateSanitizationStatus.PENDING;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getTitle() { return title; }
    public String getContentDraft() { return contentDraft; }
    public String getSanitizedContent() { return sanitizedContent; }
    public void setSanitizedContent(String sanitizedContent) {
        this.sanitizedContent = sanitizedContent;
        this.updatedAt = Instant.now();
    }
    public String getCategory() { return category; }
    public CandidateEligibilityStatus getEligibilityStatus() { return eligibilityStatus; }
    public void setEligibilityStatus(CandidateEligibilityStatus eligibilityStatus) {
        this.eligibilityStatus = eligibilityStatus;
        this.updatedAt = Instant.now();
    }
    public int getVerifiedOutcomeScore() { return verifiedOutcomeScore; }
    public int getDistinctSourceCustomers() { return distinctSourceCustomers; }
    public CandidateSanitizationStatus getSanitizationStatus() { return sanitizationStatus; }
    public void setSanitizationStatus(CandidateSanitizationStatus sanitizationStatus) {
        this.sanitizationStatus = sanitizationStatus;
        this.updatedAt = Instant.now();
    }
    public String getContentHash() { return contentHash; }
    public void setContentHash(String contentHash) { this.contentHash = contentHash; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
