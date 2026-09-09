package com.resolveiq.orchestration.domain.repository;

import com.resolveiq.orchestration.domain.model.action.ResolutionActionProposal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ResolutionActionProposalRepository extends JpaRepository<ResolutionActionProposal, UUID> {
    Optional<ResolutionActionProposal> findByIdAndTenantId(UUID id, UUID tenantId);
    List<ResolutionActionProposal> findByTenantIdAndTicketIdOrderByCreatedAtDesc(UUID tenantId, UUID ticketId);
    Optional<ResolutionActionProposal> findByTenantIdAndInputHash(UUID tenantId, String inputHash);
}
