package com.resolveiq.rag.application.service;

import com.resolveiq.rag.domain.model.KnowledgeChunk;
import com.resolveiq.rag.domain.repository.KnowledgeChunkRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class KnowledgeIndexPersistence {
    private final KnowledgeChunkRepository chunks;

    public KnowledgeIndexPersistence(KnowledgeChunkRepository chunks) {
        this.chunks = chunks;
    }

    @Transactional
    public void replace(UUID tenantId, UUID documentId, UUID versionId, String model, List<PreparedChunk> prepared) {
        chunks.deleteAll(chunks.findByVersionId(versionId));
        for (PreparedChunk value : prepared) {
            KnowledgeChunk chunk = new KnowledgeChunk(
                tenantId, documentId, versionId, value.index(), value.content(), value.hash(), model);
            chunks.saveAndFlush(chunk);
            int updated = chunks.storeEmbedding(chunk.getId(), tenantId, value.vector(), model);
            if (updated != 1) throw new IllegalStateException("Knowledge embedding was not stored");
        }
    }

    public record PreparedChunk(int index, String content, String hash, String vector) {}
}
