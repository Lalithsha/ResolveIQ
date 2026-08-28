package com.resolveiq.rag.domain.repository;

import com.resolveiq.rag.domain.model.ResolvedCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ResolvedCaseRepository extends JpaRepository<ResolvedCase, UUID> {
    Optional<ResolvedCase> findByIdAndTenantId(UUID id, UUID tenantId);
    List<ResolvedCase> findByTenantId(UUID tenantId);
}
