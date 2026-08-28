package com.resolveiq.contracts.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Standard event envelope for all asynchronous domain and integration events in ResolveIQ.
 * Adheres to Section 8.1 of RESOLVEIQ_IMPLEMENTATION_BLUEPRINT.md.
 */
public record EventEnvelope<T>(
    UUID eventId,
    String eventType,
    int eventVersion,
    Instant occurredAt,
    String producer,
    UUID tenantId,
    String aggregateType,
    UUID aggregateId,
    UUID correlationId,
    UUID causationId,
    String traceparent,
    T payload
) {
    public static <T> EventEnvelope<T> create(
        String eventType,
        int eventVersion,
        String producer,
        UUID tenantId,
        String aggregateType,
        UUID aggregateId,
        UUID correlationId,
        UUID causationId,
        String traceparent,
        T payload
    ) {
        return new EventEnvelope<>(
            UUID.randomUUID(),
            eventType,
            eventVersion,
            Instant.now(),
            producer,
            tenantId,
            aggregateType,
            aggregateId,
            correlationId != null ? correlationId : UUID.randomUUID(),
            causationId,
            traceparent,
            payload
        );
    }
}
