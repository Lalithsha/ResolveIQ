package com.resolveiq.routing.domain.repository;

import com.resolveiq.routing.domain.model.SlaPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SlaPolicyRepository extends JpaRepository<SlaPolicy, UUID> {
    Optional<SlaPolicy> findByTenantIdAndPriority(UUID tenantId, String priority);
    List<SlaPolicy> findByTenantId(UUID tenantId);
}
