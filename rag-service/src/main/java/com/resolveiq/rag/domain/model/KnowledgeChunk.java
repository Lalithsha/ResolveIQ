package com.resolveiq.rag.domain.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "knowledge_chunks", schema = "rag_schema")
public class KnowledgeChunk {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Column(name = "version_id", nullable = false)
    private UUID versionId;

    @Column(name = "chunk_index", nullable = false)
    private int chunkIndex;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "content_hash", nullable = false)
    private String contentHash;

    @Column(name = "embedding_model", nullable = false)
    private String embeddingModel;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public KnowledgeChunk() {}

    public KnowledgeChunk(
        UUID tenantId,
        UUID documentId,
        UUID versionId,
        int chunkIndex,
        String content,
        String contentHash,
        String embeddingModel
    ) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.documentId = documentId;
        this.versionId = versionId;
        this.chunkIndex = chunkIndex;
        this.content = content;
        this.contentHash = contentHash;
        this.embeddingModel = embeddingModel;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getDocumentId() { return documentId; }
    public UUID getVersionId() { return versionId; }
    public int getChunkIndex() { return chunkIndex; }
    public String getContent() { return content; }
    public String getContentHash() { return contentHash; }
    public String getEmbeddingModel() { return embeddingModel; }
    public Instant getCreatedAt() { return createdAt; }
}
