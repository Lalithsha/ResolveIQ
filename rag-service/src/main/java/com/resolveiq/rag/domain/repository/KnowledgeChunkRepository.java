package com.resolveiq.rag.domain.repository;

import com.resolveiq.rag.domain.model.KnowledgeChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface KnowledgeChunkRepository extends JpaRepository<KnowledgeChunk, UUID> {
    List<KnowledgeChunk> findByVersionId(UUID versionId);
    List<KnowledgeChunk> findByTenantId(UUID tenantId);
    long countByVersionId(UUID versionId);

    @Query(value = """
        SELECT count(*)
        FROM rag_schema.knowledge_chunks
        WHERE tenant_id = :tenantId
          AND version_id = :versionId
          AND (embedding IS NULL
               OR vector_dims(embedding) <> :dimension
               OR embedding_model <> :model)
        """, nativeQuery = true)
    long countInvalidIndexRows(
        @Param("tenantId") UUID tenantId,
        @Param("versionId") UUID versionId,
        @Param("dimension") int dimension,
        @Param("model") String model
    );

    @Query(value = """
        SELECT kc.* FROM rag_schema.knowledge_chunks kc
        JOIN rag_schema.knowledge_documents kd ON kc.document_id = kd.id
        WHERE kc.tenant_id = :tenantId
          AND kd.status = 'PUBLISHED'
          AND kc.version_id = kd.active_version_id
          AND (:category IS NULL OR kd.category = :category)
          AND (:product IS NULL OR kd.product = :product)
          AND (:language IS NULL OR kd.language = :language)
          AND kc.tsv_content @@ websearch_to_tsquery('english', :query)
        ORDER BY ts_rank_cd(kc.tsv_content, websearch_to_tsquery('english', :query)) DESC, kc.id
        LIMIT :limit
        """, nativeQuery = true)
    List<KnowledgeChunk> searchLexical(
        @Param("tenantId") UUID tenantId,
        @Param("query") String query,
        @Param("category") String category,
        @Param("product") String product,
        @Param("language") String language,
        @Param("limit") int limit
    );

    /**
     * Relaxed lexical fallback for natural-language queries. plainto_tsquery
     * treats every token as mandatory; replacing its generated AND expression
     * with OR keeps lexical retrieval useful when a user uses synonyms or
     * additional context that is not present in a short knowledge chunk.
     */
    @Query(value = """
        SELECT kc.* FROM rag_schema.knowledge_chunks kc
        JOIN rag_schema.knowledge_documents kd ON kc.document_id = kd.id
        WHERE kc.tenant_id = :tenantId
          AND kd.status = 'PUBLISHED'
          AND kc.version_id = kd.active_version_id
          AND (:category IS NULL OR kd.category = :category)
          AND (:product IS NULL OR kd.product = :product)
          AND (:language IS NULL OR kd.language = :language)
          AND kc.tsv_content @@ to_tsquery(
              'english',
              replace(plainto_tsquery('english', :query)::text, ' & ', ' | ')
          )
        ORDER BY ts_rank_cd(
            kc.tsv_content,
            to_tsquery('english', replace(plainto_tsquery('english', :query)::text, ' & ', ' | '))
        ) DESC, kc.id
        LIMIT :limit
        """, nativeQuery = true)
    List<KnowledgeChunk> searchLexicalRelaxed(
        @Param("tenantId") UUID tenantId,
        @Param("query") String query,
        @Param("category") String category,
        @Param("product") String product,
        @Param("language") String language,
        @Param("limit") int limit
    );

    @Query(value = """
        SELECT kc.* FROM rag_schema.knowledge_chunks kc
        JOIN rag_schema.knowledge_documents kd ON kc.document_id = kd.id
        WHERE kc.tenant_id = :tenantId
          AND kd.status = 'PUBLISHED'
          AND kc.version_id = kd.active_version_id
          AND (:category IS NULL OR kd.category = :category)
          AND (:product IS NULL OR kd.product = :product)
          AND (:language IS NULL OR kd.language = :language)
          AND kc.embedding IS NOT NULL
        ORDER BY kc.embedding <=> cast(:embedding as vector)
        LIMIT :limit
        """, nativeQuery = true)
    List<KnowledgeChunk> searchVector(
        @Param("tenantId") UUID tenantId,
        @Param("embedding") String embeddingString,
        @Param("category") String category,
        @Param("product") String product,
        @Param("language") String language,
        @Param("limit") int limit
    );

    @Modifying(flushAutomatically = true)
    @Transactional
    @Query(value = """
        UPDATE rag_schema.knowledge_chunks
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
