package com.resolveiq.rag.domain.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "knowledge_versions", schema = "rag_schema")
public class KnowledgeVersion {

    @Id
    private UUID id;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "created_by_user_id")
    private UUID createdByUserId;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "published_by_user_id")
    private UUID publishedByUserId;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "reviewed_by_user_id")
    private UUID reviewedByUserId;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "review_note", columnDefinition = "TEXT")
    private String reviewNote;

    @Column(name = "superseded_at")
    private Instant supersededAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public KnowledgeVersion() {}

    public KnowledgeVersion(UUID documentId, int versionNumber, String content, String summary, UUID createdByUserId) {
        this.id = UUID.randomUUID();
        this.documentId = documentId;
        this.versionNumber = versionNumber;
        this.content = content;
        this.summary = summary;
        this.status = "DRAFT";
        this.createdByUserId = createdByUserId;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getDocumentId() { return documentId; }
    public int getVersionNumber() { return versionNumber; }
    public String getContent() { return content; }
    public String getSummary() { return summary; }
    public String getStatus() { return status; }
    public UUID getCreatedByUserId() { return createdByUserId; }
    public Instant getSubmittedAt() { return submittedAt; }
    public UUID getPublishedByUserId() { return publishedByUserId; }
    public Instant getPublishedAt() { return publishedAt; }
    public UUID getReviewedByUserId() { return reviewedByUserId; }
    public Instant getReviewedAt() { return reviewedAt; }
    public String getReviewNote() { return reviewNote; }
    public Instant getSupersededAt() { return supersededAt; }
    public Instant getCreatedAt() { return createdAt; }

    public void submitForReview() {
        requireStatus("DRAFT");
        status = "IN_REVIEW";
        submittedAt = Instant.now();
        reviewNote = null;
    }

    public void reject(UUID reviewerId, String note) {
        requireStatus("IN_REVIEW");
        if (note == null || note.isBlank()) throw new IllegalArgumentException("A review note is required");
        status = "REJECTED";
        reviewedByUserId = reviewerId;
        reviewedAt = Instant.now();
        reviewNote = note.trim();
    }

    public void publish(UUID reviewerId, String note) {
        requireStatus("IN_REVIEW");
        status = "PUBLISHED";
        reviewedByUserId = reviewerId;
        reviewedAt = Instant.now();
        reviewNote = note == null || note.isBlank() ? null : note.trim();
        publishedByUserId = reviewerId;
        publishedAt = Instant.now();
        supersededAt = null;
    }

    public void supersede() {
        if (!"PUBLISHED".equals(status)) return;
        status = "SUPERSEDED";
        supersededAt = Instant.now();
    }

    public void restore(UUID reviewerId, String note) {
        if (!java.util.Set.of("SUPERSEDED", "PUBLISHED").contains(status)) {
            throw new IllegalStateException("Only a previously published version can be restored");
        }
        status = "PUBLISHED";
        reviewedByUserId = reviewerId;
        reviewedAt = Instant.now();
        reviewNote = note == null || note.isBlank() ? "Rolled back to this version" : note.trim();
        publishedByUserId = reviewerId;
        publishedAt = Instant.now();
        supersededAt = null;
    }

    private void requireStatus(String expected) {
        if (!expected.equals(status)) {
            throw new IllegalStateException("Knowledge version must be " + expected + " but is " + status);
        }
    }
}
