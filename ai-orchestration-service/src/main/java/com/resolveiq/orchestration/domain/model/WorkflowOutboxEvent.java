package com.resolveiq.orchestration.domain.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_events", schema = "orchestration_schema")
public class WorkflowOutboxEvent {

    @Id
    private UUID id;

    @Column(name = "aggregate_type", nullable = false, length = 100)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "JSONB")
    private String payload;

    @Column(nullable = false, length = 50)
    private String status; // PENDING, PUBLISHED, RETRY, DEAD

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;

    @Column(name = "last_error_code", length = 100)
    private String lastErrorCode;

    @Column(name = "claimed_at")
    private Instant claimedAt;

    @Column(name = "claimed_by", length = 100)
    private String claimedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    public WorkflowOutboxEvent() {}

    public WorkflowOutboxEvent(String aggregateType, UUID aggregateId, String eventType, String payload) {
        this.id = UUID.randomUUID();
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.status = "PENDING";
        this.retryCount = 0;
        this.attemptCount = 0;
        this.nextAttemptAt = Instant.now();
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getAggregateType() { return aggregateType; }
    public UUID getAggregateId() { return aggregateId; }
    public String getEventType() { return eventType; }
    public String getPayload() { return payload; }
    public String getStatus() { return status; }
    public int getRetryCount() { return retryCount; }
    public int getAttemptCount() { return attemptCount; }
    public Instant getNextAttemptAt() { return nextAttemptAt; }
    public String getLastErrorCode() { return lastErrorCode; }
    public Instant getClaimedAt() { return claimedAt; }
    public String getClaimedBy() { return claimedBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getPublishedAt() { return publishedAt; }

    public void markClaimed(String workerId) {
        this.claimedAt = Instant.now();
        this.claimedBy = workerId;
    }

    public void markPublished() {
        this.status = "PUBLISHED";
        this.publishedAt = Instant.now();
        this.claimedAt = null;
        this.claimedBy = null;
    }

    public void markFailed(String errorCode) {
        this.attemptCount++;
        this.retryCount++;
        this.lastErrorCode = errorCode;
        this.claimedAt = null;
        this.claimedBy = null;
        if (this.attemptCount >= 5) {
            this.status = "DEAD";
        } else {
            this.status = "RETRY";
            long delaySec = (long) Math.min(300, Math.pow(2, this.attemptCount) * 2);
            this.nextAttemptAt = Instant.now().plus(Duration.ofSeconds(delaySec));
        }
    }

    public void markFailed() {
        markFailed("PUBLISH_FAILED");
    }
}
