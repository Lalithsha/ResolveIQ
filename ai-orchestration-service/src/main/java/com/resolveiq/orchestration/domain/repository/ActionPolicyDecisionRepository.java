package com.resolveiq.orchestration.domain.repository;

import com.resolveiq.orchestration.domain.model.action.ActionPolicyDecision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ActionPolicyDecisionRepository extends JpaRepository<ActionPolicyDecision, UUID> {
    Optional<ActionPolicyDecision> findByProposalIdAndTenantId(UUID proposalId, UUID tenantId);
    Optional<ActionPolicyDecision> findFirstByProposalIdOrderByEvaluatedAtDesc(UUID proposalId);
}
