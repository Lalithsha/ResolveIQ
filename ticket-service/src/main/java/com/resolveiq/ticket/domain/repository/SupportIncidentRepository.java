package com.resolveiq.ticket.domain.repository;

import com.resolveiq.ticket.domain.model.IncidentStatus;
import com.resolveiq.ticket.domain.model.SupportIncident;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SupportIncidentRepository extends JpaRepository<SupportIncident, UUID> {
    Optional<SupportIncident> findByTenantIdAndId(UUID tenantId, UUID id);
    Optional<SupportIncident> findByTenantIdAndIncidentNumber(UUID tenantId, String incidentNumber);
    Page<SupportIncident> findByTenantId(UUID tenantId, Pageable pageable);
    Page<SupportIncident> findByTenantIdAndStatus(UUID tenantId, IncidentStatus status, Pageable pageable);
    List<SupportIncident> findByTenantIdAndStatusIn(UUID tenantId, List<IncidentStatus> statuses);
    long countByTenantIdAndStatus(UUID tenantId, IncidentStatus status);
}
