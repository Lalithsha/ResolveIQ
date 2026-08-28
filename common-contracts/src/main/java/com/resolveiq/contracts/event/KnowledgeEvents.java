package com.resolveiq.contracts.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Event payloads for Knowledge and RAG domain events.
 */
public final class KnowledgeEvents {

    private KnowledgeEvents() {}

    public static final String KNOWLEDGE_VERSION_PUBLISHED = "resolveiq.knowledge.version_published";
    public static final String KNOWLEDGE_INDEXING_COMPLETED = "resolveiq.knowledge.indexing_completed";

    public record KnowledgeVersionPublishedPayload(
        UUID documentId,
        UUID versionId,
        String title,
        String category,
        int chunkCount,
        UUID publishedByUserId,
        Instant publishedAt
    ) {}

    public record KnowledgeIndexingCompletedPayload(
        UUID documentId,
        UUID versionId,
        String embeddingModel,
        int dimensions,
        int indexedChunks,
        long indexingDurationMs,
        Instant completedAt
    ) {}
}
