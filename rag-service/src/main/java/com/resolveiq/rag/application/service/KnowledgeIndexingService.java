package com.resolveiq.rag.application.service;

import com.resolveiq.rag.application.port.EmbeddingPort;
import com.resolveiq.rag.application.dto.ReindexKnowledgeResponse;
import com.resolveiq.rag.domain.model.KnowledgeDocument;
import com.resolveiq.rag.domain.model.KnowledgeVersion;
import com.resolveiq.rag.domain.repository.KnowledgeChunkRepository;
import com.resolveiq.rag.domain.repository.KnowledgeDocumentRepository;
import com.resolveiq.rag.domain.repository.KnowledgeVersionRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
public class KnowledgeIndexingService {
    private final KnowledgeDocumentRepository documents;
    private final KnowledgeVersionRepository versions;
    private final KnowledgeChunkRepository chunks;
    private final EmbeddingPort embeddingPort;
    private final KnowledgeIndexPersistence persistence;

    public KnowledgeIndexingService(KnowledgeDocumentRepository documents,
                                    KnowledgeVersionRepository versions,
                                    KnowledgeChunkRepository chunks,
                                    EmbeddingPort embeddingPort,
                                    KnowledgeIndexPersistence persistence) {
        this.documents = documents;
        this.versions = versions;
        this.chunks = chunks;
        this.embeddingPort = embeddingPort;
        this.persistence = persistence;
    }

    public void index(UUID tenantId, UUID documentId, UUID versionId) {
        documents.findByIdAndTenantId(documentId, tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Knowledge document not found: " + documentId));
        KnowledgeVersion version = versions.findByIdAndDocumentId(versionId, documentId)
            .orElseThrow(() -> new IllegalArgumentException("Knowledge version not found: " + versionId));

        List<String> contentChunks = chunkText(version.getContent(), 600);
        if (contentChunks.isEmpty()) throw new IllegalStateException("Knowledge version contains no indexable content");
        List<KnowledgeIndexPersistence.PreparedChunk> prepared = new ArrayList<>();
        for (int i = 0; i < contentChunks.size(); i++) {
            String content = contentChunks.get(i);
            float[] embedding = embeddingPort.embed(content);
            requireValidEmbedding(embedding);
            prepared.add(new KnowledgeIndexPersistence.PreparedChunk(i, content, hash(content), formatVector(embedding)));
        }
        persistence.replace(tenantId, documentId, versionId, embeddingPort.getModelName(), prepared);
    }

    public ReindexKnowledgeResponse reindexMissing(UUID tenantId) {
        List<KnowledgeDocument> published = documents.findByTenantIdAndStatus(tenantId, "PUBLISHED");
        List<UUID> reindexed = new ArrayList<>();
        for (KnowledgeDocument document : published) {
            UUID activeVersionId = document.getActiveVersionId();
            if (activeVersionId == null) continue;
            long total = chunks.countByVersionId(activeVersionId);
            long invalid = chunks.countInvalidIndexRows(
                tenantId, activeVersionId, embeddingPort.getDimension(), embeddingPort.getModelName());
            if (total == 0 || invalid > 0) {
                index(tenantId, document.getId(), activeVersionId);
                reindexed.add(document.getId());
            }
        }
        return new ReindexKnowledgeResponse(published.size(), reindexed.size(), List.copyOf(reindexed));
    }

    private List<String> chunkText(String text, int targetChars) {
        if (text == null || text.isBlank()) return List.of();
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String paragraph : text.split("\\R\\s*\\R")) {
            if (current.length() > 0 && current.length() + paragraph.length() > targetChars) {
                result.add(current.toString().trim());
                current = new StringBuilder();
            }
            current.append(paragraph).append("\n\n");
        }
        if (!current.isEmpty()) result.add(current.toString().trim());
        return result;
    }

    private void requireValidEmbedding(float[] values) {
        if (values == null || values.length != embeddingPort.getDimension()) {
            throw new IllegalStateException("Embedding dimension does not match configured provider dimension");
        }
        for (float value : values) if (!Float.isFinite(value)) throw new IllegalStateException("Embedding contains non-finite values");
    }

    private String hash(String content) {
        try {
            return Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256")
                .digest(content.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception error) {
            throw new IllegalStateException("SHA-256 is unavailable", error);
        }
    }

    private String formatVector(float[] vector) {
        StringBuilder value = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) value.append(',');
            value.append(vector[i]);
        }
        return value.append(']').toString();
    }
}
