package com.resolveiq.ticket.domain.repository;

import com.resolveiq.ticket.domain.model.IncidentComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IncidentComponentRepository extends JpaRepository<IncidentComponent, UUID> {
    List<IncidentComponent> findByTenantIdAndIncidentId(UUID tenantId, UUID incidentId);
}
