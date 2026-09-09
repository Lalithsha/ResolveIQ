package com.resolveiq.ticket.domain.repository.resolution;

import com.resolveiq.ticket.domain.model.resolution.ResolutionOutcome;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ResolutionOutcomeRepository extends JpaRepository<ResolutionOutcome, UUID> {

    List<ResolutionOutcome> findByTenantIdAndResolutionIdOrderByOccurredAtAsc(UUID tenantId, UUID resolutionId);

    long countByTenantId(UUID tenantId);
}
