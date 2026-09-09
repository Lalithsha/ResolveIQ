package com.resolveiq.orchestration.domain.repository;

import com.resolveiq.orchestration.domain.model.action.ActionReconciliation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ActionReconciliationRepository extends JpaRepository<ActionReconciliation, UUID> {
    Optional<ActionReconciliation> findByExecutionIdAndTenantId(UUID executionId, UUID tenantId);
    Optional<ActionReconciliation> findFirstByProposalIdOrderByReconciledAtDesc(UUID proposalId);
}
