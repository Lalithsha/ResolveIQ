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

    @Column(name = "published_by_user_id")
    private UUID publishedByUserId;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public KnowledgeVersion() {}

    public KnowledgeVersion(UUID documentId, int versionNumber, String content, String summary, UUID publishedByUserId) {
        this.id = UUID.randomUUID();
        this.documentId = documentId;
        this.versionNumber = versionNumber;
        this.content = content;
        this.summary = summary;
        this.publishedByUserId = publishedByUserId;
        this.publishedAt = Instant.now();
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getDocumentId() { return documentId; }
    public int getVersionNumber() { return versionNumber; }
    public String getContent() { return content; }
    public String getSummary() { return summary; }
    public UUID getPublishedByUserId() { return publishedByUserId; }
    public Instant getPublishedAt() { return publishedAt; }
    public Instant getCreatedAt() { return createdAt; }
}
