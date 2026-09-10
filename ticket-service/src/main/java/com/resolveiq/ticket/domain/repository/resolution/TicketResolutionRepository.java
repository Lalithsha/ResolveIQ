package com.resolveiq.ticket.domain.repository.resolution;

import com.resolveiq.ticket.domain.model.resolution.ResolutionAttemptStatus;
import com.resolveiq.ticket.domain.model.resolution.TicketResolution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.Instant;

@Repository
public interface TicketResolutionRepository extends JpaRepository<TicketResolution, UUID> {

    Optional<TicketResolution> findTopByTenantIdAndTicketIdOrderByAttemptNumberDesc(UUID tenantId, UUID ticketId);

    List<TicketResolution> findByTenantIdAndTicketIdOrderByAttemptNumberAsc(UUID tenantId, UUID ticketId);

    List<TicketResolution> findByTenantIdAndStatus(UUID tenantId, ResolutionAttemptStatus status);

    Optional<TicketResolution> findByIdAndTenantId(UUID id, UUID tenantId);

    long countByTenantId(UUID tenantId);

    long countByTenantIdAndStatus(UUID tenantId, ResolutionAttemptStatus status);

    List<TicketResolution> findTop100ByStatusAndScheduledClosureAtLessThanEqualOrderByScheduledClosureAtAsc(
        ResolutionAttemptStatus status, Instant now);

    List<TicketResolution> findTop100ByStatusAndConfirmationWindowExpiresAtLessThanEqualOrderByConfirmationWindowExpiresAtAsc(
        ResolutionAttemptStatus status, Instant now);
}
