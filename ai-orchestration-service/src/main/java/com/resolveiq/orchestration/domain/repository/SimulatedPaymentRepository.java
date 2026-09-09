package com.resolveiq.orchestration.domain.repository;

import com.resolveiq.orchestration.domain.model.action.SimulatedPayment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SimulatedPaymentRepository extends JpaRepository<SimulatedPayment, UUID> {
    Optional<SimulatedPayment> findByTenantIdAndPaymentReference(UUID tenantId, String paymentReference);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM SimulatedPayment p WHERE p.tenantId = :tenantId AND p.paymentReference = :paymentReference")
    Optional<SimulatedPayment> findByTenantIdAndPaymentReferenceWithLock(@Param("tenantId") UUID tenantId, @Param("paymentReference") String paymentReference);
}
