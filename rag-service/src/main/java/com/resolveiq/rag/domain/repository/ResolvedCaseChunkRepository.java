package com.resolveiq.rag.domain.repository;

import com.resolveiq.rag.domain.model.ResolvedCaseChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public interface ResolvedCaseChunkRepository extends JpaRepository<ResolvedCaseChunk, UUID> {
    List<ResolvedCaseChunk> findByResolvedCaseId(UUID resolvedCaseId);
    List<ResolvedCaseChunk> findByTenantId(UUID tenantId);

    @Query(value = """
        SELECT rcc.* FROM rag_schema.resolved_case_chunks rcc
        WHERE rcc.tenant_id = :tenantId
          AND rcc.tsv_content @@ plainto_tsquery('english', :query)
        ORDER BY ts_rank_cd(rcc.tsv_content, plainto_tsquery('english', :query)) DESC, rcc.id
        LIMIT :limit
        """, nativeQuery = true)
    List<ResolvedCaseChunk> searchLexical(
        @Param("tenantId") UUID tenantId,
        @Param("query") String query,
        @Param("limit") int limit
    );

    @Query(value = """
        SELECT rcc.* FROM rag_schema.resolved_case_chunks rcc
        WHERE rcc.tenant_id = :tenantId
          AND rcc.embedding IS NOT NULL
        ORDER BY rcc.embedding <=> cast(:embedding as vector)
        LIMIT :limit
        """, nativeQuery = true)
    List<ResolvedCaseChunk> searchVector(
        @Param("tenantId") UUID tenantId,
        @Param("embedding") String embeddingString,
        @Param("limit") int limit
    );

    @Modifying(flushAutomatically = true)
    @Transactional
    @Query(value = """
        UPDATE rag_schema.resolved_case_chunks
        SET embedding = cast(:embedding AS vector), embedding_model = :model
        WHERE id = :chunkId AND tenant_id = :tenantId
        """, nativeQuery = true)
    int storeEmbedding(
        @Param("chunkId") UUID chunkId,
        @Param("tenantId") UUID tenantId,
        @Param("embedding") String embedding,
        @Param("model") String model
    );
}
