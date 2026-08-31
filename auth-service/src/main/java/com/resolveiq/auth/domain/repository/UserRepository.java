package com.resolveiq.auth.domain.repository;

import com.resolveiq.auth.domain.model.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    /** Serializes password-attempt counters for simultaneous logins to one identity. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<User> findByTenantIdAndNormalizedEmail(UUID tenantId, String normalizedEmail);

    /** Serializes password-attempt counters when the tenant is inferred by unique email. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<User> findByNormalizedEmail(String normalizedEmail);
    boolean existsByTenantIdAndNormalizedEmail(UUID tenantId, String normalizedEmail);
    Optional<User> findByIdAndTenantId(UUID id, UUID tenantId);
    org.springframework.data.domain.Page<User> findByTenantId(UUID tenantId, org.springframework.data.domain.Pageable pageable);
}
