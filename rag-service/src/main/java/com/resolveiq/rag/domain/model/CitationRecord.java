package com.resolveiq.rag.domain.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "citation_records", schema = "rag_schema")
public class CitationRecord {

    @Id
    private UUID id;

    @Column(name = "suggestion_id", nullable = false)
    private UUID suggestionId;

    @Column(name = "retrieval_run_id")
    private UUID retrievalRunId;

    @Column(name = "source_type", nullable = false, length = 50)
    private String sourceType; // KNOWLEDGE_ARTICLE, RESOLVED_CASE

    @Column(name = "source_id", nullable = false)
    private UUID sourceId;

    @Column(name = "version_id")
    private UUID versionId;

    @Column(name = "chunk_id", nullable = false)
    private UUID chunkId;

    @Column(name = "citation_text", nullable = false, columnDefinition = "TEXT")
    private String citationText;

    @Column(name = "confidence_score")
    private Double confidenceScore;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public CitationRecord() {}

    public CitationRecord(
        UUID suggestionId,
        UUID retrievalRunId,
        String sourceType,
        UUID sourceId,
        UUID versionId,
        UUID chunkId,
        String citationText,
        Double confidenceScore
    ) {
        this.id = UUID.randomUUID();
        this.suggestionId = suggestionId;
        this.retrievalRunId = retrievalRunId;
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.versionId = versionId;
        this.chunkId = chunkId;
        this.citationText = citationText;
        this.confidenceScore = confidenceScore;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getSuggestionId() { return suggestionId; }
    public UUID getRetrievalRunId() { return retrievalRunId; }
    public String getSourceType() { return sourceType; }
    public UUID getSourceId() { return sourceId; }
    public UUID getVersionId() { return versionId; }
    public UUID getChunkId() { return chunkId; }
    public String getCitationText() { return citationText; }
    public Double getConfidenceScore() { return confidenceScore; }
    public Instant getCreatedAt() { return createdAt; }
}
