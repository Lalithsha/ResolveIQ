package com.resolveiq.orchestration.domain.repository;

import com.resolveiq.orchestration.domain.model.action.SimulatedAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SimulatedAccountRepository extends JpaRepository<SimulatedAccount, UUID> {
    Optional<SimulatedAccount> findByTenantIdAndUserId(UUID tenantId, UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM SimulatedAccount a WHERE a.tenantId = :tenantId AND a.userId = :userId")
    Optional<SimulatedAccount> findByTenantIdAndUserIdWithLock(@Param("tenantId") UUID tenantId, @Param("userId") UUID userId);
}
