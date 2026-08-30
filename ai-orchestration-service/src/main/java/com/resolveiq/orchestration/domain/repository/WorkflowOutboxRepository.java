package com.resolveiq.orchestration.domain.repository;

import com.resolveiq.orchestration.domain.model.WorkflowOutboxEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface WorkflowOutboxRepository extends JpaRepository<WorkflowOutboxEvent, UUID> {
    List<WorkflowOutboxEvent> findByStatusOrderByCreatedAtAsc(String status, Pageable pageable);

    @Query(value = """
        SELECT * FROM orchestration_schema.outbox_events
        WHERE status IN ('PENDING', 'RETRY')
          AND (next_attempt_at IS NULL OR next_attempt_at <= CURRENT_TIMESTAMP)
          AND (claimed_at IS NULL OR claimed_at < :leaseExpiredAt)
        ORDER BY created_at ASC
        LIMIT :limit
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    List<WorkflowOutboxEvent> claimDueEvents(
        @Param("leaseExpiredAt") Instant leaseExpiredAt,
        @Param("limit") int limit
    );
}
