package com.resolveiq.orchestration.domain.repository;

import com.resolveiq.orchestration.domain.model.action.SimulatedPaymentOperation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SimulatedPaymentOperationRepository extends JpaRepository<SimulatedPaymentOperation, UUID> {
    Optional<SimulatedPaymentOperation> findByTenantIdAndProviderIdempotencyKey(UUID tenantId, String providerIdempotencyKey);
}
