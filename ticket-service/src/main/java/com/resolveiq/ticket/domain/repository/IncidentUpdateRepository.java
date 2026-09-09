package com.resolveiq.ticket.domain.repository;

import com.resolveiq.ticket.domain.model.IncidentUpdate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IncidentUpdateRepository extends JpaRepository<IncidentUpdate, UUID> {
    Optional<IncidentUpdate> findByTenantIdAndId(UUID tenantId, UUID id);
    List<IncidentUpdate> findByTenantIdAndIncidentIdOrderByUpdateNumberAsc(UUID tenantId, UUID incidentId);
    long countByTenantIdAndIncidentId(UUID tenantId, UUID incidentId);
}
