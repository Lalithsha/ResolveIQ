package com.resolveiq.rag.domain.repository;

import com.resolveiq.rag.domain.model.ResolvedCaseChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
        LIMIT :limit
        """, nativeQuery = true)
    List<ResolvedCaseChunk> searchLexical(
        @Param("tenantId") UUID tenantId,
        @Param("query") String query,
        @Param("limit") int limit
    );
}
