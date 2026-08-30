package com.resolveiq.auth.domain.repository;

import com.resolveiq.auth.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByTenantIdAndNormalizedEmail(UUID tenantId, String normalizedEmail);
    Optional<User> findByNormalizedEmail(String normalizedEmail);
    boolean existsByTenantIdAndNormalizedEmail(UUID tenantId, String normalizedEmail);
}
