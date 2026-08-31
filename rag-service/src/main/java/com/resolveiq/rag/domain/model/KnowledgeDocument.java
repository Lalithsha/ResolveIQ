package com.resolveiq.rag.domain.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "knowledge_documents", schema = "rag_schema")
public class KnowledgeDocument {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(nullable = false, length = 100)
    private String category;

    @Column(length = 100)
    private String product;

    @Column(nullable = false, length = 10)
    private String language;

    @Column(nullable = false, length = 50)
    private String status; // DRAFT, PUBLISHED, ARCHIVED

    @Column(name = "active_version_id")
    private UUID activeVersionId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public KnowledgeDocument() {}

    public KnowledgeDocument(UUID id, UUID tenantId, String title, String category, String product, String language) {
        this.id = id != null ? id : UUID.randomUUID();
        this.tenantId = tenantId;
        this.title = title;
        this.category = category;
        this.product = product;
        this.language = language != null ? language : "en";
        this.status = "DRAFT";
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getTitle() { return title; }
    public String getCategory() { return category; }
    public String getProduct() { return product; }
    public String getLanguage() { return language; }
    public String getStatus() { return status; }
    public UUID getActiveVersionId() { return activeVersionId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void publishVersion(UUID versionId) {
        this.activeVersionId = versionId;
        this.status = "PUBLISHED";
        this.updatedAt = Instant.now();
    }

    public void markInReview() {
        if ("ARCHIVED".equals(status)) throw new IllegalStateException("Archived knowledge cannot be reviewed");
        if (activeVersionId == null) this.status = "IN_REVIEW";
        this.updatedAt = Instant.now();
    }

    public void markDraft() {
        if ("ARCHIVED".equals(status)) throw new IllegalStateException("Archived knowledge cannot return to draft");
        if (activeVersionId == null) this.status = "DRAFT";
        this.updatedAt = Instant.now();
    }

    public void restore() {
        if (!"ARCHIVED".equals(status)) throw new IllegalStateException("Knowledge document is not archived");
        this.status = activeVersionId == null ? "DRAFT" : "PUBLISHED";
        this.updatedAt = Instant.now();
    }

    public void archive() {
        this.status = "ARCHIVED";
        this.updatedAt = Instant.now();
    }
}
