package com.resolveiq.ticket.domain.repository;

import com.resolveiq.ticket.domain.model.IncidentTicketLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IncidentTicketLinkRepository extends JpaRepository<IncidentTicketLink, UUID> {
    List<IncidentTicketLink> findByIncidentId(UUID incidentId);
    Optional<IncidentTicketLink> findByIncidentIdAndTicketId(UUID incidentId, UUID ticketId);
    boolean existsByIncidentIdAndTicketId(UUID incidentId, UUID ticketId);
    long countByIncidentIdAndUnlinkedAtIsNull(UUID incidentId);
}
