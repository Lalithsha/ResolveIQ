package com.resolveiq.ticket.domain.repository;

import com.resolveiq.ticket.domain.model.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, String> {
    Optional<IdempotencyKey> findByKeyAndTenantId(String key, UUID tenantId);
}
