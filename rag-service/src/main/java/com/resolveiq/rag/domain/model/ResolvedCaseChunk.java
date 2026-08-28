package com.resolveiq.rag.domain.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "resolved_case_chunks", schema = "rag_schema")
public class ResolvedCaseChunk {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "resolved_case_id", nullable = false)
    private UUID resolvedCaseId;

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

    public ResolvedCaseChunk() {}

    public ResolvedCaseChunk(
        UUID tenantId,
        UUID resolvedCaseId,
        int chunkIndex,
        String content,
        String contentHash,
        String embeddingModel
    ) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.resolvedCaseId = resolvedCaseId;
        this.chunkIndex = chunkIndex;
        this.content = content;
        this.contentHash = contentHash;
        this.embeddingModel = embeddingModel;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getResolvedCaseId() { return resolvedCaseId; }
    public int getChunkIndex() { return chunkIndex; }
    public String getContent() { return content; }
    public String getContentHash() { return contentHash; }
    public String getEmbeddingModel() { return embeddingModel; }
    public Instant getCreatedAt() { return createdAt; }
}
