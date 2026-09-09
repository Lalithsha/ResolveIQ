package com.resolveiq.orchestration.domain.repository;

import com.resolveiq.orchestration.domain.model.action.ActionExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ActionExecutionRepository extends JpaRepository<ActionExecution, UUID> {
    List<ActionExecution> findByProposalIdAndTenantIdOrderByAttemptNumberAsc(UUID proposalId, UUID tenantId);
    Optional<ActionExecution> findByTenantIdAndProviderIdempotencyKey(UUID tenantId, String providerIdempotencyKey);
}
