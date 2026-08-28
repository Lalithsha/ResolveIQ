package com.resolveiq.analysis.domain.repository;

import com.resolveiq.analysis.domain.model.AnalysisResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AnalysisResultRepository extends JpaRepository<AnalysisResult, UUID> {
    Optional<AnalysisResult> findByTicketId(UUID ticketId);
    List<AnalysisResult> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);
}
