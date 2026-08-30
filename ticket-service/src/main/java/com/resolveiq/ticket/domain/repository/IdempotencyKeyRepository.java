package com.resolveiq.ticket.domain.repository;

import com.resolveiq.ticket.domain.model.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, UUID> {
    Optional<IdempotencyKey> findByTenantIdAndActorIdAndOperationAndKey(
        UUID tenantId, UUID actorId, String operation, String key
    );

    @Modifying
    @Query(value = """
        INSERT INTO ticket_schema.idempotent_commands
            (id, tenant_id, actor_id, operation, idempotency_key, request_hash, status, created_at, expires_at, version)
        VALUES
            (:id, :tenantId, :actorId, :operation, :key, :requestHash, 'IN_PROGRESS', :createdAt, :expiresAt, 0)
        ON CONFLICT (tenant_id, actor_id, operation, idempotency_key) DO NOTHING
        """, nativeQuery = true)
    int claim(@Param("id") UUID id,
              @Param("tenantId") UUID tenantId,
              @Param("actorId") UUID actorId,
              @Param("operation") String operation,
              @Param("key") String key,
              @Param("requestHash") String requestHash,
              @Param("createdAt") Instant createdAt,
              @Param("expiresAt") Instant expiresAt);
}
