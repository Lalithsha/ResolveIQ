package com.resolveiq.orchestration.domain.repository;

import com.resolveiq.orchestration.domain.model.action.SimulatedAccountOperation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SimulatedAccountOperationRepository extends JpaRepository<SimulatedAccountOperation, UUID> {
    Optional<SimulatedAccountOperation> findByTenantIdAndProviderIdempotencyKey(UUID tenantId, String providerIdempotencyKey);
}
