package com.resolveiq.auth.domain.repository;

import com.resolveiq.auth.domain.model.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {
    Optional<Tenant> findByDomain(String domain);
}
