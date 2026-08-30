package com.resolveiq.rag.domain.repository;

import com.resolveiq.rag.domain.model.ResolvedCase;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ResolvedCaseRepository extends JpaRepository<ResolvedCase, UUID> {
    Optional<ResolvedCase> findByIdAndTenantId(UUID id, UUID tenantId);
    List<ResolvedCase> findByTenantId(UUID tenantId);
}
