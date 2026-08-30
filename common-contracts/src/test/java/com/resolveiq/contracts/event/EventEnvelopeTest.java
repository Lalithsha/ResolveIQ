package com.resolveiq.contracts.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.resolveiq.contracts.event.EventEnvelope;
import com.resolveiq.contracts.event.TicketEvents;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EventEnvelopeTest {

    @Test
    @DisplayName("Should create event envelope with default correlation ID and timestamp")
    void testCreateEventEnvelope() {
        UUID tenantId = UUID.randomUUID();
        UUID ticketId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        TicketEvents.TicketCreatedPayload payload = new TicketEvents.TicketCreatedPayload(
            ticketId,
            "RIQ-2026-000100",
            tenantId,
            customerId,
            "Test ticket",
            "Description of test ticket",
            "WEB",
            "MEDIUM",
            "TECHNICAL",
            Instant.now()
        );

        EventEnvelope<TicketEvents.TicketCreatedPayload> envelope = EventEnvelope.create(
            TicketEvents.TICKET_CREATED,
            1,
            "ticket-service",
            tenantId,
            "ticket",
            ticketId,
            null,
            null,
            "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01",
            payload
        );

        assertThat(envelope).isNotNull();
        assertThat(envelope.eventId()).isNotNull();
        assertThat(envelope.eventType()).isEqualTo("resolveiq.ticket.created");
        assertThat(envelope.eventVersion()).isEqualTo(1);
        assertThat(envelope.producer()).isEqualTo("ticket-service");
        assertThat(envelope.tenantId()).isEqualTo(tenantId);
        assertThat(envelope.aggregateId()).isEqualTo(ticketId);
        assertThat(envelope.correlationId()).isNotNull();
        assertThat(envelope.payload()).isEqualTo(payload);
    }
}
