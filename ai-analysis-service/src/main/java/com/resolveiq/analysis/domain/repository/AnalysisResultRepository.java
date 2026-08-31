package com.resolveiq.analysis.domain.repository;

import com.resolveiq.analysis.domain.model.AnalysisResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AnalysisResultRepository extends JpaRepository<AnalysisResult, UUID> {
    Optional<AnalysisResult> findByTicketId(UUID ticketId);
    List<AnalysisResult> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);
    List<AnalysisResult> findTop100ByTenantIdOrderByCreatedAtDesc(UUID tenantId);
    Page<AnalysisResult> findByTenantId(UUID tenantId, Pageable pageable);
    long countByTenantId(UUID tenantId);
    long countByTenantIdAndValidationOutcome(UUID tenantId, String outcome);
    long countByTenantIdAndGuardrailOutcome(UUID tenantId, String outcome);
    long countByTenantIdAndValidationOutcomeStartingWith(UUID tenantId, String prefix);

    @Query("""
        select coalesce(sum(result.inputTokens), 0),
               coalesce(sum(result.outputTokens), 0),
               coalesce(sum(result.estimatedCostMicros), 0)
        from AnalysisResult result
        where result.tenantId = :tenantId
        """)
    Object[] aggregateUsageByTenantId(@Param("tenantId") UUID tenantId);
}
