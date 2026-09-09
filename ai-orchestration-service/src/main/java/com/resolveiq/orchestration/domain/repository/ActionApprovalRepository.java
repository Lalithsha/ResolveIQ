package com.resolveiq.orchestration.domain.repository;

import com.resolveiq.orchestration.domain.model.action.ActionApproval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ActionApprovalRepository extends JpaRepository<ActionApproval, UUID> {
    List<ActionApproval> findByProposalIdAndTenantIdOrderByCreatedAtDesc(UUID proposalId, UUID tenantId);
    Optional<ActionApproval> findByProposalIdAndActorIdAndApprovedDigest(UUID proposalId, UUID actorId, String approvedDigest);
}
