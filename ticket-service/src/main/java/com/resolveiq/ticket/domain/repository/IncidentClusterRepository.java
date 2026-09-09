package com.resolveiq.ticket.domain.repository;

import com.resolveiq.ticket.domain.model.ClusterStatus;
import com.resolveiq.ticket.domain.model.IncidentCluster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IncidentClusterRepository extends JpaRepository<IncidentCluster, UUID> {
    Optional<IncidentCluster> findByTenantIdAndId(UUID tenantId, UUID id);
    List<IncidentCluster> findByTenantIdAndStatus(UUID tenantId, ClusterStatus status);
    Optional<IncidentCluster> findByTenantIdAndCentroidHash(UUID tenantId, String centroidHash);
}
