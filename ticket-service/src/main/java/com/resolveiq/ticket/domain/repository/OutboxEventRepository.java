package com.resolveiq.ticket.domain.repository;

import com.resolveiq.ticket.domain.model.OutboxEvent;
import com.resolveiq.ticket.domain.model.OutboxStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
    List<OutboxEvent> findByStatusOrderByCreatedAtAsc(OutboxStatus status, Pageable pageable);

    @Query(value = """
        SELECT o.status, COUNT(*)
        FROM ticket_schema.outbox_events o
        JOIN ticket_schema.tickets t ON t.id = o.aggregate_id
        WHERE t.tenant_id = :tenantId
        GROUP BY o.status
        """, nativeQuery = true)
    List<Object[]> countByStatusForTenant(@Param("tenantId") UUID tenantId);

    @Query(value = """
        SELECT * FROM ticket_schema.outbox_events
        WHERE status IN ('PENDING', 'RETRY')
          AND (next_attempt_at IS NULL OR next_attempt_at <= CURRENT_TIMESTAMP)
          AND (claimed_at IS NULL OR claimed_at < :leaseExpiredAt)
        ORDER BY created_at ASC
        LIMIT :limit
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    List<OutboxEvent> claimDueEvents(
        @Param("leaseExpiredAt") Instant leaseExpiredAt,
        @Param("limit") int limit
    );
}
