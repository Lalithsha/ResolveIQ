package com.resolveiq.ticket.domain.repository;

import com.resolveiq.ticket.domain.model.CustomerImpact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerImpactRepository extends JpaRepository<CustomerImpact, UUID> {
    List<CustomerImpact> findByTenantIdAndIncidentId(UUID tenantId, UUID incidentId);
    List<CustomerImpact> findByTenantIdAndCustomerId(UUID tenantId, UUID customerId);
    Optional<CustomerImpact> findByTenantIdAndIncidentIdAndCustomerId(UUID tenantId, UUID incidentId, UUID customerId);
    long countByTenantIdAndIncidentId(UUID tenantId, UUID incidentId);
}
