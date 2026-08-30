package com.resolveiq.routing.domain.repository;

import com.resolveiq.routing.domain.model.SlaPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SlaPolicyRepository extends JpaRepository<SlaPolicy, UUID> {
    Optional<SlaPolicy> findByTenantIdAndPriority(UUID tenantId, String priority);
    List<SlaPolicy> findByTenantId(UUID tenantId);
}
