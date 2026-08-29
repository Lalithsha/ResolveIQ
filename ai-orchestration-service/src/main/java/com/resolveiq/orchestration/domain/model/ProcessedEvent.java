package com.resolveiq.orchestration.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "processed_events", schema = "orchestration_schema")
public class ProcessedEvent {
    @Id
    @Column(name = "event_id")
    private UUID eventId;
    @Column(name = "consumer_group", nullable = false, length = 100)
    private String consumerGroup;
    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    protected ProcessedEvent() {}

    public ProcessedEvent(UUID eventId, String consumerGroup) {
        this.eventId = eventId;
        this.consumerGroup = consumerGroup;
        this.processedAt = Instant.now();
    }
}
