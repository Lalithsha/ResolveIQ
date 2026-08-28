package com.resolveiq.auth.domain.repository;

import com.resolveiq.auth.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByTenantIdAndNormalizedEmail(UUID tenantId, String normalizedEmail);
    Optional<User> findByNormalizedEmail(String normalizedEmail);
    boolean existsByTenantIdAndNormalizedEmail(UUID tenantId, String normalizedEmail);
}
