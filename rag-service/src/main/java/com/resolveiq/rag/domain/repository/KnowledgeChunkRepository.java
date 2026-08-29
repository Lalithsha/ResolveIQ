package com.resolveiq.rag.domain.repository;

import com.resolveiq.rag.domain.model.KnowledgeChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface KnowledgeChunkRepository extends JpaRepository<KnowledgeChunk, UUID> {
    List<KnowledgeChunk> findByVersionId(UUID versionId);
    List<KnowledgeChunk> findByTenantId(UUID tenantId);

    @Query(value = """
        SELECT kc.* FROM rag_schema.knowledge_chunks kc
        JOIN rag_schema.knowledge_documents kd ON kc.document_id = kd.id
        WHERE kc.tenant_id = :tenantId
          AND kd.status = 'PUBLISHED'
          AND kc.version_id = kd.active_version_id
          AND kc.tsv_content @@ plainto_tsquery('english', :query)
        LIMIT :limit
        """, nativeQuery = true)
    List<KnowledgeChunk> searchLexical(
        @Param("tenantId") UUID tenantId,
        @Param("query") String query,
        @Param("limit") int limit
    );

    @Query(value = """
        SELECT kc.* FROM rag_schema.knowledge_chunks kc
        JOIN rag_schema.knowledge_documents kd ON kc.document_id = kd.id
        WHERE kc.tenant_id = :tenantId
          AND kd.status = 'PUBLISHED'
          AND kc.version_id = kd.active_version_id
          AND kc.embedding IS NOT NULL
        ORDER BY kc.embedding <=> cast(:embedding as vector)
        LIMIT :limit
        """, nativeQuery = true)
    List<KnowledgeChunk> searchVector(
        @Param("tenantId") UUID tenantId,
        @Param("embedding") String embeddingString,
        @Param("limit") int limit
    );
}
